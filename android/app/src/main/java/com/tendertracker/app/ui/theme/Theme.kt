package com.tendertracker.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Glassmorphism Dark Color Scheme
 * Matches web app: frontend/src/theme/glassmorphism.ts
 */
private val GlassmorphismDarkColorScheme = darkColorScheme(
    // Primary (Purple)
    primary = GlassPrimary,                    // #8B5CF6
    onPrimary = Color.White,
    primaryContainer = GlassPrimaryDark,       // #7C3AED
    onPrimaryContainer = Color.White,

    // Secondary (Pink)
    secondary = GlassSecondary,                // #EC4899
    onSecondary = Color.White,
    secondaryContainer = GlassSecondaryDark,   // #DB2777
    onSecondaryContainer = Color.White,

    // Tertiary (Light Purple)
    tertiary = GlassPrimaryLight,              // #A78BFA
    onTertiary = Color.White,

    // Background (Dark Slate)
    background = GlassBackground,              // #0F172A
    onBackground = GlassTextPrimary,           // #F8FAFC

    // Surface (Slightly lighter)
    surface = GlassSurface,                    // #1E293B
    onSurface = GlassTextPrimary,              // #F8FAFC
    surfaceVariant = GlassSurfaceVariant,      // #334155
    onSurfaceVariant = GlassTextSecondary,     // #94A3B8

    // Error
    error = StatusError,                       // #EF4444
    onError = Color.White,

    // Outline (Glass border)
    outline = GlassBorder,                     // rgba(255,255,255,0.1)
    outlineVariant = Color.White.copy(alpha = 0.05f),

    // Inverse
    inverseSurface = GlassTextPrimary,
    inverseOnSurface = GlassBackground,
    inversePrimary = GlassPrimaryDark,

    // Scrim
    scrim = GlassOverlay
)

/**
 * Light color scheme (rarely used - app is primarily dark)
 * Still uses glassmorphism colors for consistency
 */
private val GlassmorphismLightColorScheme = lightColorScheme(
    primary = GlassPrimary,
    onPrimary = Color.White,
    primaryContainer = GlassPrimaryLight,
    onPrimaryContainer = GlassPrimaryDark,
    secondary = GlassSecondary,
    onSecondary = Color.White,
    secondaryContainer = GlassSecondaryLight,
    onSecondaryContainer = GlassSecondaryDark,
    background = Color(0xFFF8FAFC),
    onBackground = GlassBackground,
    surface = Color.White,
    onSurface = GlassBackground,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = GlassSurfaceVariant,
    error = StatusError,
    onError = Color.White
)

/**
 * Tender Tracker Theme
 * Always uses dark mode with glassmorphism for consistency with web app
 */
@Composable
fun TenderTrackerTheme(
    darkTheme: Boolean = true,  // Always dark to match web app
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        GlassmorphismDarkColorScheme
    } else {
        GlassmorphismLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar to dark background
            window.statusBarColor = GlassBackground.toArgb()
            // Set navigation bar to dark background
            window.navigationBarColor = GlassBackground.toArgb()
            // Use light icons on dark background
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
