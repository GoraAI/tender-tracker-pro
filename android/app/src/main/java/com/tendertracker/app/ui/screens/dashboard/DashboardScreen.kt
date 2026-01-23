package com.tendertracker.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tendertracker.app.ui.components.AttentionSection
import com.tendertracker.app.ui.components.NewOpportunitiesCard
import com.tendertracker.app.ui.components.ScoreBadge
import com.tendertracker.app.ui.components.charts.ComplianceStatusSummary
import com.tendertracker.app.ui.components.charts.FunnelSummary
import com.tendertracker.app.ui.components.charts.PipelineSummary
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassPrimary
import com.tendertracker.app.ui.theme.GlassSecondary
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary
import com.tendertracker.app.ui.theme.StatusSuccess
import com.tendertracker.app.ui.theme.StatusWarning
import com.tendertracker.app.ui.theme.glassBackground
import com.tendertracker.app.ui.theme.glassCard
import com.tendertracker.domain.model.Tender

@Composable
fun DashboardScreen(
    onTenderClick: (String) -> Unit,
    onViewAllTenders: () -> Unit,
    onChatClick: () -> Unit,
    onModelManagerClick: () -> Unit,
    onAnalyticsClick: () -> Unit = {},
    onComplianceClick: () -> Unit = {},
    onPipelineClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshModelStatus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .glassBackground()
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
                    .padding(GlassEffect.screenPadding),
                verticalArrangement = Arrangement.spacedBy(GlassEffect.itemSpacing)
            ) {
                // Header
                item {
                    DashboardHeader(
                        onRefresh = { viewModel.runPipelineSync() },
                        isSyncing = state.isSyncing
                    )
                }

                // Sync Progress Banner
                if (state.isSyncing || state.syncMessage != null) {
                    item {
                        SyncProgressBanner(
                            progress = state.syncProgress,
                            message = state.syncMessage ?: "Syncing..."
                        )
                    }
                }

                // Attention-Prioritized: Compliance + Pipeline Summary Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ComplianceStatusSummary(
                            validCount = state.complianceValidCount,
                            totalCount = state.complianceTotalCount,
                            daysToNextExpiry = state.complianceDaysToExpiry,
                            modifier = Modifier.weight(1f),
                            onClick = onComplianceClick
                        )
                    }
                }

                // Pipeline Summary
                item {
                    PipelineSummary(
                        activeCount = state.pipelineActiveCount,
                        dueThisWeek = state.pipelineDueThisWeek,
                        hasConflicts = state.pipelineHasConflicts,
                        onClick = onPipelineClick
                    )
                }

                // Attention Items (urgent actions)
                if (state.attentionItems.isNotEmpty()) {
                    item {
                        AttentionSection(
                            items = state.attentionItems,
                            onItemAction = { item ->
                                when {
                                    item.id.startsWith("tender-") -> {
                                        val tenderId = item.id.removePrefix("tender-")
                                        onTenderClick(tenderId)
                                    }
                                    item.id.contains("expiry") -> onComplianceClick()
                                }
                            }
                        )
                    }
                }

                // Stats Grid
                item {
                    StatsGrid(
                        totalTenders = state.totalTenders,
                        highPriorityCount = state.highPriorityCount,
                        avgScore = state.avgScore,
                        modelStatus = state.modelStatus,
                        onModelClick = onModelManagerClick,
                        onViewAllTenders = onViewAllTenders
                    )
                }

                // Quick Actions with Analytics
                item {
                    QuickActionsCard(
                        onViewAllTenders = onViewAllTenders,
                        onChatClick = onChatClick,
                        onModelManagerClick = onModelManagerClick,
                        onAnalyticsClick = onAnalyticsClick
                    )
                }

                // Funnel Summary
                if (state.funnelDiscovered > 0) {
                    item {
                        FunnelSummary(
                            discovered = state.funnelDiscovered,
                            qualified = state.funnelQualified,
                            submitted = state.funnelSubmitted,
                            awarded = state.funnelAwarded,
                            onClick = onAnalyticsClick
                        )
                    }
                }

                // New Opportunities
                if (state.recentTenders.isNotEmpty()) {
                    item {
                        SectionHeader(title = "New Opportunities")
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.recentTenders.take(3)) { tender ->
                                NewOpportunitiesCard(
                                    title = tender.title.take(30),
                                    value = tender.estimatedValue?.let { "R${formatValue(it)}" } ?: "TBD",
                                    score = tender.score ?: 0,
                                    modifier = Modifier.width(160.dp),
                                    onClick = { onTenderClick(tender.id) }
                                )
                            }
                        }
                    }
                }

                // Recent High-Priority Tenders
                item {
                    SectionHeader(title = "Recent High-Priority Tenders")
                }

                if (state.recentTenders.isEmpty()) {
                    item {
                        EmptyTendersCard()
                    }
                } else {
                    items(state.recentTenders) { tender ->
                        RecentTenderCard(
                            tender = tender,
                            onClick = { onTenderClick(tender.id) }
                        )
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Error display
        state.error?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = error,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    onRefresh: () -> Unit,
    isSyncing: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GlassTextPrimary
            )
            Text(
                text = "Tender Tracker Pro",
                style = MaterialTheme.typography.bodyMedium,
                color = GlassTextSecondary
            )
        }
        IconButton(
            onClick = onRefresh,
            enabled = !isSyncing,
            modifier = Modifier
                .clip(CircleShape)
                .background(GlassEffect.cardBackground)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = GlassPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync Tenders",
                    tint = GlassPrimary
                )
            }
        }
    }
}

