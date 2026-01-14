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

data class TenderListState(
    val tenders: List<Tender> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSearchVisible: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class TenderListViewModel @Inject constructor(
    private val tenderRepository: TenderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TenderListState())
    val state: StateFlow<TenderListState> = _state.asStateFlow()

    init {
        loadTenders()
    }

    private fun loadTenders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            tenderRepository.getAllTenders()
                .catch { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { tenders ->
                    _state.update { it.copy(tenders = tenders, isLoading = false) }
                }
        }
    }

    fun onSearchClick() {
        _state.update { it.copy(isSearchVisible = !it.isSearchVisible) }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchTenders(query)
    }

    private fun searchTenders(query: String) {
        viewModelScope.launch {
            tenderRepository.searchTenders(query)
                .catch { e ->
                    _state.update { it.copy(error = e.message) }
                }
                .collect { tenders ->
                    _state.update { it.copy(tenders = tenders) }
                }
        }
    }

    fun onSyncClick() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            tenderRepository.syncFromApi()
                .onSuccess { count ->
                    _state.update { it.copy(isLoading = false) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Sync failed"
                        )
                    }
                }
        }
    }
}
