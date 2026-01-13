# FR-MOBILE-004: Tender Sync and Offline Database

**Status**: PROPOSED
**Priority**: P0
**Created**: 2026-01-13
**Dependencies**: FR-MOBILE-001 (Android Foundation)

---

## Summary

Implement tender synchronization between the mobile app and backend server, with full offline capability using Room database, conflict resolution, and background sync via WorkManager.

---

## Motivation

South African users need offline-first capability:
- **Poor connectivity**: Many areas have unreliable mobile networks
- **Data costs**: Minimize network usage by syncing efficiently
- **Field work**: Users may review tenders in areas without signal
- **Battery**: Reduce network overhead for battery life

Key requirements:
- Browse tenders offline
- Search offline database
- Queue actions for later sync
- Background sync when connected
- Conflict resolution for scores

---

## Scope

### In Scope
- Full tender sync from backend API
- Incremental sync (only changed tenders)
- Offline tender browsing and search
- Local score storage with sync
- Background sync with WorkManager
- Sync status indicators
- Conflict resolution (server wins for tenders, merge for scores)
- Network state monitoring
- Sync retry with exponential backoff
- Push notification integration (FCM)

### Out of Scope
- Real-time sync (WebSocket)
- Peer-to-peer sync
- Full-text search indexing (use Room's FTS)
- Attachment sync (PDFs stay on server)
- Multi-device sync conflict resolution

---

## Acceptance Criteria

- [ ] Can sync all tenders from backend
- [ ] Incremental sync only fetches changes
- [ ] Tenders viewable when offline
- [ ] Search works offline
- [ ] Scores saved locally when offline
- [ ] Scores sync to server when online
- [ ] Background sync runs periodically
- [ ] Sync status shown in UI
- [ ] Push notifications trigger sync
- [ ] Handles network errors gracefully
- [ ] Retry with exponential backoff
- [ ] Unit tests for sync logic
- [ ] Integration tests for API calls

---

## Technical Approach

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    SYNC LAYER                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                  SyncManager                          │  │
│  │  - syncAll()                                         │  │
│  │  - syncIncremental()                                 │  │
│  │  - syncScores()                                      │  │
│  │  - getSyncStatus()                                   │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                  │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐  │
│  │  TenderSyncer  │  │  ScoreSyncer   │  │  SyncWorker  │  │
│  │  - fetchNew()  │  │  - uploadLocal │  │  - periodic  │  │
│  │  - merge()     │  │  - resolve()   │  │  - oneTime   │  │
│  └────────────────┘  └────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────────┐
│                    DATA LAYER                                │
│  ┌──────────────────┐      ┌──────────────────┐            │
│  │   Room Database  │      │   Retrofit API   │            │
│  │  - TenderDao     │      │  - getTenders()  │            │
│  │  - SyncMetaDao   │      │  - postScores()  │            │
│  └──────────────────┘      └──────────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

### Sync Strategy

```
1. INITIAL SYNC (first app launch):
   - Fetch all tenders from server
   - Store in Room database
   - Save last sync timestamp

2. INCREMENTAL SYNC (subsequent):
   - Send last sync timestamp to server
   - Receive only tenders modified after timestamp
   - Upsert into Room database
   - Update sync timestamp

3. SCORE SYNC:
   - Local scores marked as "pending sync"
   - On connectivity, upload pending scores
   - Server returns merged scores
   - Update local with server response

4. CONFLICT RESOLUTION:
   - Tenders: Server wins (authoritative source)
   - Scores: Most recent wins (by timestamp)
```

### Sync Metadata Table

```kotlin
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long
)

// Keys:
// - "last_tender_sync" -> ISO timestamp
// - "last_score_sync" -> ISO timestamp
// - "sync_status" -> "idle" | "syncing" | "error"
// - "pending_scores_count" -> number
```

### API Endpoints

```kotlin
interface TenderApi {
    @GET("tenders")
    suspend fun getTenders(
        @Query("since") since: String? = null,  // ISO timestamp for incremental
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): TenderListResponse

    @GET("tenders/{id}")
    suspend fun getTender(@Path("id") id: String): TenderResponse

    @POST("tenders/{id}/score")
    suspend fun submitScore(
        @Path("id") tenderId: String,
        @Body score: ScoreSubmission
    ): ScoreResponse
}
```

### WorkManager Integration

```kotlin
// Periodic sync (every 15 minutes when connected)
val periodicSync = PeriodicWorkRequestBuilder<SyncWorker>(
    repeatInterval = 15,
    repeatIntervalTimeUnit = TimeUnit.MINUTES
)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        1,
        TimeUnit.MINUTES
    )
    .build()

// One-time sync (triggered by push notification)
val oneTimeSync = OneTimeWorkRequestBuilder<SyncWorker>()
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
    .build()
```

### FCM Push Notification

```kotlin
class TenderPushService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        when (message.data["type"]) {
            "new_tenders" -> triggerSync()
            "tender_updated" -> syncTender(message.data["tender_id"])
            "score_updated" -> syncScores()
        }
    }

    private fun triggerSync() {
        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "push_sync",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SyncWorker>().build()
            )
    }
}
```

---

## Implementation Plan

### Phase 1: API Client (5 tasks)
- Add Retrofit dependencies
- Create TenderApi interface
- Create API response DTOs
- Create API error handling
- Create AuthInterceptor for JWT

### Phase 2: Sync Metadata (4 tasks)
- Create SyncMetadataEntity
- Create SyncMetadataDao
- Create SyncStatusManager
- Track last sync timestamps

### Phase 3: Tender Sync (6 tasks)
- Create TenderSyncer class
- Implement fetchAllTenders()
- Implement fetchIncrementalTenders()
- Implement mergeTenders() with conflict resolution
- Create SyncManager orchestrator
- Handle pagination for large datasets

### Phase 4: Score Sync (4 tasks)
- Add syncStatus field to TenderEntity
- Create ScoreSyncer class
- Implement uploadPendingScores()
- Implement conflict resolution for scores

### Phase 5: Background Sync (4 tasks)
- Create SyncWorker (WorkManager)
- Configure periodic sync (15 min)
- Configure one-time sync
- Add retry with exponential backoff

### Phase 6: Push Notifications (4 tasks)
- Add Firebase dependencies
- Create TenderPushService
- Handle push message types
- Trigger sync from push

### Phase 7: UI Integration (4 tasks)
- Create SyncStatusIndicator composable
- Show sync status in header
- Show pending changes count
- Manual sync button

### Phase 8: Testing (5 tasks)
- Unit tests for TenderSyncer
- Unit tests for ScoreSyncer
- Mock API tests for sync flow
- WorkManager tests
- Integration test for full sync

---

## Configuration

```kotlin
object SyncConfig {
    // Sync intervals
    const val PERIODIC_SYNC_INTERVAL_MINUTES = 15L
    const val MIN_SYNC_INTERVAL_MINUTES = 5L

    // Retry
    const val MAX_RETRY_ATTEMPTS = 3
    const val INITIAL_BACKOFF_SECONDS = 60L

    // Pagination
    const val SYNC_PAGE_SIZE = 100
    const val MAX_TENDERS_PER_SYNC = 1000

    // Timeouts
    const val API_TIMEOUT_SECONDS = 30L
    const val SYNC_TIMEOUT_MINUTES = 10L
}
```

---

## API Design

### SyncManager

```kotlin
class SyncManager @Inject constructor(
    private val tenderSyncer: TenderSyncer,
    private val scoreSyncer: ScoreSyncer,
    private val syncStatusManager: SyncStatusManager,
    private val networkMonitor: NetworkMonitor
) {
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    suspend fun syncAll(): SyncResult

    suspend fun syncIncremental(): SyncResult

    suspend fun syncScores(): SyncResult

    fun getSyncStatus(): SyncStatus

    fun getLastSyncTime(): Instant?

    fun getPendingScoresCount(): Int

    fun cancelSync()
}
```

### TenderSyncer

```kotlin
class TenderSyncer @Inject constructor(
    private val tenderApi: TenderApi,
    private val tenderDao: TenderDao,
    private val syncMetadataDao: SyncMetadataDao
) {
    suspend fun syncAll(): Result<SyncStats>

    suspend fun syncIncremental(): Result<SyncStats>

    suspend fun syncSingle(tenderId: String): Result<Tender>

    private suspend fun fetchPage(page: Int, since: String?): List<TenderDto>

    private suspend fun mergeTenders(remote: List<TenderDto>)
}
```

### Data Classes

```kotlin
data class SyncResult(
    val success: Boolean,
    val tendersAdded: Int,
    val tendersUpdated: Int,
    val scoresUploaded: Int,
    val errors: List<String>,
    val duration: Duration
)

enum class SyncStatus {
    IDLE,
    SYNCING,
    ERROR,
    OFFLINE
}

data class SyncStats(
    val added: Int,
    val updated: Int,
    val deleted: Int
)
```

---

## Offline Indicators

```kotlin
@Composable
fun SyncStatusIndicator(
    status: SyncStatus,
    lastSync: Instant?,
    pendingCount: Int,
    onSyncClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable(onClick = onSyncClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (status) {
                SyncStatus.IDLE -> Icons.Default.CloudDone
                SyncStatus.SYNCING -> Icons.Default.Sync
                SyncStatus.ERROR -> Icons.Default.CloudOff
                SyncStatus.OFFLINE -> Icons.Default.WifiOff
            },
            contentDescription = null,
            tint = when (status) {
                SyncStatus.IDLE -> Color.Green
                SyncStatus.SYNCING -> Color.Blue
                SyncStatus.ERROR -> Color.Red
                SyncStatus.OFFLINE -> Color.Gray
            }
        )

        if (pendingCount > 0) {
            Badge { Text("$pendingCount") }
        }

        lastSync?.let {
            Text(
                text = "Last sync: ${formatRelativeTime(it)}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
```

---

## Performance Targets

- **Initial sync**: < 30 seconds for 1000 tenders
- **Incremental sync**: < 5 seconds typical
- **Score upload**: < 2 seconds per score
- **Offline search**: < 100ms
- **Battery impact**: < 1% per day (background sync)

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Large initial sync | Timeout/failure | Pagination, progress tracking |
| Stale data | Wrong decisions | Clear last sync time, show age |
| Sync conflicts | Data loss | Server-wins policy, audit log |
| Battery drain | User complaints | WorkManager constraints, batching |
| Network errors | Failed sync | Exponential backoff, retry |

---

## Testing Strategy

1. **Unit Tests**:
   - TenderSyncer merge logic
   - ScoreSyncer conflict resolution
   - SyncStatusManager state transitions
   - Timestamp handling

2. **Integration Tests**:
   - Full sync with mock server
   - Incremental sync with mock server
   - Score upload and download
   - Error handling

3. **E2E Tests**:
   - Airplane mode → offline browse
   - Restore connection → auto sync
   - Push notification → trigger sync

---

## Success Metrics

- [ ] Initial sync completes successfully
- [ ] Incremental sync fetches only changes
- [ ] Offline browsing works
- [ ] Offline search works
- [ ] Scores sync when online
- [ ] Background sync runs
- [ ] Push notifications trigger sync
- [ ] Retry logic works
- [ ] All tests pass

---

## Future Enhancements

- Real-time sync with WebSocket
- PDF attachment sync
- Selective sync (by source)
- Bandwidth-aware sync
- Delta sync (field-level changes)
- Multi-device conflict resolution
