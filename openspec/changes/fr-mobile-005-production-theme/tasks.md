# FR-MOBILE-005: Production Theme & API Integration - Tasks

**Status**: PENDING APPROVAL
**Total Tasks**: 0/52

---

## Phase 1: Color Palette (10 tasks)

- [ ] Replace `TenderPrimary` from `#2196F3` to `#8B5CF6`
- [ ] Replace `TenderSecondary` from `#4CAF50` to `#EC4899`
- [ ] Add `GlassPrimaryLight` = `#A78BFA`
- [ ] Add `GlassPrimaryDark` = `#7C3AED`
- [ ] Add `GlassSecondaryLight` = `#F472B6`
- [ ] Add `GlassSecondaryDark` = `#DB2777`
- [ ] Add `GlassBackground` = `#0F172A`
- [ ] Add `GlassSurface` = `#1E293B`
- [ ] Add `GlassTextPrimary` = `#F8FAFC`
- [ ] Add `GlassTextSecondary` = `#94A3B8`

## Phase 2: Score Colors (5 tasks)

- [ ] Change `ScoreExcellent` from `#4CAF50` to `#22C55E`
- [ ] Change `ScoreGood` from `#8BC34A` to `#3B82F6` (green→blue)
- [ ] Change `ScoreModerate` from `#FFEB3B` to `#F59E0B`
- [ ] Change `ScoreWeak` from `#FF9800` to `#F97316`
- [ ] Change `ScorePoor` from `#F44336` to `#EF4444`

## Phase 3: Glassmorphism System (8 tasks)

- [ ] Create `GlassEffect.kt` with constants
- [ ] Define `cardBackground` = `Color(0xFF1E293B).copy(alpha = 0.6f)`
- [ ] Define `blurRadius` = `12.dp`
- [ ] Define `borderColor` = `Color.White.copy(alpha = 0.1f)`
- [ ] Define `cornerRadius` = `16.dp`
- [ ] Define `cardPadding` = `24.dp`
- [ ] Create `Modifier.glassCard()` extension
- [ ] Create `Modifier.glassBackground()` for screens

## Phase 4: Gradient System (5 tasks)

- [ ] Create `primaryGradient` = Purple→Pink at 135°
- [ ] Create `GradientButton` composable with purple glow
- [ ] Create `GradientAvatar` composable
- [ ] Add gradient to active navigation item
- [ ] Add gradient background to screens

## Phase 5: Typography (5 tasks)

- [ ] Download Inter font files (Regular, Medium, SemiBold, Bold)
- [ ] Add fonts to `res/font/` directory
- [ ] Create `InterFontFamily` in Type.kt
- [ ] Update `Typography` to use Inter
- [ ] Ensure h5/h6 use `FontWeight.SemiBold` (600)

## Phase 6: Component Updates (10 tasks)

- [ ] Update `GlassCard` with blur effect and border
- [ ] Update `ScoreBadge` with 20% opacity background
- [ ] Update `ScoreBadge` text color to match score color
- [ ] Update `TenderCard` with glassmorphism styling
- [ ] Update `BottomNavBar` with glass effect
- [ ] Update `TopAppBar` with glass effect and no elevation
- [ ] Update all `TextField` inputs with glass styling
- [ ] Update all `Button` components with purple glow
- [ ] Update `ChatBubble` with glass effect
- [ ] Update all dialogs/modals with glass effect

## Phase 7: Spacing & Layout (5 tasks)

- [ ] Change card padding from `16.dp` to `24.dp`
- [ ] Change item spacing from `12.dp` to `16.dp`
- [ ] Change screen padding from `16.dp` to `24.dp`
- [ ] Change corner radius from `12.dp` to `16.dp`
- [ ] Update icon containers to `56.dp` with `28.dp` icons

## Phase 8: API Configuration (6 tasks)

- [ ] Add `BASE_URL` to BuildConfig (debug: `http://10.0.2.2:8000`)
- [ ] Create `AuthApi` interface with login/register endpoints
- [ ] Update `TenderApi` with real endpoint paths
- [ ] Configure Retrofit with `BASE_URL`
- [ ] Add JWT `AuthInterceptor` with token refresh
- [ ] Add `RefreshTokenAuthenticator` for automatic retry

## Phase 9: Authentication Flow (6 tasks)

- [ ] Implement `LoginScreen` with real API call
- [ ] Implement `RegisterScreen` with real API call
- [ ] Store JWT in encrypted DataStore
- [ ] Create `AuthViewModel` with state management
- [ ] Add auto-logout on 401 responses
- [ ] Add token refresh before expiry

## Phase 10: Production Data (8 tasks)

- [ ] Fetch tenders from `GET /api/v1/tenders`
- [ ] Add pagination to tender list
- [ ] Implement pull-to-refresh sync
- [ ] Cache tenders in Room for offline
- [ ] Fetch tender detail from `GET /api/v1/tenders/{id}`
- [ ] Fetch scores from `GET /api/v1/tenders/{id}/score`
- [ ] Fetch dashboard stats from `GET /api/v1/dashboard/stats`
- [ ] Add error states with retry buttons

## Phase 11: Visual Verification (4 tasks)

- [ ] Screenshot comparison: Login screen
- [ ] Screenshot comparison: Tender list
- [ ] Screenshot comparison: Tender detail
- [ ] Screenshot comparison: Settings

---

**Total**: 0/52 tasks complete

## Visual Reference

### Color Mapping
```
Web                    Android (Target)
#8B5CF6 (Purple)    → GlassPrimary
#EC4899 (Pink)      → GlassSecondary
#0F172A (Background)→ GlassBackground
#1E293B (Surface)   → GlassSurface
#F8FAFC (Text)      → GlassTextPrimary
#94A3B8 (Text2)     → GlassTextSecondary
```

### Score Color Mapping
```
Web                    Android (Target)
#22C55E (Excellent) → ScoreExcellent
#3B82F6 (Good)      → ScoreGood (BLUE not green!)
#F59E0B (Moderate)  → ScoreModerate
#F97316 (Weak)      → ScoreWeak
#EF4444 (Poor)      → ScorePoor
```

### Spacing Mapping
```
Web                    Android (Target)
padding: 24px       → 24.dp (CardPadding)
gap: 16px           → 16.dp (ItemSpacing)
borderRadius: 16px  → 16.dp (CornerRadius)
```
