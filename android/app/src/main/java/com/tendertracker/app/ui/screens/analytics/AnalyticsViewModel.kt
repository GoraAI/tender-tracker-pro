package com.tendertracker.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.app.ui.components.charts.CostCategory
import com.tendertracker.app.ui.components.charts.CostItem
import com.tendertracker.app.ui.components.charts.FunnelStage
import com.tendertracker.app.ui.components.charts.PaymentDelayData
import com.tendertracker.domain.model.CostAnalytics
import com.tendertracker.domain.model.FunnelAnalytics
import com.tendertracker.domain.model.PaymentDelayAnalytics
import com.tendertracker.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    val selectedPeriod: String = "12 months",

    // Empty state flags
    val funnelIsEmpty: Boolean = true,
    val costIsEmpty: Boolean = true,
    val paymentDelayIsEmpty: Boolean = false
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
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
                val periodMonths = when (_state.value.selectedPeriod) {
                    "3 months" -> 3
                    "6 months" -> 6
                    "12 months" -> 12
                    "24 months" -> 24
                    else -> 12
                }

                // Load all analytics data from API in parallel
                val funnelResult = analyticsRepository.getFunnelAnalytics(periodMonths)
                val costResult = analyticsRepository.getCostAnalytics(periodMonths)
                val paymentResult = analyticsRepository.getPaymentDelayAnalytics()

                // Process funnel data
                val funnelData = funnelResult.getOrNull()
                val funnelStages = funnelData?.toChartStages() ?: emptyList()

                // Process cost data
                val costData = costResult.getOrNull()
                val costCategories = costData?.toChartCategories() ?: emptyList()

                // Process payment delay data
                val paymentData = paymentResult.getOrNull()
                val paymentDelayData = paymentData?.toChartData() ?: emptyList()

                _state.update { it.copy(
                    isLoading = false,
                    funnelStages = funnelStages,
                    winRate = funnelData?.winRate?.toFloat() ?: 0f,
                    avgDaysToAward = funnelData?.avgDaysToAward ?: 0,
                    costCategories = costCategories,
                    totalSpent = costData?.totalSpent ?: 0.0,
                    tendersWon = costData?.tendersWon ?: 0,
                    costPerWin = costData?.costPerWin ?: 0.0,
                    paymentDelayData = paymentDelayData,
                    funnelIsEmpty = funnelData?.isEmpty ?: true,
                    costIsEmpty = costData?.isEmpty ?: true,
                    paymentDelayIsEmpty = paymentData?.byProvince?.isEmpty() ?: true
                )}
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
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            analyticsRepository.addCostItem(
                category = category,
                description = description,
                amount = amount,
                date = today
            )
            loadAnalytics()
        }
    }
}

// Extension functions to convert domain models to chart models

private fun FunnelAnalytics.toChartStages(): List<FunnelStage> {
    return stages.map { stage ->
        FunnelStage(
            name = stage.name,
            count = stage.count,
            dropOffCount = stage.dropOffCount,
            dropOffReason = stage.dropOffReason
        )
    }
}

private fun CostAnalytics.toChartCategories(): List<CostCategory> {
    return categories.map { category ->
        CostCategory(
            name = category.name,
            amount = category.amount,
            percentage = category.percentage.toFloat(),
            items = category.items.map { item ->
                CostItem(
                    id = item.id,
                    description = item.description,
                    amount = item.amount,
                    date = item.date,
                    tenderId = item.tenderId
                )
            }
        )
    }
}

private fun PaymentDelayAnalytics.toChartData(): List<PaymentDelayData> {
    return byProvince.map { province ->
        PaymentDelayData(
            id = province.id,
            name = province.name,
            shortName = province.shortName,
            avgDaysToPayment = province.avgDaysToPayment,
            sampleSize = province.sampleSize,
            trend = province.trend.toFloat()
        )
    }
}
