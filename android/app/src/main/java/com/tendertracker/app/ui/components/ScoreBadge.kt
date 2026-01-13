package com.tendertracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tendertracker.app.ui.theme.ScoreExcellent
import com.tendertracker.app.ui.theme.ScoreGood
import com.tendertracker.app.ui.theme.ScoreLow
import com.tendertracker.app.ui.theme.ScoreModerate
import com.tendertracker.app.ui.theme.ScorePoor

@Composable
fun ScoreBadge(
    score: Int,
    large: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = getScoreColor(score)
    val size = if (large) 64.dp else 48.dp
    val textStyle = if (large) {
        MaterialTheme.typography.headlineMedium
    } else {
        MaterialTheme.typography.titleMedium
    }

    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = score.toString(),
            style = textStyle,
            color = Color.White
        )
    }
}

@Composable
fun ScoreLabel(score: Int): String {
    return when {
        score >= 80 -> "Excellent"
        score >= 60 -> "Good"
        score >= 40 -> "Moderate"
        score >= 20 -> "Low"
        else -> "Poor"
    }
}

private fun getScoreColor(score: Int): Color {
    return when {
        score >= 80 -> ScoreExcellent
        score >= 60 -> ScoreGood
        score >= 40 -> ScoreModerate
        score >= 20 -> ScoreLow
        else -> ScorePoor
    }
}
