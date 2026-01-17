package com.tendertracker.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tendertracker.app.ui.theme.GlassPrimary
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary
import com.tendertracker.app.ui.theme.getComplianceColor
import com.tendertracker.app.ui.theme.glassCard
import kotlin.math.roundToInt

/**
 * Timeline marker data
 */
data class TimelineMarker(
    val id: String,
    val label: String,
    val daysFromNow: Int,
    val description: String = ""
)

/**
 * Compliance expiration timeline
 */
@Composable
fun ExpirationTimeline(
    markers: List<TimelineMarker>,
    modifier: Modifier = Modifier,
    maxDays: Int = 180
) {
    val sortedMarkers = markers.sortedBy { it.daysFromNow }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Timeline labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TimelineLabel("Today")
            TimelineLabel("+30d")
            TimelineLabel("+60d")
            TimelineLabel("+90d")
            TimelineLabel("+120d")
            TimelineLabel("+180d")
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Timeline track with markers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            // Track line
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
            ) {
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Markers
            sortedMarkers.forEach { marker ->
                val position = (marker.daysFromNow.toFloat() / maxDays).coerceIn(0f, 1f)
                val markerColor = getComplianceColor(marker.daysFromNow)

                TimelineMarkerView(
                    marker = marker,
                    color = markerColor,
                    modifier = Modifier.align(Alignment.CenterStart),
                    offsetFraction = position
                )
            }
        }
    }
}

@Composable
private fun TimelineLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = GlassTextSecondary,
        fontSize = 10.sp
    )
}

@Composable
private fun TimelineMarkerView(
    marker: TimelineMarker,
    color: Color,
    modifier: Modifier = Modifier,
    offsetFraction: Float
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(offsetFraction.coerceAtLeast(0.05f))
                .align(Alignment.CenterStart),
            horizontalAlignment = Alignment.End
        ) {
            // Marker dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            // Label below
            Text(
                text = marker.label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp
            )

            // Days
            Text(
                text = "(${marker.daysFromNow}d)",
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextSecondary,
                fontSize = 8.sp
            )
        }
    }
}

/**
 * Compact timeline row for inline display
 */
@Composable
fun CompactTimeline(
    markers: List<TimelineMarker>,
    modifier: Modifier = Modifier,
    maxDays: Int = 90
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            markers.sortedBy { it.daysFromNow }.forEach { marker ->
                val position = (marker.daysFromNow.toFloat() / maxDays).coerceIn(0f, 1f)
                val markerColor = getComplianceColor(marker.daysFromNow)

                Box(
                    modifier = Modifier
                        .fillMaxWidth(position.coerceAtLeast(0.02f))
                        .height(4.dp)
                        .align(Alignment.CenterStart)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(markerColor)
                            .align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

/**
 * Deadline countdown badge
 */
@Composable
fun DeadlineCountdown(
    daysRemaining: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val color = getComplianceColor(daysRemaining)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when {
                daysRemaining < 0 -> "Overdue"
                daysRemaining == 0 -> "Today"
                daysRemaining == 1 -> "1 day"
                else -> "$daysRemaining days"
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )

        if (showLabel && daysRemaining > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "left",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Milestone timeline for tender preparation
 */
@Composable
fun MilestoneTimeline(
    milestones: List<Milestone>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        milestones.forEachIndexed { index, milestone ->
            MilestoneRow(
                milestone = milestone,
                isLast = index == milestones.size - 1
            )
        }
    }
}

data class Milestone(
    val name: String,
    val date: String,
    val isComplete: Boolean = false,
    val isCurrent: Boolean = false
)

@Composable
private fun MilestoneRow(
    milestone: Milestone,
    isLast: Boolean
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        // Marker and line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            milestone.isComplete -> Color(0xFF16A34A)
                            milestone.isCurrent -> GlassPrimary
                            else -> Color.White.copy(alpha = 0.3f)
                        }
                    )
            )

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column {
            Text(
                text = milestone.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (milestone.isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (milestone.isComplete || milestone.isCurrent) GlassTextPrimary else GlassTextSecondary
            )
            Text(
                text = milestone.date,
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextSecondary
            )
        }
    }
}
