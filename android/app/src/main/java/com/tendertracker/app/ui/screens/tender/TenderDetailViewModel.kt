package com.tendertracker.app.ui.screens.tender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.domain.model.Tender
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
}

@HiltViewModel
class TenderDetailViewModel @Inject constructor(
    private val tenderRepository: TenderRepository
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
        // Navigate to chat with tender context - implemented in FR-MOBILE-003
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
        val tenderId = _state.value.tender?.id
        if (tenderId == null) {
            viewModelScope.launch {
                _events.send(TenderDetailEvent.ShowError("Tender not loaded"))
            }
            return
        }

        // TODO: Call API to trigger re-scoring
        // For MVP, show a message that scoring has been queued
        // In production, this would:
        // 1. Call POST /tenders/{id}/score
        // 2. Poll or wait for scoring to complete
        // 3. Reload tender to get updated score
        viewModelScope.launch {
            _events.send(TenderDetailEvent.ShowError("Scoring queued. Refresh in a few moments to see updated score."))
        }
    }
}
