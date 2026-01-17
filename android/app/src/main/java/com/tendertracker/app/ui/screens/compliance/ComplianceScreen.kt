package com.tendertracker.app.ui.screens.compliance

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tendertracker.app.ui.components.charts.ComplianceGauge
import com.tendertracker.app.ui.components.charts.ComplianceGaugeRow
import com.tendertracker.app.ui.components.charts.ComplianceStatusSummary
import com.tendertracker.app.ui.components.charts.ExpirationTimeline
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassPrimary
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary
import com.tendertracker.app.ui.theme.glassBackground
import com.tendertracker.app.ui.theme.glassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplianceScreen(
    onNavigateBack: () -> Unit,
    viewModel: ComplianceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compliance Status",
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
                    // Summary Card
                    item {
                        ComplianceStatusSummary(
                            validCount = state.validCount,
                            totalCount = state.totalCount,
                            daysToNextExpiry = state.daysToNextExpiry
                        )
                    }

                    // Gauge Row
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Registration Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTextPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            ComplianceGaugeRow(
                                items = state.complianceItems,
                                onRenewClick = { item ->
                                    item.renewalUrl?.let { url ->
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                }
                            )
                        }
                    }

                    // Expiration Timeline
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Expiration Timeline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTextPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            ExpirationTimeline(
                                markers = state.timelineMarkers
                            )
                        }
                    }

                    // Individual compliance items
                    item {
                        Text(
                            text = "All Registrations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTextPrimary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(state.complianceItems) { item ->
                        ComplianceItemCard(
                            item = item,
                            onRenewClick = {
                                item.renewalUrl?.let { url ->
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                        )
                    }

                    // Bottom spacing
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

@Composable
private fun ComplianceItemCard(
    item: com.tendertracker.app.ui.components.charts.ComplianceItem,
    onRenewClick: () -> Unit
) {
    ComplianceGauge(
        item = item,
        modifier = Modifier.fillMaxWidth(),
        size = 100.dp,
        strokeWidth = 10.dp,
        onRenewClick = if (item.daysRemaining < 30) onRenewClick else null
    )
}
