package com.tendertracker.app.ui.components.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary
import com.tendertracker.app.ui.theme.RadarAxisLabel
import com.tendertracker.app.ui.theme.RadarFill
import com.tendertracker.app.ui.theme.RadarGridLine
import com.tendertracker.app.ui.theme.RadarStroke
import com.tendertracker.app.ui.theme.RadarWinnerFill
import com.tendertracker.app.ui.theme.RadarWinnerStroke
import com.tendertracker.app.ui.theme.StatusSuccess
import com.tendertracker.app.ui.theme.StatusWarning
import com.tendertracker.app.ui.theme.glassCard
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Radar chart axis data
 */
data class RadarAxis(
    val name: String,
    val value: Float,        // 0-100 score
    val weight: Float = 1f,  // Weight in evaluation (e.g., 0.6 for 60%)
    val benchmark: Float? = null  // Optional benchmark value
)

/**
 * Bid strength assessment insights
 */
data class BidStrengthInsight(
    val isStrength: Boolean,
    val text: String
)

/**
 * Bid strength radar chart
 */
@Composable
fun BidStrengthRadar(
    axes: List<RadarAxis>,
    overallScore: Int,
    modifier: Modifier = Modifier,
    showBenchmark: Boolean = false,
    insights: List<BidStrengthInsight> = emptyList(),
    tenderTitle: String = ""
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .padding(16.dp)
    ) {
        // Header
        if (tenderTitle.isNotEmpty()) {
            Text(
                text = "Bid Strength: $tenderTitle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = GlassTextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Radar chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarChartCanvas(
                axes = axes,
                showBenchmark = showBenchmark
            )

            // Center score
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = overallScore.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = GlassTextPrimary
                )
                Text(
                    text = "/100",
                    style = MaterialTheme.typography.labelMedium,
                    color = GlassTextSecondary
                )
            }
        }

        // Axis legend
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            axes.forEach { axis ->
                AxisLegendItem(axis = axis)
            }
        }

        // Insights
        if (insights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            insights.forEach { insight ->
                BidInsightRow(insight = insight)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun RadarChartCanvas(
    axes: List<RadarAxis>,
    showBenchmark: Boolean
) {
    val animatedValues = axes.map { axis ->
        animateFloatAsState(
            targetValue = axis.value / 100f,
            animationSpec = tween(durationMillis = 800),
            label = "radar_${axis.name}"
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = minOf(centerX, centerY) * 0.85f
        val axisCount = axes.size
        val angleStep = (2 * PI / axisCount).toFloat()

        // Draw grid circles
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { level ->
            drawCircle(
                color = RadarGridLine,
                radius = radius * level,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Draw axis lines
        for (i in 0 until axisCount) {
            val angle = -PI.toFloat() / 2 + i * angleStep
            val endX = centerX + radius * cos(angle)
            val endY = centerY + radius * sin(angle)
            drawLine(
                color = RadarGridLine,
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw benchmark polygon if enabled
        if (showBenchmark) {
            val benchmarkPath = Path()
            axes.forEachIndexed { index, axis ->
                val benchmarkValue = (axis.benchmark ?: 70f) / 100f
                val angle = -PI.toFloat() / 2 + index * angleStep
                val x = centerX + radius * benchmarkValue * cos(angle)
                val y = centerY + radius * benchmarkValue * sin(angle)
                if (index == 0) {
                    benchmarkPath.moveTo(x, y)
                } else {
                    benchmarkPath.lineTo(x, y)
                }
            }
            benchmarkPath.close()

            drawPath(
                path = benchmarkPath,
                color = RadarWinnerFill,
                style = Fill
            )
            drawPath(
                path = benchmarkPath,
                color = RadarWinnerStroke,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Draw value polygon
        val valuePath = Path()
        animatedValues.forEachIndexed { index, animatedValue ->
            val value by animatedValue
            val angle = -PI.toFloat() / 2 + index * angleStep
            val x = centerX + radius * value * cos(angle)
            val y = centerY + radius * value * sin(angle)
            if (index == 0) {
                valuePath.moveTo(x, y)
            } else {
                valuePath.lineTo(x, y)
            }
        }
        valuePath.close()

        drawPath(
            path = valuePath,
            color = RadarFill,
            style = Fill
        )
        drawPath(
            path = valuePath,
            color = RadarStroke,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw value points
        animatedValues.forEachIndexed { index, animatedValue ->
            val value by animatedValue
            val angle = -PI.toFloat() / 2 + index * angleStep
            val x = centerX + radius * value * cos(angle)
            val y = centerY + radius * value * sin(angle)
            drawCircle(
                color = RadarStroke,
                radius = 6.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun AxisLegendItem(axis: RadarAxis) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = axis.name,
            style = MaterialTheme.typography.labelSmall,
            color = GlassTextSecondary,
            textAlign = TextAlign.Center,
            fontSize = 10.sp
        )
        Text(
            text = "${axis.value.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = RadarStroke
        )
        if (axis.weight != 1f) {
            Text(
                text = "(${(axis.weight * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextSecondary,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun BidInsightRow(insight: BidStrengthInsight) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (insight.isStrength) StatusSuccess.copy(alpha = 0.1f)
                else StatusWarning.copy(alpha = 0.1f)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (insight.isStrength) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (insight.isStrength) StatusSuccess else StatusWarning,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.padding(4.dp))
        Text(
            text = insight.text,
            style = MaterialTheme.typography.bodySmall,
            color = GlassTextPrimary
        )
    }
}

/**
 * Compact radar preview for tender list
 */
@Composable
fun RadarMini(
    axes: List<RadarAxis>,
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = this.size.width / 2
            val centerY = this.size.height / 2
            val radius = minOf(centerX, centerY) * 0.9f
            val axisCount = axes.size
            val angleStep = (2 * PI / axisCount).toFloat()

            // Draw value polygon only
            val valuePath = Path()
            axes.forEachIndexed { index, axis ->
                val value = axis.value / 100f
                val angle = -PI.toFloat() / 2 + index * angleStep
                val x = centerX + radius * value * cos(angle)
                val y = centerY + radius * value * sin(angle)
                if (index == 0) {
                    valuePath.moveTo(x, y)
                } else {
                    valuePath.lineTo(x, y)
                }
            }
            valuePath.close()

            drawPath(
                path = valuePath,
                color = RadarFill,
                style = Fill
            )
            drawPath(
                path = valuePath,
                color = RadarStroke,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
