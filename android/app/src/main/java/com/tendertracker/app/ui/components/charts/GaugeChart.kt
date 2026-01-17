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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary
import com.tendertracker.app.ui.theme.getComplianceColor
import com.tendertracker.app.ui.theme.glassCard

/**
 * Compliance status for a single registration
 */
data class ComplianceItem(
    val id: String,
    val name: String,
    val shortName: String,
    val daysRemaining: Int,
    val expiryDate: String,
    val isValid: Boolean = true,
    val renewalUrl: String? = null
)

/**
 * Single gauge showing compliance status
 */
@Composable
fun ComplianceGauge(
    item: ComplianceItem,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    onRenewClick: (() -> Unit)? = null
) {
    val progress = (item.daysRemaining.coerceIn(0, 180) / 180f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "gauge_progress"
    )
    val gaugeColor = getComplianceColor(item.daysRemaining)

    Column(
        modifier = modifier
            .glassCard(cornerRadius = 12.dp)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gauge
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidthPx = strokeWidth.toPx()
                val arcSize = Size(size.toPx() - strokeWidthPx, size.toPx() - strokeWidthPx)
                val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

                // Background arc
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // Progress arc
                drawArc(
                    color = gaugeColor,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            // Center text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.shortName,
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status text
        Text(
            text = when {
                item.daysRemaining < 0 -> "Expired"
                item.daysRemaining == 0 -> "Today"
                item.daysRemaining == 1 -> "1 day"
                else -> "${item.daysRemaining} days"
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = gaugeColor
        )

        // Renew button if needed
        if (item.daysRemaining < 30 && onRenewClick != null) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onRenewClick,
                modifier = Modifier.height(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Renew",
                    modifier = Modifier.size(14.dp),
                    tint = gaugeColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Renew",
                    style = MaterialTheme.typography.labelSmall,
                    color = gaugeColor
                )
            }
        }
    }
}

/**
 * Row of compliance gauges
 */
@Composable
fun ComplianceGaugeRow(
    items: List<ComplianceItem>,
    modifier: Modifier = Modifier,
    onRenewClick: (ComplianceItem) -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.take(5).forEach { item ->
            ComplianceGauge(
                item = item,
                modifier = Modifier.weight(1f),
                size = 60.dp,
                strokeWidth = 6.dp,
                onRenewClick = { onRenewClick(item) }
            )
        }
    }
}

/**
 * Mini gauge for dashboard summary
 */
@Composable
fun ComplianceStatusSummary(
    validCount: Int,
    totalCount: Int,
    daysToNextExpiry: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val progress = validCount.toFloat() / totalCount.coerceAtLeast(1)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "summary_progress"
    )
    val statusColor = getComplianceColor(daysToNextExpiry)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mini gauge
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(48.dp)) {
                val strokeWidthPx = 5.dp.toPx()
                val arcSize = Size(48.dp.toPx() - strokeWidthPx, 48.dp.toPx() - strokeWidthPx)
                val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                drawArc(
                    color = statusColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "$validCount/$totalCount",
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextPrimary,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Compliance Status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = GlassTextPrimary
            )
            Text(
                text = when {
                    daysToNextExpiry < 0 -> "Action required - expired registration"
                    daysToNextExpiry < 7 -> "Urgent - expiring in $daysToNextExpiry days"
                    daysToNextExpiry < 30 -> "Next expiry in $daysToNextExpiry days"
                    else -> "All registrations valid"
                },
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
        }

        Text(
            text = "View",
            style = MaterialTheme.typography.labelMedium,
            color = GlassTextSecondary
        )
    }
}
