package com.tendertracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tendertracker.app.ui.theme.getScoreCategory
import com.tendertracker.app.ui.theme.getScoreColor

/**
 * Score Badge Component
 * Matches web app: frontend/src/components/common/ScoreBadge.tsx
 *
 * Web CSS equivalent:
 * - backgroundColor: `${color}20` (20% opacity)
 * - color: (text) color (hex)
 * - border: `1px solid ${color}40` (40% opacity)
 * - borderRadius: 8px
 * - fontWeight: 600
 */
@Composable
fun ScoreBadge(
    score: Int,
    showCategory: Boolean = false,
    large: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scoreColor = getScoreColor(score)
    val backgroundColor = scoreColor.copy(alpha = 0.2f)  // 20% opacity
    val borderColor = scoreColor.copy(alpha = 0.4f)      // 40% opacity
    val category = getScoreCategory(score)

    val cornerRadius = 8.dp  // Web: borderRadius: 8px
    val shape = RoundedCornerShape(cornerRadius)

    if (showCategory) {
        // Badge with category label (chip style)
        Row(
            modifier = modifier
                .clip(shape)
                .background(backgroundColor)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = score.toString(),
                style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,  // fontWeight: 600
                color = scoreColor  // Text color matches score color
            )
            Text(
                text = category,
                style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor
            )
        }
    } else {
        // Simple score badge (circular style)
        val size = if (large) 64.dp else 48.dp

        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(backgroundColor)
                .border(1.dp, borderColor, shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                style = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor  // Text color matches score color (not white!)
            )
        }
    }
}

/**
 * Score chip with icon and category
 * Matches web MUI Chip component styling
 */
@Composable
fun ScoreChip(
    score: Int,
    modifier: Modifier = Modifier
) {
    val scoreColor = getScoreColor(score)
    val backgroundColor = scoreColor.copy(alpha = 0.2f)
    val borderColor = scoreColor.copy(alpha = 0.4f)
    val category = getScoreCategory(score)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor
            )
            Text(
                text = category,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = scoreColor
            )
        }
    }
}

/**
 * Detailed score display with breakdown
 */
@Composable
fun ScoreDisplay(
    score: Int,
    explanation: String? = null,
    modifier: Modifier = Modifier
) {
    val scoreColor = getScoreColor(score)
    val category = getScoreCategory(score)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreBadge(score = score, large = true)
            Column {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scoreColor
                )
                Text(
                    text = "Relevance Score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!explanation.isNullOrEmpty()) {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
