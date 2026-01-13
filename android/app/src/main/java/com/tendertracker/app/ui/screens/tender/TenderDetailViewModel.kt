package com.tendertracker.app.ui.screens.tender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class TenderDetailState(
    val tender: Tender? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TenderDetailViewModel @Inject constructor(
    private val tenderRepository: TenderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TenderDetailState())
    val state: StateFlow<TenderDetailState> = _state.asStateFlow()

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
}
