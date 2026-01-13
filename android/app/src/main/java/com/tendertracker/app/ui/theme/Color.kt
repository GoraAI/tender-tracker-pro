package com.tendertracker.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Glassmorphism Color Palette
 * Matches web app: frontend/src/theme/glassmorphism.ts
 */

// =============================================================================
// PRIMARY COLORS (Purple - matching web #8B5CF6)
// =============================================================================
val GlassPrimary = Color(0xFF8B5CF6)
val GlassPrimaryLight = Color(0xFFA78BFA)
val GlassPrimaryDark = Color(0xFF7C3AED)

// =============================================================================
// SECONDARY COLORS (Pink - matching web #EC4899)
// =============================================================================
val GlassSecondary = Color(0xFFEC4899)
val GlassSecondaryLight = Color(0xFFF472B6)
val GlassSecondaryDark = Color(0xFFDB2777)

// =============================================================================
// BACKGROUND COLORS (Dark Slate - matching web #0F172A)
// =============================================================================
val GlassBackground = Color(0xFF0F172A)
val GlassSurface = Color(0xFF1E293B)
val GlassSurfaceVariant = Color(0xFF334155)

// =============================================================================
// TEXT COLORS (matching web)
// =============================================================================
val GlassTextPrimary = Color(0xFFF8FAFC)
val GlassTextSecondary = Color(0xFF94A3B8)

// =============================================================================
// GLASS EFFECT COLORS (semi-transparent for glassmorphism)
// =============================================================================
val GlassCardBackground = Color(0xFF1E293B).copy(alpha = 0.6f)  // 60% opacity
val GlassCardBackgroundHover = Color(0xFF1E293B).copy(alpha = 0.7f)  // 70% opacity
val GlassBorder = Color.White.copy(alpha = 0.1f)  // 10% white border
val GlassOverlay = Color(0xFF0F172A).copy(alpha = 0.9f)  // 90% overlay

// =============================================================================
// SCORE COLORS (matching web exactly)
// =============================================================================
val ScoreExcellent = Color(0xFF22C55E)  // Green - 90+
val ScoreGood = Color(0xFF3B82F6)       // Blue - 70-89 (NOT green!)
val ScoreModerate = Color(0xFFF59E0B)   // Amber - 50-69
val ScoreWeak = Color(0xFFF97316)       // Orange - 30-49
val ScorePoor = Color(0xFFEF4444)       // Red - 0-29

// =============================================================================
// STATUS COLORS (matching web)
// =============================================================================
val StatusSuccess = Color(0xFF22C55E)
val StatusWarning = Color(0xFFF59E0B)
val StatusError = Color(0xFFEF4444)
val StatusInfo = Color(0xFF3B82F6)

val StatusOnline = Color(0xFF22C55E)
val StatusOffline = Color(0xFF64748B)
val StatusSyncing = Color(0xFF8B5CF6)  // Purple for syncing

// =============================================================================
// HELPER FUNCTIONS (matching web getScoreColor/getScoreCategory)
// =============================================================================
fun getScoreColor(score: Int): Color = when {
    score >= 90 -> ScoreExcellent
    score >= 70 -> ScoreGood
    score >= 50 -> ScoreModerate
    score >= 30 -> ScoreWeak
    else -> ScorePoor
}

fun getScoreCategory(score: Int): String = when {
    score >= 90 -> "Excellent"
    score >= 70 -> "Good"
    score >= 50 -> "Moderate"
    score >= 30 -> "Weak"
    else -> "Poor"
}

// =============================================================================
// LEGACY ALIASES (for backward compatibility during migration)
// =============================================================================
@Deprecated("Use GlassPrimary", ReplaceWith("GlassPrimary"))
val TenderPrimary = GlassPrimary

@Deprecated("Use GlassPrimaryDark", ReplaceWith("GlassPrimaryDark"))
val TenderPrimaryDark = GlassPrimaryDark

@Deprecated("Use GlassSecondary", ReplaceWith("GlassSecondary"))
val TenderSecondary = GlassSecondary
