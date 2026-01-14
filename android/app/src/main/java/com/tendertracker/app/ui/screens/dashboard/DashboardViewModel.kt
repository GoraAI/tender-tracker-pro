package com.tendertracker.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.app.gemma.engine.GemmaInferenceEngine
import com.tendertracker.domain.model.Tender
import com.tendertracker.domain.repository.TenderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val totalTenders: Int = 0,
    val highPriorityCount: Int = 0,
    val avgScore: Float = 0f,
    val modelStatus: ModelStatus = ModelStatus.NOT_LOADED,
    val recentTenders: List<Tender> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

enum class ModelStatus(val displayName: String) {
    NOT_LOADED("Not Loaded"),
    LOADING("Loading..."),
    READY("Ready"),
    ERROR("Error")
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val tenderRepository: TenderRepository,
    private val inferenceEngine: GemmaInferenceEngine
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load all tenders
            tenderRepository.getAllTenders()
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load tenders"
                        )
                    }
                }
                .collect { tenders ->
                    val totalCount = tenders.size
                    val highPriority = tenders.filter { (it.score ?: 0) >= 70 }
                    val avgScore = if (tenders.isNotEmpty()) {
                        tenders.mapNotNull { it.score }.average().toFloat()
                    } else 0f

                    val recentHighPriority = highPriority
                        .sortedByDescending { it.score }
                        .take(5)

                    val modelStatus = when {
                        inferenceEngine.isReady() -> ModelStatus.READY
                        else -> ModelStatus.NOT_LOADED
                    }

                    _state.update {
                        it.copy(
                            totalTenders = totalCount,
                            highPriorityCount = highPriority.size,
                            avgScore = avgScore,
                            modelStatus = modelStatus,
                            recentTenders = recentHighPriority,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun refreshModelStatus() {
        val status = when {
            inferenceEngine.isReady() -> ModelStatus.READY
            else -> ModelStatus.NOT_LOADED
        }
        _state.update { it.copy(modelStatus = status) }
    }
}
