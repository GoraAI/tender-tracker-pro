# Spec Delta: FR-MOBILE-004 Tender Sync

## ADDED

### android/data/src/main/java/com/tendertracker/data/remote/api/TenderApi.kt
```kotlin
interface TenderApi {
    @GET("tenders")
    suspend fun getTenders(
        @Query("since") since: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): Response<TenderListResponse>

    @GET("tenders/{id}")
    suspend fun getTender(@Path("id") id: String): Response<TenderResponse>

    @POST("tenders/{id}/score")
    suspend fun submitScore(
        @Path("id") tenderId: String,
        @Body score: ScoreSubmission
    ): Response<ScoreResponse>

    @POST("auth/login")
    suspend fun login(@Body credentials: LoginRequest): Response<TokenResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(): Response<TokenResponse>
}
```

### android/data/src/main/java/com/tendertracker/data/remote/dto/TenderDto.kt
```kotlin
data class TenderListResponse(
    val tenders: List<TenderDto>,
    val total: Int,
    val page: Int,
    val hasMore: Boolean
)

data class TenderDto(
    val id: String,
    val title: String,
    val reference: String,
    val sourceId: String,
    val deadline: String?,
    val estimatedValue: Double?,
    val requirementsSummary: String?,
    val score: Int?,
    val scoreExplanation: String?,
    val pdfUrl: String,
    val createdAt: String,
    val updatedAt: String
)

data class ScoreSubmission(
    val score: Int,
    val explanation: String,
    val scoredAt: String
)

data class ScoreResponse(
    val tenderId: String,
    val score: Int,
    val explanation: String,
    val syncedAt: String
)
```

