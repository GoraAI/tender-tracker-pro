package com.tendertracker.app.ui.screens.tender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.domain.model.Tender
import com.tendertracker.domain.repository.CriteriaRepository
import com.tendertracker.domain.repository.TenderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TenderDetailState(
    val tender: Tender? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class TenderDetailEvent {
    data class OpenUrl(val url: String) : TenderDetailEvent()
    data class DownloadPdf(val url: String, val filename: String) : TenderDetailEvent()
    data class ShowError(val message: String) : TenderDetailEvent()
    data class NavigateToChat(val tenderId: String) : TenderDetailEvent()
}

@HiltViewModel
class TenderDetailViewModel @Inject constructor(
    private val tenderRepository: TenderRepository,
    private val scoringService: com.tendertracker.app.gemma.ScoringService,
    private val criteriaRepository: CriteriaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TenderDetailState())
    val state: StateFlow<TenderDetailState> = _state.asStateFlow()

    private val _events = Channel<TenderDetailEvent>()
    val events = _events.receiveAsFlow()

    fun loadTender(tenderId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            tenderRepository.getTenderById(tenderId)
                .catch { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { tender ->
                    _state.update { it.copy(tender = tender, isLoading = false) }
                }
        }
    }

    fun onAskAI() {
        val tenderId = _state.value.tender?.id
        if (tenderId != null) {
            viewModelScope.launch {
                _events.send(TenderDetailEvent.NavigateToChat(tenderId))
            }
        }
    }

    fun openPdf() {
        val pdfUrl = _state.value.tender?.pdfUrl
        if (pdfUrl.isNullOrBlank()) {
            viewModelScope.launch {
                _events.send(TenderDetailEvent.ShowError("PDF not available for this tender"))
            }
            return
        }
        viewModelScope.launch {
            _events.send(TenderDetailEvent.OpenUrl(pdfUrl))
        }
    }

    fun downloadPdf() {
        val tender = _state.value.tender
        val pdfUrl = tender?.pdfUrl
        if (pdfUrl.isNullOrBlank()) {
            viewModelScope.launch {
                _events.send(TenderDetailEvent.ShowError("PDF not available for this tender"))
            }
            return
        }
        val filename = "${tender.reference.replace("/", "_")}.pdf"
        viewModelScope.launch {
            _events.send(TenderDetailEvent.DownloadPdf(pdfUrl, filename))
        }
    }

    fun rescore() {
        val tender = _state.value.tender
        if (tender == null) {
            viewModelScope.launch {
                _events.send(TenderDetailEvent.ShowError("Tender not loaded"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Fetch user's default criteria to build a profile string
            val profileString = try {
                val criteriaResult = criteriaRepository.getDefaultCriteria()
                criteriaResult.getOrNull()?.let { criteria ->
                    buildString {
                        if (criteria.capabilities.isNotEmpty()) {
                            append("Capabilities: ${criteria.capabilities.joinToString(", ")}. ")
                        }
                        if (criteria.preferredSectors.isNotEmpty()) {
                            append("Sectors: ${criteria.preferredSectors.joinToString(", ")}. ")
                        }
                        if (criteria.preferredLocations.isNotEmpty()) {
                            append("Locations: ${criteria.preferredLocations.joinToString(", ")}. ")
                        }
                        if (criteria.keywords.isNotEmpty()) {
                            append("Keywords: ${criteria.keywords.joinToString(", ")}. ")
                        }
                        append("Value range: R${"%,.0f".format(criteria.valueRange.min)}")
                        criteria.valueRange.max?.let { max ->
                            append(" - R${"%,.0f".format(max)}")
                        }
                        append(".")
                    }
                } ?: ""
            } catch (_: Exception) {
                ""
            }

            val result = scoringService.scoreTender(tender, profileString)

            result.fold(
                onSuccess = { tenderScore ->
                    // Update tender score in local DB
                    tenderRepository.updateScore(
                        id = tender.id,
                        score = tenderScore.score,
                        explanation = tenderScore.explanation
                    )
                    // Submit score to backend (fire-and-forget)
                    launch {
                        try {
                            tenderRepository.submitScoreToBackend(
                                tenderId = tender.id,
                                score = tenderScore.score,
                                explanation = tenderScore.explanation
                            )
                        } catch (_: Exception) {
                            // Backend submission is best-effort
                        }
                    }
                    // Update local state
                    val updatedTender = tender.copy(
                        score = tenderScore.score,
                        scoreExplanation = tenderScore.explanation
                    )
                    _state.update { it.copy(tender = updatedTender, isLoading = false) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false) }
                    _events.send(
                        TenderDetailEvent.ShowError(
                            error.message ?: "Failed to score tender. Make sure the AI model is loaded."
                        )
                    )
                }
            )
        }
    }
}
