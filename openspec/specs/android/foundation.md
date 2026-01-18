# Android Foundation Specification

**Feature ID**: FR-MOBILE-001
**Status**: Implemented
**Last Updated**: 2026-01-18

---

## Overview

The Android Foundation module establishes the core architecture for the Tender Tracker Pro Android application using Kotlin, Jetpack Compose, and modern Android development best practices.

## Architecture

### Multi-Module Structure

```
android/
├── app/                    # UI layer (Compose screens, navigation, theme)
├── domain/                 # Business logic (entities, use cases, repositories)
└── data/                   # Data layer (Room, network, repositories impl)
```

### Technology Stack

- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose (Material3)
- **DI**: Hilt 2.50
- **Database**: Room 2.6.1
- **Async**: Kotlin Coroutines 1.7.3
- **Build**: Gradle with Version Catalogs

---

## Domain Layer

### Entities

#### Tender
```kotlin
data class Tender(
    val id: String,
    val title: String,
    val reference: String,
    val sourceId: String,
    val deadline: Instant?,
    val estimatedValue: Double?,
    val requirementsSummary: String?,
    val score: Int?,
    val scoreExplanation: String?,
    val pdfUrl: String,
    val localPdfPath: String?,
    val extractedText: String?,
    val createdAt: Instant,
    val syncedAt: Instant?
)
```

#### ChatMessage
```kotlin
data class ChatMessage(
    val id: String,
    val content: String,
    val role: MessageRole,
    val tenderId: String?,
    val timestamp: Instant
)

enum class MessageRole {
    USER, ASSISTANT
}
```

### Repositories

#### TenderRepository
```kotlin
interface TenderRepository {
    fun getTenders(): Flow<List<Tender>>
    fun getTenderById(id: String): Flow<Tender?>
    fun searchTenders(query: String): Flow<List<Tender>>
    suspend fun insertTender(tender: Tender)
    suspend fun updateTenderScore(id: String, score: Int, explanation: String)
}
```

---

## Data Layer

### Database Schema

#### TenderDatabase
```kotlin
@Database(
    entities = [TenderEntity::class, ChatMessageEntity::class, UserPreferencesEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TenderDatabase : RoomDatabase() {
    abstract fun tenderDao(): TenderDao
    abstract fun chatMessageDao(): ChatMessageDao
}
```

#### TenderEntity
```kotlin
@Entity(tableName = "tenders")
data class TenderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val reference: String,
    val sourceId: String,
    val deadline: Long?,
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
```

#### TenderDao
```kotlin
@Dao
interface TenderDao {
    @Query("SELECT * FROM tenders ORDER BY createdAt DESC")
    fun getAllTenders(): Flow<List<TenderEntity>>

    @Query("SELECT * FROM tenders WHERE id = :id")
    fun getTenderById(id: String): Flow<TenderEntity?>

    @Query("SELECT * FROM tenders WHERE title LIKE '%' || :query || '%' OR reference LIKE '%' || :query || '%'")
    fun searchTenders(query: String): Flow<List<TenderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTender(tender: TenderEntity)

    @Query("UPDATE tenders SET score = :score, scoreExplanation = :explanation WHERE id = :id")
    suspend fun updateScore(id: String, score: Int, explanation: String)
}
```

---

## Presentation Layer

### Navigation

#### Screen Routes
```kotlin
sealed class Screen(val route: String) {
    object TenderList : Screen("tender_list")
    object TenderDetail : Screen("tender_detail/{tenderId}") {
        fun createRoute(tenderId: String) = "tender_detail/$tenderId"
    }
    object Chat : Screen("chat")
    object Settings : Screen("settings")
}
```

#### NavHost
```kotlin
@Composable
fun TenderTrackerNavHost(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.TenderList.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.TenderList.route) { TenderListScreen(navController) }
            composable(Screen.TenderDetail.route) { TenderDetailScreen(navController) }
            composable(Screen.Chat.route) { ChatScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
        }
    }
}
```

### Theme

#### Material3 Theme
```kotlin
@Composable
fun TenderTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

---

## Build Configuration

### Version Catalog (libs.versions.toml)
```toml
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
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-material3 = { module = "androidx.compose.material3:material3" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
mediapipe-llm = { module = "com.google.mediapipe:tasks-genai", version.ref = "mediapipe" }

[plugins]
android-application = { id = "com.android.application", version = "8.2.2" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### Application Entry Point

#### TenderTrackerApp
```kotlin
@HiltAndroidApp
class TenderTrackerApp : Application()
```

#### MainActivity
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TenderTrackerTheme {
                TenderTrackerNavHost()
            }
        }
    }
}
```

---

## Testing

### Unit Tests
- Domain entities (Tender, ChatMessage)
- Use cases (GetTendersUseCase, SearchTendersUseCase)

### Instrumented Tests
- Room database creation and migration
- DAO operations (insert, query, update)

### Test Command
```bash
./gradlew test connectedAndroidTest
```

---

## Build & Release

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
1. Generate signing key
2. Configure `keystore.properties`
3. Build release APK:
```bash
./gradlew assembleRelease
```

---

## Dependencies

- **Hilt**: Dependency injection
- **Room**: Local database
- **Compose**: Declarative UI
- **Coroutines**: Async operations
- **Navigation**: Screen navigation

---

## References

- **Proposal**: `openspec/archive/fr-mobile-001-android-foundation/proposal.md`
- **Tasks**: `openspec/archive/fr-mobile-001-android-foundation/tasks.md`
- **Implementation**: 36/36 tasks complete

---

**Archived**: 2026-01-18
**Change ID**: fr-mobile-001-android-foundation
