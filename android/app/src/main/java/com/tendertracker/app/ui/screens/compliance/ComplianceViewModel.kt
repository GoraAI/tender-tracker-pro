package com.tendertracker.app.ui.screens.compliance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.app.ui.components.charts.ComplianceItem
import com.tendertracker.app.ui.components.charts.TimelineMarker
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
    val nextExpiryItem: String = ""
)

@HiltViewModel
class ComplianceViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ComplianceState())
    val state: StateFlow<ComplianceState> = _state.asStateFlow()

    init {
        loadComplianceData()
    }

    fun loadComplianceData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // In production, this would load from user's saved compliance data
                // For now, using representative mock data
                val complianceItems = listOf(
                    ComplianceItem(
                        id = "csd",
                        name = "Central Supplier Database",
                        shortName = "CSD",
                        daysRemaining = 180,  // CSD doesn't expire but needs updating
                        expiryDate = "Ongoing",
                        isValid = true,
                        renewalUrl = "https://secure.csd.gov.za"
                    ),
                    ComplianceItem(
                        id = "cidb",
                        name = "CIDB Registration",
                        shortName = "CIDB",
                        daysRemaining = 45,
                        expiryDate = "2026-03-03",
                        isValid = true,
                        renewalUrl = "https://www.cidb.org.za"
                    ),
                    ComplianceItem(
                        id = "bbbee",
                        name = "B-BBEE Certificate",
                        shortName = "B-BBEE",
                        daysRemaining = 12,
                        expiryDate = "2026-01-29",
                        isValid = true,
                        renewalUrl = null
                    ),
                    ComplianceItem(
                        id = "sars",
                        name = "Tax Clearance (SARS)",
                        shortName = "SARS",
                        daysRemaining = 120,
                        expiryDate = "Continuous",
                        isValid = true,
                        renewalUrl = "https://www.sarsefiling.co.za"
                    ),
                    ComplianceItem(
                        id = "uif",
                        name = "UIF Letter of Good Standing",
                        shortName = "UIF",
                        daysRemaining = 98,
                        expiryDate = "2026-04-25",
                        isValid = true,
                        renewalUrl = "https://www.ufiling.co.za"
                    ),
                    ComplianceItem(
                        id = "coid",
                        name = "COID Letter of Good Standing",
                        shortName = "COID",
                        daysRemaining = 156,
                        expiryDate = "2026-06-22",
                        isValid = true,
                        renewalUrl = null
                    )
                )

                val validItems = complianceItems.filter { it.isValid && it.daysRemaining > 0 }
                val nextExpiry = complianceItems
                    .filter { it.daysRemaining > 0 }
                    .minByOrNull { it.daysRemaining }

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
                    validCount = validItems.size,
                    totalCount = complianceItems.size,
                    daysToNextExpiry = nextExpiry?.daysRemaining ?: 0,
                    nextExpiryItem = nextExpiry?.shortName ?: ""
                )}
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load compliance data"
                )}
            }
        }
    }

    fun updateComplianceItem(itemId: String, newExpiryDate: String) {
        viewModelScope.launch {
            // In production, persist to backend
            loadComplianceData()
        }
    }

    fun openRenewalUrl(item: ComplianceItem) {
        // In production, open URL in browser
        // Intent(Intent.ACTION_VIEW, Uri.parse(item.renewalUrl))
    }
}
