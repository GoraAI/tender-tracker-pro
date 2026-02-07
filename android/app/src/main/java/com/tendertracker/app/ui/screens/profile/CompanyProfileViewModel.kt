package com.tendertracker.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.domain.model.ScoringWeights
import com.tendertracker.domain.model.UserCriteria
import com.tendertracker.domain.model.ValueRange
import com.tendertracker.domain.repository.CriteriaRepository
import com.tendertracker.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CompanyProfileViewModel @Inject constructor(
    private val criteriaRepository: CriteriaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyProfileUiState())
    val uiState: StateFlow<CompanyProfileUiState> = _uiState.asStateFlow()

    init {
        loadCriteria()
    }

    private fun loadCriteria() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = criteriaRepository.getCriteria()) {
                is Result.Success -> {
                    val defaultCriteria = result.data.firstOrNull { it.isDefault }
                    if (defaultCriteria != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                existingCriteria = defaultCriteria,
                                name = defaultCriteria.name,
                                capabilities = defaultCriteria.capabilities.toMutableList(),
                                preferredSectors = defaultCriteria.preferredSectors.toMutableList(),
                                preferredLocations = defaultCriteria.preferredLocations.toMutableList(),
                                valueRangeMin = defaultCriteria.valueRange.min.toString(),
                                valueRangeMax = defaultCriteria.valueRange.max?.toString() ?: "",
                                keywords = defaultCriteria.keywords.toMutableList(),
                                capabilityWeight = defaultCriteria.weights.capabilityMatch,
                                sectorWeight = defaultCriteria.weights.sectorMatch,
                                locationWeight = defaultCriteria.weights.locationMatch,
                                valueWeight = defaultCriteria.weights.valueMatch,
                                keywordWeight = defaultCriteria.weights.keywordMatch
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onCapabilityAdd(capability: String) {
        if (capability.isNotBlank()) {
            _uiState.update {
                it.copy(
                    capabilities = (it.capabilities + capability).toMutableList(),
                    capabilityInput = ""
                )
            }
        }
    }

    fun onCapabilityRemove(capability: String) {
        _uiState.update {
            it.copy(capabilities = it.capabilities.filter { c -> c != capability }.toMutableList())
        }
    }

    fun onCapabilityInputChange(input: String) {
        _uiState.update { it.copy(capabilityInput = input) }
    }

    fun onSectorAdd(sector: String) {
        if (sector.isNotBlank()) {
            _uiState.update {
                it.copy(
                    preferredSectors = (it.preferredSectors + sector).toMutableList(),
                    sectorInput = ""
                )
            }
        }
    }

    fun onSectorRemove(sector: String) {
        _uiState.update {
            it.copy(preferredSectors = it.preferredSectors.filter { s -> s != sector }.toMutableList())
        }
    }

    fun onSectorInputChange(input: String) {
        _uiState.update { it.copy(sectorInput = input) }
    }

    fun onLocationAdd(location: String) {
        if (location.isNotBlank()) {
            _uiState.update {
                it.copy(
                    preferredLocations = (it.preferredLocations + location).toMutableList(),
                    locationInput = ""
                )
            }
        }
    }

    fun onLocationRemove(location: String) {
        _uiState.update {
            it.copy(preferredLocations = it.preferredLocations.filter { l -> l != location }.toMutableList())
        }
    }

    fun onLocationInputChange(input: String) {
        _uiState.update { it.copy(locationInput = input) }
    }

    fun onValueRangeMinChange(min: String) {
        _uiState.update { it.copy(valueRangeMin = min) }
    }

    fun onValueRangeMaxChange(max: String) {
        _uiState.update { it.copy(valueRangeMax = max) }
    }

    fun onKeywordAdd(keyword: String) {
        if (keyword.isNotBlank()) {
            _uiState.update {
                it.copy(
                    keywords = (it.keywords + keyword).toMutableList(),
                    keywordInput = ""
                )
            }
        }
    }

    fun onKeywordRemove(keyword: String) {
        _uiState.update {
            it.copy(keywords = it.keywords.filter { k -> k != keyword }.toMutableList())
        }
    }

    fun onKeywordInputChange(input: String) {
        _uiState.update { it.copy(keywordInput = input) }
    }

    fun onCapabilityWeightChange(weight: Float) {
        _uiState.update { it.copy(capabilityWeight = weight.toDouble()) }
    }

    fun onSectorWeightChange(weight: Float) {
        _uiState.update { it.copy(sectorWeight = weight.toDouble()) }
    }

    fun onLocationWeightChange(weight: Float) {
        _uiState.update { it.copy(locationWeight = weight.toDouble()) }
    }

    fun onValueWeightChange(weight: Float) {
        _uiState.update { it.copy(valueWeight = weight.toDouble()) }
    }

    fun onKeywordWeightChange(weight: Float) {
        _uiState.update { it.copy(keywordWeight = weight.toDouble()) }
    }

    fun onSave() {
        viewModelScope.launch {
            val state = _uiState.value

            // Validation
            if (state.name.isBlank()) {
                _uiState.update { it.copy(error = "Profile name is required") }
                return@launch
            }

            val minValue = state.valueRangeMin.toDoubleOrNull()
            if (minValue == null) {
                _uiState.update { it.copy(error = "Minimum value must be a valid number") }
                return@launch
            }

            val maxValue = state.valueRangeMax.toDoubleOrNull()

            _uiState.update { it.copy(isSaving = true, error = null) }

            val criteria = UserCriteria(
                id = state.existingCriteria?.id ?: UUID.randomUUID().toString(),
                userId = "", // Will be set by backend
                name = state.name,
                capabilities = state.capabilities,
                preferredSectors = state.preferredSectors,
                preferredLocations = state.preferredLocations,
                valueRange = ValueRange(min = minValue, max = maxValue),
                keywords = state.keywords,
                weights = ScoringWeights(
                    capabilityMatch = state.capabilityWeight,
                    sectorMatch = state.sectorWeight,
                    locationMatch = state.locationWeight,
                    valueMatch = state.valueWeight,
                    keywordMatch = state.keywordWeight
                ),
                isDefault = true,
                createdAt = state.existingCriteria?.createdAt ?: Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )

            val result = if (state.existingCriteria != null) {
                criteriaRepository.updateCriteria(criteria)
            } else {
                criteriaRepository.createCriteria(criteria)
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            existingCriteria = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onDismissSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}

data class CompanyProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val existingCriteria: UserCriteria? = null,

    // Form fields
    val name: String = "My Company Profile",
    val capabilities: MutableList<String> = mutableListOf(),
    val capabilityInput: String = "",
    val preferredSectors: MutableList<String> = mutableListOf(),
    val sectorInput: String = "",
    val preferredLocations: MutableList<String> = mutableListOf(),
    val locationInput: String = "",
    val valueRangeMin: String = "100000",
    val valueRangeMax: String = "5000000",
    val keywords: MutableList<String> = mutableListOf(),
    val keywordInput: String = "",

    // Weights
    val capabilityWeight: Double = 0.4,
    val sectorWeight: Double = 0.2,
    val locationWeight: Double = 0.1,
    val valueWeight: Double = 0.1,
    val keywordWeight: Double = 0.2
)
