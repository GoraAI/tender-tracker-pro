package com.tendertracker.app.ui.components

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary
import com.tendertracker.app.ui.theme.StatusError
import com.tendertracker.app.ui.theme.StatusInfo
import com.tendertracker.app.ui.theme.StatusSuccess
import com.tendertracker.app.ui.theme.StatusWarning
import com.tendertracker.app.ui.theme.UrgencyCritical
import com.tendertracker.app.ui.theme.UrgencyWarning
import com.tendertracker.app.ui.theme.glassCard

/**
 * Attention priority levels
 */
enum class AttentionPriority {
    CRITICAL,   // Red - immediate action required
    WARNING,    // Orange - action needed soon
    INFO,       // Blue - informational
    SUCCESS     // Green - positive update
}

/**
 * Attention item data
 */
data class AttentionItem(
    val id: String,
    val title: String,
    val description: String,
    val priority: AttentionPriority,
    val actionLabel: String? = null,
    val secondaryActionLabel: String? = null
)

/**
 * Single attention card for urgent items
 */
@Composable
fun AttentionCard(
    item: AttentionItem,
    modifier: Modifier = Modifier,
    onActionClick: () -> Unit = {},
    onSecondaryActionClick: () -> Unit = {},
    onDismiss: (() -> Unit)? = null
) {
    val (backgroundColor, iconColor, icon) = when (item.priority) {
        AttentionPriority.CRITICAL -> Triple(
            UrgencyCritical.copy(alpha = 0.15f),
            UrgencyCritical,
            Icons.Default.Error
        )
        AttentionPriority.WARNING -> Triple(
            UrgencyWarning.copy(alpha = 0.15f),
            UrgencyWarning,
            Icons.Default.Warning
        )
        AttentionPriority.INFO -> Triple(
            StatusInfo.copy(alpha = 0.15f),
            StatusInfo,
            Icons.Default.Info
        )
        AttentionPriority.SUCCESS -> Triple(
            StatusSuccess.copy(alpha = 0.15f),
            StatusSuccess,
            Icons.Default.CheckCircle
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSecondary
                    )
                }
            }

            // Actions
            if (item.actionLabel != null || item.secondaryActionLabel != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    item.secondaryActionLabel?.let { label ->
                        TextButton(
                            onClick = onSecondaryActionClick,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = GlassTextSecondary
                            )
                        ) {
                            Text(text = label)
                        }
                    }

                    item.actionLabel?.let { label ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onActionClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = iconColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = label, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section header for attention items
 */
@Composable
fun AttentionSection(
    items: List<AttentionItem>,
    modifier: Modifier = Modifier,
    onItemAction: (AttentionItem) -> Unit = {},
    onViewAll: (() -> Unit)? = null
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Requires Attention",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = GlassTextPrimary
            )

            if (items.size > 3 && onViewAll != null) {
                TextButton(onClick = onViewAll) {
                    Text(
                        text = "View All (${items.size})",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Items (show max 3)
        items.take(3).forEach { item ->
            AttentionCard(
                item = item,
                onActionClick = { onItemAction(item) }
            )
        }
    }
}

/**
 * Compact attention badge for navigation
 */
@Composable
fun AttentionBadge(
    count: Int,
    priority: AttentionPriority,
    modifier: Modifier = Modifier
) {
    if (count == 0) return

    val color = when (priority) {
        AttentionPriority.CRITICAL -> UrgencyCritical
        AttentionPriority.WARNING -> UrgencyWarning
        AttentionPriority.INFO -> StatusInfo
        AttentionPriority.SUCCESS -> StatusSuccess
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * New opportunities card
 */
@Composable
fun NewOpportunitiesCard(
    title: String,
    value: String,
    score: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .glassCard(cornerRadius = 12.dp)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextPrimary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GlassTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            ScoreBadge(score = score)
        }
    }
}
