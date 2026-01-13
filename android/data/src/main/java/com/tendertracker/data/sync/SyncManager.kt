package com.tendertracker.data.sync

import com.tendertracker.data.remote.NetworkMonitor
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(
    val success: Boolean,
    val tendersAdded: Int = 0,
    val tendersUpdated: Int = 0,
    val scoresUploaded: Int = 0,
    val errors: List<String> = emptyList(),
    val duration: Duration = Duration.ZERO
)

@Singleton
class SyncManager @Inject constructor(
    private val tenderSyncer: TenderSyncer,
    private val syncStatusManager: SyncStatusManager,
    private val networkMonitor: NetworkMonitor
) {
    val syncStatus: StateFlow<SyncStatus> = syncStatusManager.syncStatus

    suspend fun syncAll(): SyncResult {
        if (!networkMonitor.isOnline()) {
            syncStatusManager.setSyncStatus(SyncStatus.OFFLINE)
            return SyncResult(
                success = false,
                errors = listOf("No network connection")
            )
        }

        syncStatusManager.setSyncStatus(SyncStatus.SYNCING)

        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        var tendersAdded = 0
        var tendersUpdated = 0

        // Sync tenders
        tenderSyncer.syncAll()
            .onSuccess {
                tendersAdded = it.added
                tendersUpdated = it.updated
            }
            .onFailure {
                errors.add("Tender sync failed: ${it.message}")
            }

        val status = if (errors.isEmpty()) SyncStatus.IDLE else SyncStatus.ERROR
        syncStatusManager.setSyncStatus(status)

        return SyncResult(
            success = errors.isEmpty(),
            tendersAdded = tendersAdded,
            tendersUpdated = tendersUpdated,
            errors = errors,
            duration = Duration.ofMillis(System.currentTimeMillis() - startTime)
        )
    }

    suspend fun syncIncremental(): SyncResult {
        if (!networkMonitor.isOnline()) {
            syncStatusManager.setSyncStatus(SyncStatus.OFFLINE)
            return SyncResult(
                success = false,
                errors = listOf("No network connection")
            )
        }

        syncStatusManager.setSyncStatus(SyncStatus.SYNCING)

        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        var tendersAdded = 0
        var tendersUpdated = 0

        tenderSyncer.syncIncremental()
            .onSuccess {
                tendersAdded = it.added
                tendersUpdated = it.updated
            }
            .onFailure {
                errors.add("Sync failed: ${it.message}")
            }

        val status = if (errors.isEmpty()) SyncStatus.IDLE else SyncStatus.ERROR
        syncStatusManager.setSyncStatus(status)

        return SyncResult(
            success = errors.isEmpty(),
            tendersAdded = tendersAdded,
            tendersUpdated = tendersUpdated,
            errors = errors,
            duration = Duration.ofMillis(System.currentTimeMillis() - startTime)
        )
    }

    suspend fun getLastSyncTime(): Instant? =
        syncStatusManager.getLastTenderSync()
}