@Composable
private fun SyncProgressBanner(
    progress: Int,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = GlassPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTextPrimary
                )
            }
            if (progress > 0 && progress < 100) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = GlassPrimary,
                    trackColor = GlassPrimary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(
    totalTenders: Int,
    highPriorityCount: Int,
    avgScore: Float,
    modelStatus: ModelStatus,
    onModelClick: () -> Unit,
    onViewAllTenders: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Total Tenders",
                value = totalTenders.toString(),
                icon = Icons.Default.Description,
                iconTint = GlassPrimary,
                onClick = onViewAllTenders
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "High Priority",
                value = highPriorityCount.toString(),
                icon = Icons.Default.PriorityHigh,
                iconTint = GlassSecondary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Avg Score",
                value = "%.1f".format(avgScore),
                icon = Icons.Default.Assessment,
                iconTint = StatusWarning
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "AI Model",
                value = modelStatus.displayName,
                icon = Icons.Default.Memory,
                iconTint = if (modelStatus == ModelStatus.READY) StatusSuccess else GlassTextSecondary,
                onClick = onModelClick
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .glassCard()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GlassTextPrimary
            )
        }
    }
}

@Composable
private fun QuickActionsCard(
    onViewAllTenders: () -> Unit,
    onChatClick: () -> Unit,
    onModelManagerClick: () -> Unit,
    onAnalyticsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(16.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = GlassTextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Description,
                label = "Tenders",
                onClick = onViewAllTenders
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Analytics,
                label = "Analytics",
                onClick = onAnalyticsClick
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.Chat,
                label = "AI Chat",
                onClick = onChatClick
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Memory,
                label = "AI Model",
                onClick = onModelManagerClick
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GlassPrimary.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GlassPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GlassTextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = GlassTextPrimary
        )
    }
}

@Composable
private fun RecentTenderCard(
    tender: Tender,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tender.reference,
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tender.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                tender.deadline?.let { deadline ->
                    Text(
                        text = "Deadline: $deadline",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            tender.score?.let { score ->
                ScoreBadge(score = score)
            }
        }
    }
}

@Composable
private fun EmptyTendersCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = GlassTextSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No high-priority tenders yet",
                style = MaterialTheme.typography.bodyMedium,
                color = GlassTextSecondary
            )
            Text(
                text = "Tenders with score 70+ will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatValue(amount: Double): String {
    return when {
        amount >= 1_000_000 -> String.format("%.1fM", amount / 1_000_000)
        amount >= 1_000 -> String.format("%.0fK", amount / 1_000)
        else -> String.format("%.0f", amount)
    }
}
