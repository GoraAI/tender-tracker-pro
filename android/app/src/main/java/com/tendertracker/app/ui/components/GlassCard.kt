package com.tendertracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tendertracker.app.ui.theme.GlassCardBackground
import com.tendertracker.app.ui.theme.GlassBorder
import com.tendertracker.app.ui.theme.GlassEffect

/**
 * Glassmorphism Card Component
 * Matches web app: frontend/src/components/common/GlassCard.tsx
 *
 * Web CSS equivalent:
 * - background: rgba(30, 41, 59, 0.6)
 * - backdropFilter: blur(12px)
 * - border: 1px solid rgba(255, 255, 255, 0.1)
 * - borderRadius: 16px
 * - boxShadow: 0 8px 32px rgba(0, 0, 0, 0.3)
 * - padding: 24px
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassEffect.cornerRadius,  // 16dp
    elevation: Dp = GlassEffect.shadowElevation,  // 8dp
    contentPadding: Dp = GlassEffect.cardPadding,  // 24dp
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = GlassCardBackground  // rgba(30, 41, 59, 0.6)
        ),
        border = BorderStroke(
            width = GlassEffect.borderWidth,  // 1dp
            color = GlassBorder  // rgba(255, 255, 255, 0.1)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation,
            pressedElevation = elevation + 4.dp,
            hoveredElevation = elevation + 4.dp
        )
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Compact glass card with less padding
 * For list items and smaller containers
 */
@Composable
fun GlassCardCompact(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassEffect.cornerRadius,
    elevation: Dp = 4.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        elevation = elevation,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * Clickable glass card with ripple effect
 */
@Composable
fun GlassCardClickable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassEffect.cornerRadius,
    elevation: Dp = GlassEffect.shadowElevation,
    contentPadding: Dp = GlassEffect.cardPadding,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = GlassCardBackground
        ),
        border = BorderStroke(
            width = GlassEffect.borderWidth,
            color = GlassBorder
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation,
            pressedElevation = elevation + 4.dp,
            hoveredElevation = elevation + 4.dp
        )
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
