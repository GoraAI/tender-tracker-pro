package com.tendertracker.app.ui.screens.pipeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tendertracker.app.ui.components.EmptyStates
import com.tendertracker.app.ui.components.charts.Milestone
import com.tendertracker.app.ui.components.charts.MilestoneTimeline
import com.tendertracker.app.ui.components.charts.PipelineCalendar
import com.tendertracker.app.ui.components.charts.PipelineSummary
import com.tendertracker.app.ui.components.charts.PipelineTender
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassPrimary
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary
import com.tendertracker.app.ui.theme.glassBackground
import com.tendertracker.app.ui.theme.glassCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineScreen(
    onNavigateBack: () -> Unit,
    onTenderClick: (String) -> Unit,
    viewModel: PipelineViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showTenderDetail by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pipeline",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Show add tender dialog */ },
                containerColor = GlassPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to Pipeline",
                    tint = Color.White
                )
            }
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
                    // Summary
                    item {
                        PipelineSummary(
                            activeCount = state.activeCount,
                            dueThisWeek = state.dueThisWeek,
                            hasConflicts = state.hasConflicts
                        )
                    }

                    if (state.isEmpty) {
                        // Empty state for new users
                        item {
                            EmptyStates.Pipeline(
                                onAddTender = { /* Show add tender dialog */ }
                            )
                        }
                    } else {
                        // Calendar
                        item {
                            PipelineCalendar(
                                tenders = state.tenders,
                                onTenderClick = { tender ->
                                    viewModel.selectTender(tender)
                                    showTenderDetail = true
                                },
                                onAddClick = { /* Show add dialog */ }
                            )
                        }
                    }

                    // Pipeline tips
                    item {
                        PipelineTips()
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

        // Tender detail bottom sheet
        if (showTenderDetail && state.selectedTender != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showTenderDetail = false
                    viewModel.selectTender(null)
                },
                sheetState = sheetState,
                containerColor = GlassEffect.cardBackground
            ) {
                TenderDetailSheet(
                    tender = state.selectedTender!!,
                    onViewDetails = {
                        showTenderDetail = false
                        onTenderClick(state.selectedTender!!.id)
                    },
                    onDismiss = {
                        scope.launch { sheetState.hide() }
                        showTenderDetail = false
                        viewModel.selectTender(null)
                    }
                )
            }
        }
    }
}

@Composable
private fun TenderDetailSheet(
    tender: PipelineTender,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = tender.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = GlassTextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = tender.reference,
            style = MaterialTheme.typography.bodySmall,
            color = GlassPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status and deadline
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(16.dp)
        ) {
            DetailRow(label = "Status", value = tender.status.name.replace("_", " "))
            DetailRow(label = "Deadline", value = tender.deadline.format(dateFormatter))
            DetailRow(
                label = "Days Remaining",
                value = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), tender.deadline).toString()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Milestones
        Text(
            text = "Milestones",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = GlassTextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        val milestones = tender.phases.map { phase ->
            Milestone(
                name = phase.name,
                date = phase.endDate.format(dateFormatter),
                isComplete = phase.isComplete,
                isCurrent = !phase.isComplete && tender.phases.indexOf(phase) ==
                    tender.phases.indexOfFirst { !it.isComplete }
            )
        }

        MilestoneTimeline(milestones = milestones)

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        androidx.compose.material3.Button(
            onClick = onViewDetails,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = GlassPrimary
            )
        ) {
            Text("View Tender Details")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = GlassTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = GlassTextPrimary
        )
    }
}

@Composable
private fun PipelineTips() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(16.dp)
    ) {
        Text(
            text = "Pipeline Tips",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = GlassTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap on a tender bar to see details. Drag to reschedule internal milestones. Watch for conflict warnings when multiple deadlines cluster.",
            style = MaterialTheme.typography.bodySmall,
            color = GlassTextSecondary
        )
    }
}
