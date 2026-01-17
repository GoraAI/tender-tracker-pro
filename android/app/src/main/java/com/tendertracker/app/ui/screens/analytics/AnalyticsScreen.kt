package com.tendertracker.app.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tendertracker.app.ui.components.EmptyStates
import com.tendertracker.app.ui.components.charts.CostPerBidCalculator
import com.tendertracker.app.ui.components.charts.HeatMapViewMode
import com.tendertracker.app.ui.components.charts.OpportunityFunnel
import com.tendertracker.app.ui.components.charts.PaymentRiskHeatMap
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassPrimary
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.glassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var heatMapViewMode by remember { mutableStateOf(HeatMapViewMode.PROVINCE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GlassTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GlassTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassBackground()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GlassPrimary
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = GlassEffect.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(GlassEffect.itemSpacing)
                ) {
                    // Opportunity Funnel
                    item {
                        if (state.funnelIsEmpty) {
                            EmptyStates.Funnel()
                        } else {
                            OpportunityFunnel(
                                stages = state.funnelStages,
                                winRate = state.winRate,
                                costPerWin = state.costPerWin,
                                avgDaysToAward = state.avgDaysToAward,
                                onStageClick = { /* Navigate to filtered tender list */ }
                            )
                        }
                    }

                    // Cost Per Bid Calculator
                    item {
                        if (state.costIsEmpty) {
                            EmptyStates.Costs(
                                onAddCost = { /* Show add cost dialog */ }
                            )
                        } else {
                            CostPerBidCalculator(
                                categories = state.costCategories,
                                totalSpent = state.totalSpent,
                                tendersWon = state.tendersWon,
                                periodLabel = state.selectedPeriod,
                                onAddCost = { /* Show add cost dialog */ },
                                onCategoryClick = { /* Show category details */ }
                            )
                        }
                    }

                    // Payment Risk Heat Map (always show - uses reference data)
                    item {
                        PaymentRiskHeatMap(
                            data = state.paymentDelayData,
                            viewMode = heatMapViewMode,
                            onItemClick = { /* Show payment details */ },
                            onViewModeChange = { heatMapViewMode = it }
                        )
                    }

                    // Bottom spacing for nav bar
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            // Error display
            state.error?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}
