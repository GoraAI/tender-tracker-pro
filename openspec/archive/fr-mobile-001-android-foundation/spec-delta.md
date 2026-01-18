# Spec Delta: FR-MOBILE-001 Android Foundation

## ADDED

### android/app/src/main/java/com/tendertracker/TenderTrackerApp.kt
```kotlin
@HiltAndroidApp
class TenderTrackerApp : Application()
```

### android/app/src/main/java/com/tendertracker/MainActivity.kt
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

### android/domain/src/main/java/com/tendertracker/domain/model/Tender.kt
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

### android/domain/src/main/java/com/tendertracker/domain/model/ChatMessage.kt
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

### android/domain/src/main/java/com/tendertracker/domain/repository/TenderRepository.kt
```kotlin
interface TenderRepository {
    fun getTenders(): Flow<List<Tender>>
    fun getTenderById(id: String): Flow<Tender?>
    fun searchTenders(query: String): Flow<List<Tender>>
    suspend fun insertTender(tender: Tender)
    suspend fun updateTenderScore(id: String, score: Int, explanation: String)
}
```

### android/data/src/main/java/com/tendertracker/data/local/TenderDatabase.kt
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

### android/data/src/main/java/com/tendertracker/data/local/entity/TenderEntity.kt
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

### android/data/src/main/java/com/tendertracker/data/local/dao/TenderDao.kt
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

### android/app/src/main/java/com/tendertracker/ui/theme/Theme.kt
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

### android/app/src/main/java/com/tendertracker/ui/navigation/Screen.kt
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

### android/app/src/main/java/com/tendertracker/ui/navigation/NavHost.kt
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

### android/gradle/libs.versions.toml
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

## MODIFIED

None - this is a new module.

## REMOVED

None - this is a new module.
