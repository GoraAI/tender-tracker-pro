package com.tendertracker.app.ui.components.charts

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tendertracker.app.ui.theme.GlassPrimary
import com.tendertracker.app.ui.theme.GlassSecondary
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary
import com.tendertracker.app.ui.theme.StatusError
import com.tendertracker.app.ui.theme.StatusWarning
import com.tendertracker.app.ui.theme.UrgencyCaution
import com.tendertracker.app.ui.theme.getUrgencyColor
import com.tendertracker.app.ui.theme.glassCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Pipeline tender status
 */
enum class PipelineStatus {
    CONSIDERING,
    PREPARING,
    REVIEWING,
    READY_TO_SUBMIT,
    SUBMITTED,
    AWARDED,
    LOST
}

/**
 * Pipeline tender item
 */
data class PipelineTender(
    val id: String,
    val title: String,
    val reference: String,
    val deadline: LocalDate,
    val status: PipelineStatus,
    val startDate: LocalDate = LocalDate.now(),
    val phases: List<PipelinePhase> = emptyList(),
    val hasConflict: Boolean = false,
    val conflictMessage: String = ""
)

/**
 * Phase within a tender preparation
 */
data class PipelinePhase(
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isComplete: Boolean = false
)

/**
 * Pipeline calendar (Gantt-style)
 */
@Composable
fun PipelineCalendar(
    tenders: List<PipelineTender>,
    modifier: Modifier = Modifier,
    startDate: LocalDate = LocalDate.now(),
    daysToShow: Int = 14,
    onTenderClick: (PipelineTender) -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val dayWidth = 48.dp
    val rowHeight = 56.dp
    val scrollState = rememberScrollState()

    // Detect deadline conflicts
    val conflicts = detectConflicts(tenders)

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
                text = "Pipeline Calendar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = GlassTextPrimary
            )
            Text(
                text = "+ Add",
                style = MaterialTheme.typography.labelMedium,
                color = GlassPrimary,
                modifier = Modifier.clickable(onClick = onAddClick)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Conflict warning
        if (conflicts.isNotEmpty()) {
            ConflictWarning(conflicts = conflicts)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Date header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.width(120.dp)) // Label column
            for (i in 0 until daysToShow) {
                val date = startDate.plusDays(i.toLong())
                DateHeader(
                    date = date,
                    isToday = date == LocalDate.now(),
                    modifier = Modifier.width(dayWidth)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tender rows
        LazyColumn(
            modifier = Modifier.height((tenders.size.coerceAtMost(5) * rowHeight.value).dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tenders) { tender ->
                TenderGanttRow(
                    tender = tender,
                    startDate = startDate,
                    daysToShow = daysToShow,
                    dayWidth = dayWidth,
                    scrollState = scrollState,
                    onClick = { onTenderClick(tender) }
                )
            }
        }

        // Empty state
        if (tenders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tenders in pipeline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTextSecondary
                )
            }
        }
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")
    val dateFormatter = DateTimeFormatter.ofPattern("d")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.format(dayFormatter),
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) GlassPrimary else GlassTextSecondary,
            fontSize = 10.sp
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isToday) GlassPrimary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.format(dateFormatter),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) Color.White else GlassTextPrimary
            )
        }
    }
}

@Composable
private fun TenderGanttRow(
    tender: PipelineTender,
    startDate: LocalDate,
    daysToShow: Int,
    dayWidth: androidx.compose.ui.unit.Dp,
    scrollState: androidx.compose.foundation.ScrollState,
    onClick: () -> Unit
) {
    val daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), tender.deadline).toInt()
    val urgencyColor = getUrgencyColor(daysUntilDeadline)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tender label
        Column(
            modifier = Modifier
                .width(120.dp)
                .padding(end = 8.dp)
        ) {
            Text(
                text = tender.title,
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tender.reference,
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextSecondary,
                fontSize = 10.sp
            )
        }

        // Timeline
        Box(
            modifier = Modifier
                .width(dayWidth * daysToShow)
                .height(32.dp)
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            )

            // Tender bar
            val barStart = ChronoUnit.DAYS.between(startDate, tender.startDate).toInt().coerceAtLeast(0)
            val barEnd = ChronoUnit.DAYS.between(startDate, tender.deadline).toInt().coerceAtMost(daysToShow)

            if (barEnd > barStart) {
                TenderBar(
                    tender = tender,
                    startOffset = barStart * dayWidth.value,
                    width = (barEnd - barStart) * dayWidth.value,
                    urgencyColor = urgencyColor
                )
            }

            // Deadline marker
            val deadlineOffset = ChronoUnit.DAYS.between(startDate, tender.deadline).toInt()
            if (deadlineOffset in 0 until daysToShow) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset((deadlineOffset * dayWidth.value.dp.toPx()).roundToInt(), 0) }
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(urgencyColor)
                )
            }
        }
    }
}

@Composable
private fun TenderBar(
    tender: PipelineTender,
    startOffset: Float,
    width: Float,
    urgencyColor: Color
) {
    val statusColor = when (tender.status) {
        PipelineStatus.CONSIDERING -> Color.White.copy(alpha = 0.3f)
        PipelineStatus.PREPARING -> GlassPrimary.copy(alpha = 0.6f)
        PipelineStatus.REVIEWING -> UrgencyCaution.copy(alpha = 0.6f)
        PipelineStatus.READY_TO_SUBMIT -> GlassSecondary.copy(alpha = 0.6f)
        PipelineStatus.SUBMITTED -> GlassPrimary
        PipelineStatus.AWARDED -> Color(0xFF16A34A)
        PipelineStatus.LOST -> Color(0xFFEF4444).copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .offset { IntOffset((startOffset.dp.toPx()).roundToInt(), 0) }
            .width(width.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(statusColor),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tender.hasConflict) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Conflict",
                    tint = StatusWarning,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = tender.status.name.replace("_", " "),
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextPrimary,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ConflictWarning(conflicts: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StatusWarning.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = StatusWarning,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = conflicts.first(),
            style = MaterialTheme.typography.bodySmall,
            color = GlassTextPrimary
        )
    }
}

private fun detectConflicts(tenders: List<PipelineTender>): List<String> {
    val conflicts = mutableListOf<String>()
    val deadlineGroups = tenders.groupBy { it.deadline }

    deadlineGroups.forEach { (date, tendersOnDate) ->
        if (tendersOnDate.size > 1) {
            conflicts.add("${tendersOnDate.size} deadlines on ${date.format(DateTimeFormatter.ofPattern("MMM d"))}")
        }
    }

    return conflicts
}

/**
 * Compact pipeline summary for dashboard
 */
@Composable
fun PipelineSummary(
    activeCount: Int,
    dueThisWeek: Int,
    hasConflicts: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Progress indicator
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GlassPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = activeCount.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GlassPrimary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Active Pipeline",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = GlassTextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$dueThisWeek due this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dueThisWeek > 0) StatusWarning else GlassTextSecondary
                )
                if (hasConflicts) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Conflicts",
                        tint = StatusWarning,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Text(
            text = "View",
            style = MaterialTheme.typography.labelMedium,
            color = GlassTextSecondary
        )
    }
}
