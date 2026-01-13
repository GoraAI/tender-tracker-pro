# FR-MOBILE-001: Android App Foundation

**Status**: PROPOSED
**Priority**: P0
**Created**: 2026-01-13
**Dependencies**: None

---

## Summary

Create the foundational Android application with Kotlin, Jetpack Compose, and Clean Architecture that will serve as the base for the mobile Tender Tracker Pro app with on-device AI capabilities.

---

## Motivation

Users need mobile access to tender tracking with:
- Offline-first capability for areas with poor connectivity
- On-device AI (Gemma 3n) for privacy and speed
- Push notifications for new high-relevance tenders
- Conversational interface for natural language queries

An Android-first approach targets the majority mobile market share in South Africa.

---

## Scope

### In Scope
- Android project setup with Kotlin + Jetpack Compose
- Clean Architecture (Presentation, Domain, Data layers)
- MVI pattern for state management
- Room database for local storage
- Hilt for dependency injection
- Navigation component setup
- Base UI components with Material Design 3
- Dark/Light theme support
- Gradle configuration for release builds
- ProGuard/R8 configuration

### Out of Scope
- Gemma integration (FR-MOBILE-002)
- Chat interface (FR-MOBILE-003)
- Backend sync (FR-MOBILE-004)
- iOS version (future)
- Tablet-optimized layouts (future)

---

## Acceptance Criteria

- [ ] Android project compiles and runs on emulator
- [ ] Clean Architecture layers properly separated
- [ ] MVI pattern implemented for ViewModels
- [ ] Room database initialized with schema
- [ ] Hilt DI configured for all layers
- [ ] Navigation graph with placeholder screens
- [ ] Material Design 3 theme configured
- [ ] Dark/Light mode toggle works
- [ ] Release build generates signed APK
- [ ] ProGuard rules configured for dependencies
- [ ] Unit tests for Domain layer (models, use cases)
- [ ] Instrumented tests for Room database

---

## Technical Approach

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │
│  │   Screens    │ │  ViewModels  │ │   UI State   │        │
│  │  (Compose)   │ │    (MVI)     │ │  (data class)│        │
│  └──────────────┘ └──────────────┘ └──────────────┘        │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                            │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │
│  │   Entities   │ │   Use Cases  │ │ Repositories │        │
│  │  (Models)    │ │  (Business)  │ │ (Interfaces) │        │
│  └──────────────┘ └──────────────┘ └──────────────┘        │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                       DATA LAYER                             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │
│  │  Repository  │ │   Room DAO   │ │   Retrofit   │        │
│  │    Impl      │ │  (Local DB)  │ │  (Remote API)│        │
│  └──────────────┘ └──────────────┘ └──────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

### Module Structure

```
android/
├── app/                          # Application module
│   ├── src/main/
│   │   ├── java/com/tendertracker/
│   │   │   ├── TenderTrackerApp.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── di/               # Hilt modules
│   │   │   ├── ui/               # Compose screens
│   │   │   │   ├── navigation/
│   │   │   │   ├── theme/
│   │   │   │   ├── components/
│   │   │   │   └── screens/
│   │   │   └── viewmodel/        # MVI ViewModels
│   │   └── res/
│   └── build.gradle.kts
│
├── domain/                       # Domain module (pure Kotlin)
│   ├── src/main/java/com/tendertracker/domain/
│   │   ├── model/               # Entities
│   │   ├── repository/          # Repository interfaces
│   │   └── usecase/             # Business logic
│   └── build.gradle.kts
│
├── data/                         # Data module
│   ├── src/main/java/com/tendertracker/data/
│   │   ├── local/               # Room database
│   │   │   ├── TenderDatabase.kt
│   │   │   ├── dao/
│   │   │   └── entity/
│   │   ├── remote/              # Retrofit API
│   │   │   ├── api/
│   │   │   └── dto/
│   │   └── repository/          # Repository implementations
│   └── build.gradle.kts
│
├── build.gradle.kts              # Root build file
├── settings.gradle.kts
└── gradle.properties
```

### MVI Pattern

```kotlin
// State
data class TenderListState(
    val tenders: List<Tender> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filter: TenderFilter = TenderFilter.ALL
)

// Intent (Events)
sealed class TenderListIntent {
    object LoadTenders : TenderListIntent()
    data class FilterChanged(val filter: TenderFilter) : TenderListIntent()
    data class TenderClicked(val tenderId: String) : TenderListIntent()
}

// ViewModel
class TenderListViewModel @Inject constructor(
    private val getTendersUseCase: GetTendersUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(TenderListState())
    val state: StateFlow<TenderListState> = _state.asStateFlow()

    fun onIntent(intent: TenderListIntent) {
        when (intent) {
            is TenderListIntent.LoadTenders -> loadTenders()
            // ...
        }
    }
}
```

