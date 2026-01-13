# Proposal: FR-MOBILE-005 Production Theme & API Integration

## Summary
Update Android app to **exactly match** web app's glassmorphism design and connect to production backend API.

## Motivation
Comprehensive comparison reveals **critical visual inconsistencies** between platforms:

### Color Palette Mismatch (CRITICAL)
| Element | Web App | Android (WRONG) |
|---------|---------|-----------------|
| **Primary** | `#8B5CF6` (Purple) | `#2196F3` (Blue) |
| **Secondary** | `#EC4899` (Pink) | `#4CAF50` (Green) |
| **Background** | `#0F172A` (Dark Slate) | Material default |
| **Score Excellent** | `#22C55E` | `#4CAF50` |
| **Score Good** | `#3B82F6` (Blue) | `#8BC34A` (Green) |
| **Score Moderate** | `#F59E0B` | `#FFEB3B` |

### Glassmorphism Missing (CRITICAL)
| Effect | Web App | Android |
|--------|---------|---------|
| Card Background | `rgba(30, 41, 59, 0.6)` | Solid color |
| Backdrop Blur | `blur(12px)` | None |
| Border | `1px solid rgba(255,255,255,0.1)` | None |
| Shadow | `0 8px 32px rgba(0,0,0,0.3)` | Material default |
| Border Radius | `16px` | `12dp` |
| Card Padding | `24px` | `16dp` |

### Gradients Missing
| Element | Web App | Android |
|---------|---------|---------|
| Logo/Branding | `linear-gradient(135deg, #8B5CF6, #EC4899)` | None |
| Buttons | Purple→Pink gradient | Solid color |
| Active Menu | Gradient with 20% opacity | None |
| Avatar | Purple→Pink gradient | Solid color |

## Scope

### Included
1. **Complete color palette replacement** - All colors to match web
2. **Glassmorphism implementation** - Blur, transparency, borders
3. **Typography update** - Inter font, correct weights
4. **Component restyling** - Cards, badges, buttons, inputs
5. **Layout adjustments** - Padding (24dp), spacing, radius (16dp)
6. **Gradient system** - Purple→Pink gradients throughout
7. **Shadow system** - Custom shadows matching web
8. **Production API** - Real endpoints, JWT auth, offline support

### NOT Included
- Backend API changes
- New features not in web app
- iOS version

## Detailed Changes Required

### 1. Color.kt - Complete Replacement
```kotlin
// CURRENT (WRONG)
val TenderPrimary = Color(0xFF2196F3)      // Blue
val TenderSecondary = Color(0xFF4CAF50)    // Green

// TARGET (CORRECT - matching web)
val GlassPrimary = Color(0xFF8B5CF6)       // Purple
val GlassPrimaryLight = Color(0xFFA78BFA)
val GlassPrimaryDark = Color(0xFF7C3AED)
val GlassSecondary = Color(0xFFEC4899)     // Pink
val GlassSecondaryLight = Color(0xFFF472B6)
val GlassSecondaryDark = Color(0xFFDB2777)
val GlassBackground = Color(0xFF0F172A)    // Dark slate
val GlassSurface = Color(0xFF1E293B)
val GlassTextPrimary = Color(0xFFF8FAFC)
val GlassTextSecondary = Color(0xFF94A3B8)

// Score colors (matching web exactly)
val ScoreExcellent = Color(0xFF22C55E)     // was #4CAF50
val ScoreGood = Color(0xFF3B82F6)          // was #8BC34A (MAJOR CHANGE)
val ScoreModerate = Color(0xFFF59E0B)      // was #FFEB3B
val ScoreWeak = Color(0xFFF97316)          // was #FF9800
val ScorePoor = Color(0xFFEF4444)          // was #F44336
```

### 2. GlassEffect.kt - New File
```kotlin
object GlassEffect {
    val cardBackground = Color(0xFF1E293B).copy(alpha = 0.6f)
    val blurRadius = 12.dp
    val borderColor = Color.White.copy(alpha = 0.1f)
    val borderWidth = 1.dp
    val cornerRadius = 16.dp
    val cardPadding = 24.dp
    val shadowElevation = 8.dp

    val primaryGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}
```

### 3. Typography - Font Update
```kotlin
// CURRENT
fontFamily = FontFamily.Default

// TARGET
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)
```

### 4. ScoreBadge - Style Fix
```kotlin
// CURRENT (WRONG)
background = scoreColor (solid)
text = White

// TARGET (CORRECT - matching web)
background = scoreColor.copy(alpha = 0.2f)  // 20% opacity
border = scoreColor.copy(alpha = 0.4f)      // 40% opacity
text = scoreColor                            // Same color as badge
```

### 5. Spacing Constants
```kotlin
// CURRENT
val CardPadding = 16.dp
val ItemSpacing = 12.dp

// TARGET (matching web)
val CardPadding = 24.dp      // Web: padding: 24px
val ItemSpacing = 16.dp      // Web: gap: 2 (16px)
val ScreenPadding = 24.dp    // Web: p: 3 (24px)
val CornerRadius = 16.dp     // Web: borderRadius: 16px
```

## Acceptance Criteria

### Visual Parity (Must Pass)
- [ ] Side-by-side screenshot comparison shows identical appearance
- [ ] Primary purple `#8B5CF6` used throughout
- [ ] Secondary pink `#EC4899` used throughout
- [ ] Cards have visible glassmorphism blur effect
- [ ] Cards have subtle white border (10% opacity)
- [ ] Card corners are 16dp rounded
- [ ] Card padding is 24dp
- [ ] Score badges use transparent backgrounds with colored text
- [ ] Buttons have purple glow shadow
- [ ] Gradients used for avatars, active states, branding

### Production Functionality
- [ ] Login connects to `POST /api/v1/auth/login`
- [ ] JWT token stored in encrypted DataStore
- [ ] Tenders fetched from `GET /api/v1/tenders`
- [ ] Scores fetched from `GET /api/v1/tenders/{id}/score`
- [ ] Pull-to-refresh syncs with backend
- [ ] Offline mode works with Room cache
- [ ] Proper error handling with retry options

## References
- Web theme: `frontend/src/theme/glassmorphism.ts`
- Web components: `frontend/src/components/`
- API routes: `src/api/routers/`
