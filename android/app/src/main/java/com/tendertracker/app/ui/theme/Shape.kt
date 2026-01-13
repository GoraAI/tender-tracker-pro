package com.tendertracker.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shapes matching web app borderRadius values
 * Web uses: borderRadius: 12px for general, 16px for cards
 */
val Shapes = Shapes(
    // Extra small (8dp) - chips, badges
    extraSmall = RoundedCornerShape(8.dp),

    // Small (12dp) - buttons, inputs
    small = RoundedCornerShape(12.dp),

    // Medium (16dp) - cards, dialogs (matching web card borderRadius: 16px)
    medium = RoundedCornerShape(16.dp),

    // Large (16dp) - same as medium for consistency
    large = RoundedCornerShape(16.dp),

    // Extra large (24dp) - bottom sheets, large dialogs
    extraLarge = RoundedCornerShape(24.dp)
)