### android/data/src/main/java/com/tendertracker/data/remote/AuthInterceptor.kt
```kotlin
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

### android/data/src/main/java/com/tendertracker/data/local/entity/SyncMetadataEntity.kt
```kotlin
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long
)
```

### android/data/src/main/java/com/tendertracker/data/local/dao/SyncMetadataDao.kt
```kotlin
@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE key = :key")
    suspend fun get(key: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(metadata: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE key = :key")
    suspend fun delete(key: String)
}
```

### android/data/src/main/java/com/tendertracker/data/sync/SyncStatusManager.kt
```kotlin
class SyncStatusManager @Inject constructor(
    private val syncMetadataDao: SyncMetadataDao
) {
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    suspend fun getLastTenderSync(): Instant? {
        val metadata = syncMetadataDao.get(KEY_LAST_TENDER_SYNC)
        return metadata?.value?.let { Instant.parse(it) }
    }

    suspend fun setLastTenderSync(time: Instant) {
        syncMetadataDao.set(
            SyncMetadataEntity(
                key = KEY_LAST_TENDER_SYNC,
                value = time.toString(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun setSyncStatus(status: SyncStatus) {
        _syncStatus.value = status
    }

    companion object {
        const val KEY_LAST_TENDER_SYNC = "last_tender_sync"
        const val KEY_LAST_SCORE_SYNC = "last_score_sync"
    }
}

enum class SyncStatus {
    IDLE, SYNCING, ERROR, OFFLINE
}
```

### android/data/src/main/java/com/tendertracker/data/sync/TenderSyncer.kt
```kotlin
class TenderSyncer @Inject constructor(
    private val tenderApi: TenderApi,
    private val tenderDao: TenderDao,
    private val syncStatusManager: SyncStatusManager
) {
    suspend fun syncAll(): Result<SyncStats> = runCatching {
        var page = 1
        var totalAdded = 0
        var totalUpdated = 0

        do {
            val response = tenderApi.getTenders(page = page, limit = SyncConfig.SYNC_PAGE_SIZE)
            if (!response.isSuccessful) {
                throw SyncException("Failed to fetch tenders: ${response.code()}")
            }

            val body = response.body()!!
            val stats = mergeTenders(body.tenders)
            totalAdded += stats.added
            totalUpdated += stats.updated

            page++
        } while (body.hasMore && page * SyncConfig.SYNC_PAGE_SIZE < SyncConfig.MAX_TENDERS_PER_SYNC)

        syncStatusManager.setLastTenderSync(Instant.now())
        SyncStats(added = totalAdded, updated = totalUpdated, deleted = 0)
    }

    suspend fun syncIncremental(): Result<SyncStats> = runCatching {
        val lastSync = syncStatusManager.getLastTenderSync()?.toString()

        val response = tenderApi.getTenders(since = lastSync)
        if (!response.isSuccessful) {
            throw SyncException("Failed to fetch tenders: ${response.code()}")
        }

        val body = response.body()!!
        val stats = mergeTenders(body.tenders)

        syncStatusManager.setLastTenderSync(Instant.now())
        stats
    }

    private suspend fun mergeTenders(remote: List<TenderDto>): SyncStats {
        var added = 0
        var updated = 0

        for (dto in remote) {
            val existing = tenderDao.getTenderById(dto.id).firstOrNull()
            val entity = dto.toEntity()

            if (existing == null) {
                tenderDao.insertTender(entity)
                added++
            } else {
                tenderDao.insertTender(entity)  // Upsert
                updated++
            }
        }

        return SyncStats(added, updated, 0)
    }
}

data class SyncStats(val added: Int, val updated: Int, val deleted: Int)
class SyncException(message: String) : Exception(message)
```

### android/data/src/main/java/com/tendertracker/data/sync/ScoreSyncer.kt
```kotlin
class ScoreSyncer @Inject constructor(
    private val tenderApi: TenderApi,
    private val tenderDao: TenderDao
) {
    suspend fun uploadPendingScores(): Result<Int> = runCatching {
        val pending = tenderDao.getTendersWithPendingScores()
        var uploaded = 0

        for (tender in pending) {
            if (tender.score == null) continue

            val submission = ScoreSubmission(
                score = tender.score,
                explanation = tender.scoreExplanation ?: "",
                scoredAt = Instant.now().toString()
            )

            val response = tenderApi.submitScore(tender.id, submission)
            if (response.isSuccessful) {
                tenderDao.markScoreSynced(tender.id)
                uploaded++
            }
        }

        uploaded
    }
}
```

### android/data/src/main/java/com/tendertracker/data/sync/SyncManager.kt
```kotlin
class SyncManager @Inject constructor(
    private val tenderSyncer: TenderSyncer,
    private val scoreSyncer: ScoreSyncer,
    private val syncStatusManager: SyncStatusManager,
    private val networkMonitor: NetworkMonitor
) {
    val syncStatus: StateFlow<SyncStatus> = syncStatusManager.syncStatus

    suspend fun syncAll(): SyncResult {
        if (!networkMonitor.isOnline()) {
            syncStatusManager.setSyncStatus(SyncStatus.OFFLINE)
            return SyncResult(success = false, errors = listOf("No network connection"))
        }

        syncStatusManager.setSyncStatus(SyncStatus.SYNCING)

        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        var tendersAdded = 0
        var tendersUpdated = 0
        var scoresUploaded = 0

        // Sync tenders
        tenderSyncer.syncAll()
            .onSuccess {
                tendersAdded = it.added
                tendersUpdated = it.updated
            }
            .onFailure { errors.add("Tender sync failed: ${it.message}") }

        // Upload scores
        scoreSyncer.uploadPendingScores()
            .onSuccess { scoresUploaded = it }
            .onFailure { errors.add("Score sync failed: ${it.message}") }

        val status = if (errors.isEmpty()) SyncStatus.IDLE else SyncStatus.ERROR
        syncStatusManager.setSyncStatus(status)

        return SyncResult(
            success = errors.isEmpty(),
            tendersAdded = tendersAdded,
            tendersUpdated = tendersUpdated,
            scoresUploaded = scoresUploaded,
            errors = errors,
            duration = Duration.ofMillis(System.currentTimeMillis() - startTime)
        )
    }

    suspend fun syncIncremental(): SyncResult {
        // Similar to syncAll but uses syncIncremental
    }

    fun getLastSyncTime(): Instant? = runBlocking {
        syncStatusManager.getLastTenderSync()
    }
}

data class SyncResult(
    val success: Boolean,
    val tendersAdded: Int = 0,
    val tendersUpdated: Int = 0,
    val scoresUploaded: Int = 0,
    val errors: List<String> = emptyList(),
    val duration: Duration = Duration.ZERO
)
```

### android/data/src/main/java/com/tendertracker/data/sync/SyncWorker.kt
```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val syncType = inputData.getString(KEY_SYNC_TYPE) ?: SYNC_TYPE_INCREMENTAL

        val result = when (syncType) {
            SYNC_TYPE_FULL -> syncManager.syncAll()
            SYNC_TYPE_INCREMENTAL -> syncManager.syncIncremental()
            else -> syncManager.syncIncremental()
        }

        return if (result.success) {
            Result.success()
        } else if (runAttemptCount < SyncConfig.MAX_RETRY_ATTEMPTS) {
            Result.retry()
        } else {
            Result.failure()
        }
    }

    companion object {
        const val KEY_SYNC_TYPE = "sync_type"
        const val SYNC_TYPE_FULL = "full"
        const val SYNC_TYPE_INCREMENTAL = "incremental"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = SyncConfig.PERIODIC_SYNC_INTERVAL_MINUTES,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    SyncConfig.INITIAL_BACKOFF_SECONDS,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "periodic_sync",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
```

### android/app/src/main/java/com/tendertracker/push/TenderPushService.kt
```kotlin
class TenderPushService : FirebaseMessagingService() {
    @Inject
    lateinit var workManager: WorkManager

    override fun onMessageReceived(message: RemoteMessage) {
        when (message.data["type"]) {
            "new_tenders" -> triggerSync(SyncWorker.SYNC_TYPE_INCREMENTAL)
            "tender_updated" -> triggerSync(SyncWorker.SYNC_TYPE_INCREMENTAL)
            "full_sync" -> triggerSync(SyncWorker.SYNC_TYPE_FULL)
        }
    }

    private fun triggerSync(syncType: String) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf(SyncWorker.KEY_SYNC_TYPE to syncType))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            "push_sync",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun onNewToken(token: String) {
        // Send token to backend for push registration
    }
}
```

### android/app/src/main/java/com/tendertracker/ui/components/SyncStatusIndicator.kt
```kotlin
@Composable
fun SyncStatusIndicator(
    status: SyncStatus,
    lastSync: Instant?,
    pendingCount: Int,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSyncClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status icon
        when (status) {
            SyncStatus.IDLE -> Icon(
                Icons.Default.CloudDone,
                contentDescription = "Synced",
                tint = Color(0xFF4CAF50)
            )
            SyncStatus.SYNCING -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            SyncStatus.ERROR -> Icon(
                Icons.Default.CloudOff,
                contentDescription = "Sync error",
                tint = Color(0xFFF44336)
            )
            SyncStatus.OFFLINE -> Icon(
                Icons.Default.WifiOff,
                contentDescription = "Offline",
                tint = Color(0xFF9E9E9E)
            )
        }

        // Pending badge
        if (pendingCount > 0) {
            Badge {
                Text("$pendingCount")
            }
        }

        // Last sync time
        lastSync?.let { time ->
            Text(
                text = formatRelativeTime(time),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatRelativeTime(time: Instant): String {
    val now = Instant.now()
    val duration = Duration.between(time, now)

    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
        duration.toHours() < 24 -> "${duration.toHours()}h ago"
        else -> "${duration.toDays()}d ago"
    }
}
```

## MODIFIED

### android/data/src/main/java/com/tendertracker/data/local/entity/TenderEntity.kt
```diff
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
-    val syncedAt: Long?
+    val syncedAt: Long?,
+    val scoreSyncStatus: String = "synced"  // "synced", "pending", "error"
 )
```

### android/data/src/main/java/com/tendertracker/data/local/dao/TenderDao.kt
```diff
 @Dao
 interface TenderDao {
     // ... existing methods ...

+    @Query("SELECT * FROM tenders WHERE scoreSyncStatus = 'pending'")
+    suspend fun getTendersWithPendingScores(): List<TenderEntity>
+
+    @Query("UPDATE tenders SET scoreSyncStatus = 'synced', syncedAt = :now WHERE id = :id")
+    suspend fun markScoreSynced(id: String, now: Long = System.currentTimeMillis())
+
+    @Query("UPDATE tenders SET score = :score, scoreExplanation = :explanation, scoreSyncStatus = 'pending' WHERE id = :id")
+    suspend fun updateScoreLocal(id: String, score: Int, explanation: String)
 }
```

### android/data/src/main/java/com/tendertracker/data/local/TenderDatabase.kt
```diff
 @Database(
-    entities = [TenderEntity::class, ChatMessageEntity::class],
-    version = 2,
+    entities = [TenderEntity::class, ChatMessageEntity::class, SyncMetadataEntity::class],
+    version = 3,
     exportSchema = true
 )
 abstract class TenderDatabase : RoomDatabase() {
     abstract fun tenderDao(): TenderDao
     abstract fun chatMessageDao(): ChatMessageDao
+    abstract fun syncMetadataDao(): SyncMetadataDao
 }
```

## REMOVED

None.
