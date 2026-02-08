package com.tendertracker.app.ui.screens.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.domain.model.TenderApplication
import com.tendertracker.domain.model.QualificationResult
import com.tendertracker.domain.repository.ApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplicationViewModel @Inject constructor(
    private val applicationRepository: ApplicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationUiState())
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()

    fun checkQualification(tenderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQualification = true, qualificationError = null) }

            applicationRepository.checkQualification(tenderId).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            isLoadingQualification = false,
                            qualificationResult = result
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoadingQualification = false,
                            qualificationError = exception.message
                        )
                    }
                }
            )
        }
    }

    fun createApplication(tenderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }

            applicationRepository.createApplication(tenderId).fold(
                onSuccess = { application ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            application = application,
                            createSuccess = true
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = exception.message
                        )
                    }
                }
            )
        }
    }

    fun loadApplication(applicationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            applicationRepository.getApplication(applicationId).fold(
                onSuccess = { application ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            application = application
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
            )
        }
    }

    fun updateField(fieldName: String, value: Any) {
        val applicationId = _uiState.value.application?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }

            val updates = mapOf(fieldName to value)
            applicationRepository.updateFields(applicationId, updates).fold(
                onSuccess = { application ->
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            application = application
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = exception.message
                        )
                    }
                }
            )
        }
    }

    fun updatePricing(pricingData: Map<String, Any>) {
        val applicationId = _uiState.value.application?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }

            applicationRepository.updatePricing(applicationId, pricingData).fold(
                onSuccess = { application ->
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            application = application
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = exception.message
                        )
                    }
                }
            )
        }
    }

    fun toggleTodoStatus(todoId: String, completed: Boolean) {
        val applicationId = _uiState.value.application?.id ?: return

        viewModelScope.launch {
            applicationRepository.updateTodoStatus(applicationId, todoId, completed).fold(
                onSuccess = { application ->
                    _uiState.update { it.copy(application = application) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(error = exception.message) }
                }
            )
        }
    }

    fun submitApplication() {
        val applicationId = _uiState.value.application?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }

            applicationRepository.submitApplication(applicationId).fold(
                onSuccess = { application ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            application = application,
                            submitSuccess = true
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = exception.message
                        )
                    }
                }
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null, qualificationError = null) }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(createSuccess = false, submitSuccess = false) }
    }
}

data class ApplicationUiState(
    val isLoadingQualification: Boolean = false,
    val isCreating: Boolean = false,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val isSubmitting: Boolean = false,
    val qualificationResult: QualificationResult? = null,
    val application: TenderApplication? = null,
    val createSuccess: Boolean = false,
    val submitSuccess: Boolean = false,
    val error: String? = null,
    val qualificationError: String? = null
)
