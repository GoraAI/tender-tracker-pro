package com.tendertracker.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.app.ui.components.charts.CostCategory
import com.tendertracker.app.ui.components.charts.CostItem
import com.tendertracker.app.ui.components.charts.FunnelStage
import com.tendertracker.app.ui.components.charts.PaymentDelayData
import com.tendertracker.domain.repository.TenderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // Funnel data
    val funnelStages: List<FunnelStage> = emptyList(),
    val winRate: Float = 0f,
    val avgDaysToAward: Int = 0,

    // Cost data
    val costCategories: List<CostCategory> = emptyList(),
    val totalSpent: Double = 0.0,
    val tendersWon: Int = 0,
    val costPerWin: Double = 0.0,

    // Payment delay data
    val paymentDelayData: List<PaymentDelayData> = emptyList(),

    // Period
    val selectedPeriod: String = "12 months"
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val tenderRepository: TenderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Load tender data and calculate analytics
                tenderRepository.getAllTenders().collect { tenders ->
                    // Calculate funnel stages based on tender statuses
                    // For now, using mock data - in production would aggregate from tender status history
                    val discovered = tenders.size
                    val qualified = (discovered * 0.62).toInt()
                    val submitted = (qualified * 0.64).toInt()
                    val awarded = (submitted * 0.33).toInt()

                    val funnelStages = listOf(
                        FunnelStage(
                            name = "Discovered",
                            count = discovered,
                            dropOffCount = discovered - qualified,
                            dropOffReason = "filtered out"
                        ),
                        FunnelStage(
                            name = "Qualified",
                            count = qualified,
                            dropOffCount = qualified - submitted,
                            dropOffReason = "no-bid decision"
                        ),
                        FunnelStage(
                            name = "Submitted",
                            count = submitted,
                            dropOffCount = submitted - awarded,
                            dropOffReason = "unsuccessful"
                        ),
                        FunnelStage(
                            name = "Awarded",
                            count = awarded
                        )
                    )

                    val winRate = if (submitted > 0) awarded.toFloat() / submitted else 0f

                    // Mock cost data - in production would come from cost tracking
                    val costCategories = listOf(
                        CostCategory(
                            name = "Platform",
                            amount = 17100.0,
                            percentage = 0.09f,
                            items = listOf(
                                CostItem("1", "Leads 2 Business Annual", 17112.0, "2025-01-01")
                            )
                        ),
                        CostCategory(
                            name = "Compliance",
                            amount = 24400.0,
                            percentage = 0.13f,
                            items = listOf(
                                CostItem("2", "B-BBEE Verification", 12000.0, "2025-03-15"),
                                CostItem("3", "CIDB Renewal", 2400.0, "2025-06-01"),
                                CostItem("4", "Tax Clearance", 0.0, "2025-01-01"),
                                CostItem("5", "Other compliance", 10000.0, "2025-01-01")
                            )
                        ),
                        CostCategory(
                            name = "Preparation",
                            amount = 98000.0,
                            percentage = 0.52f,
                            items = listOf(
                                CostItem("6", "Staff time (120 hrs @ R500)", 60000.0),
                                CostItem("7", "Consultant fees", 25000.0),
                                CostItem("8", "Site visits & briefings", 8000.0),
                                CostItem("9", "Document production", 5000.0)
                            )
                        ),
                        CostCategory(
                            name = "Submission",
                            amount = 18000.0,
                            percentage = 0.10f,
                            items = listOf(
                                CostItem("10", "Printing & binding", 5000.0),
                                CostItem("11", "Courier & delivery", 3000.0),
                                CostItem("12", "Travel", 10000.0)
                            )
                        ),
                        CostCategory(
                            name = "Opportunity",
                            amount = 30000.0,
                            percentage = 0.16f,
                            items = listOf(
                                CostItem("13", "Lost billable hours", 30000.0)
                            )
                        )
                    )

                    val totalSpent = costCategories.sumOf { it.amount }
                    val tendersWon = awarded.coerceAtLeast(1)
                    val costPerWin = totalSpent / tendersWon

                    // Mock payment delay data
                    val paymentDelayData = listOf(
                        PaymentDelayData("GP", "Gauteng", "GP", 34, 45, -2f),
                        PaymentDelayData("WC", "Western Cape", "WC", 29, 38, -5f),
                        PaymentDelayData("KZN", "KwaZulu-Natal", "KZN", 38, 52, 3f),
                        PaymentDelayData("EC", "Eastern Cape", "EC", 71, 23, 8f),
                        PaymentDelayData("FS", "Free State", "FS", 67, 19, 5f),
                        PaymentDelayData("NW", "North West", "NW", 98, 15, 12f),
                        PaymentDelayData("LP", "Limpopo", "LP", 52, 28, 0f),
                        PaymentDelayData("MP", "Mpumalanga", "MP", 45, 31, -3f),
                        PaymentDelayData("NC", "Northern Cape", "NC", 41, 12, 2f)
                    )

                    _state.update { it.copy(
                        isLoading = false,
                        funnelStages = funnelStages,
                        winRate = winRate,
                        avgDaysToAward = 47,
                        costCategories = costCategories,
                        totalSpent = totalSpent,
                        tendersWon = tendersWon,
                        costPerWin = costPerWin,
                        paymentDelayData = paymentDelayData
                    )}
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load analytics"
                )}
            }
        }
    }

    fun setPeriod(period: String) {
        _state.update { it.copy(selectedPeriod = period) }
        loadAnalytics()
    }

    fun addCost(category: String, description: String, amount: Double) {
        // In production, this would persist to backend
        viewModelScope.launch {
            // Add cost and refresh
            loadAnalytics()
        }
    }
}