### Room Database Schema

```kotlin
@Entity(tableName = "tenders")
data class TenderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val reference: String,
    val sourceId: String,
    val deadline: Long?,  // Unix timestamp
    val estimatedValue: Double?,
    val requirementsSummary: String?,
    val score: Int?,
    val scoreExplanation: String?,
    val pdfUrl: String,
    val localPdfPath: String?,
    val extractedText: String?,
    val createdAt: Long,
    val syncedAt: Long?
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val content: String,
    val role: String,  // "user" or "assistant"
    val tenderId: String?,  // Optional: if message relates to specific tender
    val timestamp: Long
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val key: String,
    val value: String
)
```

---

## Implementation Plan

### Phase 1: Project Setup (8 tasks)
- Create Android project with Kotlin DSL
- Configure multi-module structure
- Set up Gradle version catalogs
- Configure Hilt DI
- Add Compose dependencies
- Configure BuildConfig for environments
- Set up signing configs
- Configure ProGuard/R8

### Phase 2: Domain Layer (6 tasks)
- Create Tender entity
- Create ChatMessage entity
- Create repository interfaces
- Create GetTendersUseCase
- Create SearchTendersUseCase
- Unit tests for use cases

### Phase 3: Data Layer (8 tasks)
- Create Room database
- Create TenderDao
- Create ChatMessageDao
- Create entity classes
- Create repository implementations
- Database migration setup
- Instrumented tests for Room

### Phase 4: Presentation Layer (10 tasks)
- Configure Material Design 3 theme
- Create navigation graph
- Create placeholder screens
- Implement base MVI ViewModel
- Create TenderListScreen (placeholder)
- Create TenderDetailScreen (placeholder)
- Create ChatScreen (placeholder)
- Create SettingsScreen (placeholder)
- Bottom navigation bar
- Dark/Light theme toggle

### Phase 5: Build & Release (4 tasks)
- Configure debug build
- Configure release build
- Generate signing keys
- Test APK installation

---

## Configuration

```kotlin
// gradle/libs.versions.toml
[versions]
kotlin = "1.9.22"
compose-compiler = "1.5.10"
compose-bom = "2024.02.00"
hilt = "2.50"
room = "2.6.1"
retrofit = "2.9.0"
coroutines = "1.7.3"
mediapipe = "0.10.9"

[libraries]
# Compose
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-navigation = { module = "androidx.navigation:navigation-compose" }

# Hilt
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose" }

# Room
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

# MediaPipe (for Gemma)
mediapipe-llm = { module = "com.google.mediapipe:tasks-genai", version.ref = "mediapipe" }
```

---

## Dependencies

### Gradle Dependencies
```kotlin
// App module
dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.navigation)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.android)
}
```

---

## Performance Targets

- **Cold start**: < 2 seconds
- **APK size**: < 30 MB (before ML model)
- **Memory usage**: < 100 MB baseline
- **Room queries**: < 50ms for list operations

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Compose learning curve | Slower development | Use established patterns, copy from samples |
| Multi-module complexity | Build time increase | Configure Gradle caching, parallel builds |
| Room schema changes | Data loss | Implement migrations from v1 |
| ProGuard issues | Runtime crashes | Test release builds early, add keep rules |

---

## Testing Strategy

1. **Unit Tests** (Domain layer):
   - Entity serialization
   - Use case business logic
   - Repository contract tests

2. **Instrumented Tests** (Data layer):
   - Room DAO operations
   - Database migrations
   - Repository implementations

3. **UI Tests** (Presentation layer):
   - Compose UI tests
   - Navigation tests
   - Screenshot tests (optional)

---

## Success Metrics

- [ ] App builds and runs on emulator
- [ ] All layers properly separated
- [ ] MVI pattern working
- [ ] Room database operational
- [ ] Navigation works between screens
- [ ] Theme switching works
- [ ] Release APK generated
- [ ] All unit tests pass
- [ ] All instrumented tests pass

---

## Future Enhancements

- Tablet-optimized layouts
- Wear OS companion app
- Widget for home screen
- iOS version with KMM
- Accessibility improvements
