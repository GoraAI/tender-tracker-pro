package com.tendertracker.app.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism Effect System
 * Matches web app: frontend/src/theme/glassmorphism.ts glassEffect
 */
object GlassEffect {
    // Card styling (matching web)
    val cardBackground = Color(0xFF1E293B).copy(alpha = 0.6f)  // rgba(30, 41, 59, 0.6)
    val cardBackgroundHover = Color(0xFF1E293B).copy(alpha = 0.7f)  // rgba(30, 41, 59, 0.7)
    val blurRadius = 12.dp  // backdrop-filter: blur(12px)
    val borderColor = Color.White.copy(alpha = 0.1f)  // rgba(255, 255, 255, 0.1)
    val borderWidth = 1.dp
    val cornerRadius = 16.dp  // borderRadius: 16px
    val cardPadding = 24.dp  // padding: 24px
    val shadowElevation = 8.dp  // box-shadow: 0 8px 32px

    // Screen background gradient (matching web body background)
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F172A),  // #0F172A
            Color(0xFF1E293B)   // #1E293B
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Primary gradient (purple to pink - matching web)
    val primaryGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF8B5CF6),  // #8B5CF6 purple
            Color(0xFFEC4899)   // #EC4899 pink
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Active menu gradient (matching web sidebar active state)
    val activeGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF8B5CF6).copy(alpha = 0.2f),  // rgba(139, 92, 246, 0.2)
            Color(0xFFEC4899).copy(alpha = 0.1f)   // rgba(236, 72, 153, 0.1)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Button shadow color (purple glow)
    val buttonShadowColor = Color(0xFF8B5CF6).copy(alpha = 0.4f)  // rgba(139, 92, 246, 0.4)
    val buttonShadowColorHover = Color(0xFF8B5CF6).copy(alpha = 0.6f)  // rgba(139, 92, 246, 0.6)

    // Spacing constants (matching web)
    val itemSpacing = 16.dp  // gap: 2 (16px)
    val sectionSpacing = 24.dp  // gap: 3 (24px)
    val screenPadding = 24.dp  // p: 3 (24px)
}

/**
 * Apply glass card effect to a composable
 * Replicates: glassEffect from web
 */
fun Modifier.glassCard(
    cornerRadius: Dp = GlassEffect.cornerRadius,
    borderWidth: Dp = GlassEffect.borderWidth,
    elevation: Dp = GlassEffect.shadowElevation
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = Color.Black.copy(alpha = 0.3f),
        spotColor = Color.Black.copy(alpha = 0.3f)
    )
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        color = GlassEffect.cardBackground,
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = borderWidth,
        color = GlassEffect.borderColor,
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Apply glass background to a screen
 */
fun Modifier.glassBackground(): Modifier = this
    .background(brush = GlassEffect.backgroundGradient)

/**
 * Apply gradient background for buttons
 */
fun Modifier.gradientBackground(
    gradient: Brush = GlassEffect.primaryGradient,
    cornerRadius: Dp = 12.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(brush = gradient)

/**
 * Apply active/selected state gradient
 */
fun Modifier.activeGradient(
    cornerRadius: Dp = 12.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(brush = GlassEffect.activeGradient)
    .border(
        width = 1.dp,
        color = Color(0xFF8B5CF6).copy(alpha = 0.3f),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Apply purple glow shadow for buttons
 */
fun Modifier.purpleGlow(
    elevation: Dp = 4.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = RoundedCornerShape(12.dp),
        ambientColor = GlassEffect.buttonShadowColor,
        spotColor = GlassEffect.buttonShadowColor
    )

/**
 * Get score color with transparency for badge background
 * Matches web: backgroundColor: `${color}20` (20% opacity)
 */
fun getScoreBackgroundColor(score: Int): Color {
    return getScoreColor(score).copy(alpha = 0.2f)
}

/**
 * Get score border color with transparency
 * Matches web: border: `1px solid ${color}40` (40% opacity)
 */
fun getScoreBorderColor(score: Int): Color {
    return getScoreColor(score).copy(alpha = 0.4f)
}
