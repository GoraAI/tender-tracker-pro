package com.tendertracker.app.ui.screens.pipeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.app.ui.components.charts.PipelinePhase
import com.tendertracker.app.ui.components.charts.PipelineStatus
import com.tendertracker.app.ui.components.charts.PipelineTender
import com.tendertracker.domain.repository.TenderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class PipelineState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // Pipeline tenders
    val tenders: List<PipelineTender> = emptyList(),

    // Summary
    val activeCount: Int = 0,
    val dueThisWeek: Int = 0,
    val hasConflicts: Boolean = false,

    // Selected tender
    val selectedTender: PipelineTender? = null
)

@HiltViewModel
class PipelineViewModel @Inject constructor(
    private val tenderRepository: TenderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PipelineState())
    val state: StateFlow<PipelineState> = _state.asStateFlow()

    init {
        loadPipelineData()
    }

    fun loadPipelineData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                tenderRepository.getAllTenders().collect { tenders ->
                    // Convert tenders to pipeline items
                    // In production, this would use actual pipeline status from user's tracked tenders
                    val today = LocalDate.now()

                    val pipelineTenders = tenders.take(6).mapIndexed { index, tender ->
                        val deadline = today.plusDays((3 + index * 2).toLong())
                        val status = when (index) {
                            0 -> PipelineStatus.REVIEWING
                            1 -> PipelineStatus.PREPARING
                            2 -> PipelineStatus.READY_TO_SUBMIT
                            3 -> PipelineStatus.CONSIDERING
                            4 -> PipelineStatus.SUBMITTED
                            else -> PipelineStatus.PREPARING
                        }

                        PipelineTender(
                            id = tender.id,
                            title = tender.title.take(40),
                            reference = tender.reference,
                            deadline = deadline,
                            status = status,
                            startDate = today.minusDays((5 - index).toLong().coerceAtLeast(0)),
                            phases = generatePhases(today, deadline, status),
                            hasConflict = index == 0 && tenders.size > 2,
                            conflictMessage = if (index == 0 && tenders.size > 2) "Deadline conflict" else ""
                        )
                    }

                    val dueThisWeek = pipelineTenders.count {
                        it.deadline.isBefore(today.plusDays(7)) &&
                        it.status != PipelineStatus.SUBMITTED &&
                        it.status != PipelineStatus.AWARDED &&
                        it.status != PipelineStatus.LOST
                    }

                    val hasConflicts = pipelineTenders.any { it.hasConflict }

                    _state.update { it.copy(
                        isLoading = false,
                        tenders = pipelineTenders,
                        activeCount = pipelineTenders.count {
                            it.status != PipelineStatus.SUBMITTED &&
                            it.status != PipelineStatus.AWARDED &&
                            it.status != PipelineStatus.LOST
                        },
                        dueThisWeek = dueThisWeek,
                        hasConflicts = hasConflicts
                    )}
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load pipeline data"
                )}
            }
        }
    }

    private fun generatePhases(
        today: LocalDate,
        deadline: LocalDate,
        status: PipelineStatus
    ): List<PipelinePhase> {
        val daysUntilDeadline = java.time.temporal.ChronoUnit.DAYS.between(today, deadline).toInt()
        val prepDays = (daysUntilDeadline * 0.6).toInt().coerceAtLeast(1)
        val reviewDays = (daysUntilDeadline * 0.3).toInt().coerceAtLeast(1)

        return listOf(
            PipelinePhase(
                name = "Prepare",
                startDate = today,
                endDate = today.plusDays(prepDays.toLong()),
                isComplete = status.ordinal > PipelineStatus.PREPARING.ordinal
            ),
            PipelinePhase(
                name = "Review",
                startDate = today.plusDays(prepDays.toLong()),
                endDate = today.plusDays((prepDays + reviewDays).toLong()),
                isComplete = status.ordinal > PipelineStatus.REVIEWING.ordinal
            ),
            PipelinePhase(
                name = "Submit",
                startDate = today.plusDays((prepDays + reviewDays).toLong()),
                endDate = deadline,
                isComplete = status == PipelineStatus.SUBMITTED ||
                           status == PipelineStatus.AWARDED ||
                           status == PipelineStatus.LOST
            )
        )
    }

    fun selectTender(tender: PipelineTender?) {
        _state.update { it.copy(selectedTender = tender) }
    }

    fun updateTenderStatus(tenderId: String, newStatus: PipelineStatus) {
        viewModelScope.launch {
            // In production, persist to backend
            loadPipelineData()
        }
    }

    fun addTenderToPipeline(tenderId: String) {
        viewModelScope.launch {
            // In production, add tender to user's pipeline
            loadPipelineData()
        }
    }

    fun removeTenderFromPipeline(tenderId: String) {
        viewModelScope.launch {
            // In production, remove from pipeline
            loadPipelineData()
        }
    }
}
