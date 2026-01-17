package com.tendertracker.app.ui.components.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tendertracker.app.ui.theme.FunnelAwarded
import com.tendertracker.app.ui.theme.FunnelDiscovered
import com.tendertracker.app.ui.theme.FunnelDropped
import com.tendertracker.app.ui.theme.FunnelQualified
import com.tendertracker.app.ui.theme.FunnelSubmitted
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary
import com.tendertracker.app.ui.theme.glassCard

/**
 * Funnel stage data
 */
data class FunnelStage(
    val name: String,
    val count: Int,
    val dropOffCount: Int = 0,
    val dropOffReason: String = ""
)

/**
 * Opportunity funnel visualization
 */
@Composable
fun OpportunityFunnel(
    stages: List<FunnelStage>,
    modifier: Modifier = Modifier,
    winRate: Float = 0f,
    costPerWin: Double = 0.0,
    avgDaysToAward: Int = 0,
    onStageClick: (FunnelStage) -> Unit = {}
) {
    var selectedStage by remember { mutableStateOf<FunnelStage?>(null) }
    val maxCount = stages.maxOfOrNull { it.count } ?: 1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Opportunity Funnel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = GlassTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Funnel stages
        stages.forEachIndexed { index, stage ->
            val stageColor = when (index) {
                0 -> FunnelDiscovered
                1 -> FunnelQualified
                2 -> FunnelSubmitted
                3 -> FunnelAwarded
                else -> FunnelDropped
            }

            FunnelStageRow(
                stage = stage,
                color = stageColor,
                maxCount = maxCount,
                isSelected = selectedStage == stage,
                onClick = {
                    selectedStage = if (selectedStage == stage) null else stage
                    onStageClick(stage)
                }
            )

            // Drop-off annotation
            if (stage.dropOffCount > 0 && index < stages.size - 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = FunnelDropped.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${stage.dropOffCount} ${stage.dropOffReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = FunnelDropped.copy(alpha = 0.8f)
                    )
                }
            }

            if (index < stages.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Summary metrics
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FunnelMetric(
                label = "Win Rate",
                value = "${(winRate * 100).toInt()}%"
            )
            FunnelMetric(
                label = "Cost/Win",
                value = "R${formatCurrency(costPerWin)}"
            )
            FunnelMetric(
                label = "Avg Days",
                value = avgDaysToAward.toString()
            )
        }
    }
}

@Composable
private fun FunnelStageRow(
    stage: FunnelStage,
    color: Color,
    maxCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val widthFraction = (stage.count.toFloat() / maxCount).coerceIn(0.2f, 1f)
    val animatedWidth by animateFloatAsState(
        targetValue = widthFraction,
        animationSpec = tween(durationMillis = 600),
        label = "funnel_width"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Funnel bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
        ) {
            // Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            )

            // Filled portion
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedWidth)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) color else color.copy(alpha = 0.7f)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stage.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = GlassTextPrimary,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Count
        Text(
            text = "(${stage.count})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun FunnelMetric(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = GlassTextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GlassTextSecondary
        )
    }
}

/**
 * Compact funnel for dashboard
 */
@Composable
fun FunnelSummary(
    discovered: Int,
    qualified: Int,
    submitted: Int,
    awarded: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val winRate = if (submitted > 0) awarded.toFloat() / submitted else 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mini funnel visualization
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MiniFunnelBar(count = discovered, maxCount = discovered, color = FunnelDiscovered, label = "Discovered")
            MiniFunnelBar(count = qualified, maxCount = discovered, color = FunnelQualified, label = "Qualified")
            MiniFunnelBar(count = submitted, maxCount = discovered, color = FunnelSubmitted, label = "Submitted")
            MiniFunnelBar(count = awarded, maxCount = discovered, color = FunnelAwarded, label = "Awarded")
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Win rate
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(winRate * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = FunnelAwarded
            )
            Text(
                text = "Win Rate",
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextSecondary
            )
        }
    }
}

@Composable
private fun MiniFunnelBar(
    count: Int,
    maxCount: Int,
    color: Color,
    label: String
) {
    val width = (count.toFloat() / maxCount.coerceAtLeast(1)).coerceIn(0.1f, 1f)

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(width)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.7f))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.width(24.dp)
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return when {
        amount >= 1_000_000 -> String.format("%.1fM", amount / 1_000_000)
        amount >= 1_000 -> String.format("%.0fK", amount / 1_000)
        else -> String.format("%.0f", amount)
    }
}
