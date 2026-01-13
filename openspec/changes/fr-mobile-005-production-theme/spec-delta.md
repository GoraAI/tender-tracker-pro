# Spec Delta: FR-MOBILE-005 Production Theme & API Integration

## MODIFIED

### android/app/src/main/java/com/tendertracker/app/ui/theme/Color.kt
```diff
- val TenderPrimary = Color(0xFF2196F3)
- val TenderPrimaryDark = Color(0xFF1976D2)
- val TenderSecondary = Color(0xFF4CAF50)
- val TenderAccent = Color(0xFFFF9800)
+ // Glassmorphism palette (matching web app)
+ val GlassPrimary = Color(0xFF8B5CF6)
+ val GlassPrimaryLight = Color(0xFFA78BFA)
+ val GlassPrimaryDark = Color(0xFF7C3AED)
+ val GlassSecondary = Color(0xFFEC4899)
+ val GlassSecondaryLight = Color(0xFFF472B6)
+ val GlassSecondaryDark = Color(0xFFDB2777)
+ val GlassBackground = Color(0xFF0F172A)
+ val GlassSurface = Color(0xFF1E293B)
+ val GlassCard = Color(0x991E293B) // 60% opacity
+ val GlassTextPrimary = Color(0xFFF8FAFC)
+ val GlassTextSecondary = Color(0xFF94A3B8)
+ val GlassBorder = Color(0x1AFFFFFF) // 10% white
```

### android/app/src/main/java/com/tendertracker/app/ui/theme/Theme.kt
```diff
- val Purple40 = Color(0xFF6650a4)
+ // Dark theme with glassmorphism
+ private val GlassDarkColorScheme = darkColorScheme(
+     primary = GlassPrimary,
+     onPrimary = GlassTextPrimary,
+     secondary = GlassSecondary,
+     onSecondary = GlassTextPrimary,
+     background = GlassBackground,
+     surface = GlassSurface,
+     onBackground = GlassTextPrimary,
+     onSurface = GlassTextPrimary,
+ )
```

## ADDED

### android/app/src/main/java/com/tendertracker/app/ui/theme/Glass.kt
```kotlin
// Glassmorphism effects for Compose
object GlassEffect {
    val cardBackground = Color(0x991E293B)
    val blurRadius = 12.dp
    val borderColor = Color(0x1AFFFFFF)
    val borderWidth = 1.dp
    val cornerRadius = 16.dp
    val shadowElevation = 8.dp
}

fun Modifier.glassCard(): Modifier = this
    .clip(RoundedCornerShape(GlassEffect.cornerRadius))
    .background(GlassEffect.cardBackground)
    .border(
        width = GlassEffect.borderWidth,
        color = GlassEffect.borderColor,
        shape = RoundedCornerShape(GlassEffect.cornerRadius)
    )
```

### android/app/src/main/java/com/tendertracker/app/data/remote/AuthApi.kt
```kotlin
interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse
}
```

### android/app/build.gradle.kts
```diff
+ buildTypes {
+     debug {
+         buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000\"")
+     }
+     release {
+         buildConfigField("String", "BASE_URL", "\"https://api.tendertracker.com\"")
+     }
+ }
```

## REMOVED

- Mock data providers (replaced with real API calls)
- Placeholder screens (replaced with production UI)
