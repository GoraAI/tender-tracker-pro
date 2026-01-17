package com.tendertracker.app.ui.screens.compliance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.app.ui.components.charts.ComplianceItem
import com.tendertracker.app.ui.components.charts.TimelineMarker
import com.tendertracker.domain.model.ComplianceStatus
import com.tendertracker.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComplianceState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // Compliance items
    val complianceItems: List<ComplianceItem> = emptyList(),

    // Timeline markers
    val timelineMarkers: List<TimelineMarker> = emptyList(),

    // Summary
    val validCount: Int = 0,
    val totalCount: Int = 0,
    val daysToNextExpiry: Int = 0,
    val nextExpiryItem: String = "",

    // Empty state
    val isEmpty: Boolean = true
)

@HiltViewModel
class ComplianceViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ComplianceState())
    val state: StateFlow<ComplianceState> = _state.asStateFlow()

    init {
        loadComplianceData()
    }

    fun loadComplianceData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val result = analyticsRepository.getComplianceStatus()

                result.onSuccess { status ->
                    val complianceItems = status.toChartItems()

                    val timelineMarkers = complianceItems
                        .filter { it.daysRemaining > 0 && it.daysRemaining < 180 }
                        .map { item ->
                            TimelineMarker(
                                id = item.id,
                                label = item.shortName,
                                daysFromNow = item.daysRemaining,
                                description = item.expiryDate
                            )
                        }
                        .sortedBy { it.daysFromNow }

                    _state.update { it.copy(
                        isLoading = false,
                        complianceItems = complianceItems,
                        timelineMarkers = timelineMarkers,
                        validCount = status.validCount,
                        totalCount = status.totalCount,
                        daysToNextExpiry = status.daysToNextExpiry ?: 0,
                        nextExpiryItem = status.nextExpiryItem ?: "",
                        isEmpty = status.isEmpty
                    )}
                }.onFailure { e ->
                    _state.update { it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load compliance data"
                    )}
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load compliance data"
                )}
            }
        }
    }

    fun addComplianceItem(
        type: String,
        name: String,
        shortName: String,
        registrationNumber: String,
        issueDate: String,
        expiryDate: String? = null,
        expiryType: String = "annual",
        renewalUrl: String? = null
    ) {
        viewModelScope.launch {
            analyticsRepository.addComplianceItem(
                type = type,
                name = name,
                shortName = shortName,
                registrationNumber = registrationNumber,
                issueDate = issueDate,
                expiryDate = expiryDate,
                expiryType = expiryType,
                renewalUrl = renewalUrl
            )
            loadComplianceData()
        }
    }

    fun updateComplianceItem(itemId: String, newExpiryDate: String) {
        viewModelScope.launch {
            // Get current item to preserve other fields
            val currentItem = _state.value.complianceItems.find { it.id == itemId }
            if (currentItem != null) {
                analyticsRepository.updateComplianceItem(
                    itemId = itemId,
                    type = "registration", // Default type
                    name = currentItem.name,
                    shortName = currentItem.shortName,
                    registrationNumber = "", // Would need to store this
                    issueDate = "", // Would need to store this
                    expiryDate = newExpiryDate,
                    renewalUrl = currentItem.renewalUrl
                )
                loadComplianceData()
            }
        }
    }

    fun deleteComplianceItem(itemId: String) {
        viewModelScope.launch {
            analyticsRepository.deleteComplianceItem(itemId)
            loadComplianceData()
        }
    }

    fun openRenewalUrl(item: ComplianceItem) {
        // Intent handling would be done in the UI layer
    }
}

// Extension function to convert domain model to chart model
private fun ComplianceStatus.toChartItems(): List<ComplianceItem> {
    return items.map { item ->
        ComplianceItem(
            id = item.id,
            name = item.name,
            shortName = item.shortName,
            daysRemaining = item.daysRemaining ?: 180, // Default to 180 for perpetual
            expiryDate = item.expiryDate ?: "Perpetual",
            isValid = item.isValid,
            renewalUrl = item.renewalUrl
        )
    }
}
