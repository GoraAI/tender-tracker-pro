package com.tendertracker.app.ui.screens.pipeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.app.ui.components.charts.PipelinePhase
import com.tendertracker.app.ui.components.charts.PipelineStatus
import com.tendertracker.app.ui.components.charts.PipelineTender
import com.tendertracker.domain.model.PipelineTender as DomainPipelineTender
import com.tendertracker.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
    val selectedTender: PipelineTender? = null,

    // Empty state
    val isEmpty: Boolean = true
)

@HiltViewModel
class PipelineViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
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
                val result = analyticsRepository.getPipelineStatus()

                result.onSuccess { status ->
                    val today = LocalDate.now()
                    val pipelineTenders = status.tenders.map { it.toChartTender(today) }

                    val dueThisWeek = pipelineTenders.count {
                        it.deadline.isBefore(today.plusDays(7)) &&
                        it.status != PipelineStatus.SUBMITTED &&
                        it.status != PipelineStatus.AWARDED &&
                        it.status != PipelineStatus.LOST
                    }

                    _state.update { it.copy(
                        isLoading = false,
                        tenders = pipelineTenders,
                        activeCount = status.activeCount,
                        dueThisWeek = status.dueThisWeek,
                        hasConflicts = status.hasConflicts,
                        isEmpty = status.isEmpty
                    )}
                }.onFailure { e ->
                    _state.update { it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load pipeline data"
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

    fun selectTender(tender: PipelineTender?) {
        _state.update { it.copy(selectedTender = tender) }
    }

    fun updateTenderStatus(pipelineId: String, newStatus: PipelineStatus) {
        viewModelScope.launch {
            analyticsRepository.updatePipelineStatus(
                pipelineId = pipelineId,
                status = newStatus.name.lowercase()
            )
            loadPipelineData()
        }
    }

    fun addTenderToPipeline(tenderId: String) {
        viewModelScope.launch {
            analyticsRepository.addToPipeline(
                tenderId = tenderId,
                status = "discovered"
            )
            loadPipelineData()
        }
    }

    fun removeTenderFromPipeline(pipelineId: String) {
        viewModelScope.launch {
            analyticsRepository.removeFromPipeline(pipelineId)
            loadPipelineData()
        }
    }
}

// Extension function to convert domain model to chart model
private fun DomainPipelineTender.toChartTender(today: LocalDate): PipelineTender {
    val deadlineDate = deadline?.let {
        try {
            LocalDate.parse(it.substringBefore("T"))
        } catch (e: Exception) {
            today.plusDays(14)
        }
    } ?: today.plusDays(14)

    val startDateParsed = startDate?.let {
        try {
            LocalDate.parse(it.substringBefore("T"))
        } catch (e: Exception) {
            today
        }
    } ?: today

    val chartStatus = when (status.lowercase()) {
        "discovered" -> PipelineStatus.CONSIDERING
        "considering" -> PipelineStatus.CONSIDERING
        "preparing" -> PipelineStatus.PREPARING
        "reviewing" -> PipelineStatus.REVIEWING
        "ready_to_submit", "ready" -> PipelineStatus.READY_TO_SUBMIT
        "submitted" -> PipelineStatus.SUBMITTED
        "awarded", "won" -> PipelineStatus.AWARDED
        "lost" -> PipelineStatus.LOST
        else -> PipelineStatus.CONSIDERING
    }

    return PipelineTender(
        id = id,
        title = title?.take(40) ?: "Untitled Tender",
        reference = reference ?: "",
        deadline = deadlineDate,
        status = chartStatus,
        startDate = startDateParsed,
        phases = generatePhases(today, startDateParsed, deadlineDate, chartStatus),
        hasConflict = hasConflict,
        conflictMessage = if (hasConflict) "Schedule conflict" else ""
    )
}

private fun generatePhases(
    today: LocalDate,
    startDate: LocalDate,
    deadline: LocalDate,
    status: PipelineStatus
): List<PipelinePhase> {
    val totalDays = ChronoUnit.DAYS.between(startDate, deadline).toInt().coerceAtLeast(1)
    val prepDays = (totalDays * 0.6).toInt().coerceAtLeast(1)
    val reviewDays = (totalDays * 0.3).toInt().coerceAtLeast(1)

    return listOf(
        PipelinePhase(
            name = "Prepare",
            startDate = startDate,
            endDate = startDate.plusDays(prepDays.toLong()),
            isComplete = status.ordinal > PipelineStatus.PREPARING.ordinal
        ),
        PipelinePhase(
            name = "Review",
            startDate = startDate.plusDays(prepDays.toLong()),
            endDate = startDate.plusDays((prepDays + reviewDays).toLong()),
            isComplete = status.ordinal > PipelineStatus.REVIEWING.ordinal
        ),
        PipelinePhase(
            name = "Submit",
            startDate = startDate.plusDays((prepDays + reviewDays).toLong()),
            endDate = deadline,
            isComplete = status == PipelineStatus.SUBMITTED ||
                       status == PipelineStatus.AWARDED ||
                       status == PipelineStatus.LOST
        )
    )
}
