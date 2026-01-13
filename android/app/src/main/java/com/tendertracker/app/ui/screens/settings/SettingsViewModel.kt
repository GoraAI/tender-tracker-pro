package com.tendertracker.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val modelStatus: String = "Not loaded",
    val autoSyncEnabled: Boolean = true,
    val wifiOnlySync: Boolean = false,
    val notificationsEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getAutoSync().collect { enabled ->
                _state.update { it.copy(autoSyncEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.getWifiOnlySync().collect { enabled ->
                _state.update { it.copy(wifiOnlySync = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.getNotificationsEnabled().collect { enabled ->
                _state.update { it.copy(notificationsEnabled = enabled) }
            }
        }
    }

    fun onAutoSyncChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSync(enabled)
            _state.update { it.copy(autoSyncEnabled = enabled) }
        }
    }

    fun onWifiOnlyChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWifiOnlySync(enabled)
            _state.update { it.copy(wifiOnlySync = enabled) }
        }
    }

    fun onNotificationsChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            _state.update { it.copy(notificationsEnabled = enabled) }
        }
    }

    fun updateModelStatus(status: String) {
        _state.update { it.copy(modelStatus = status) }
    }
}
