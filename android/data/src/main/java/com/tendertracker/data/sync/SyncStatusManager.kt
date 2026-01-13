package com.tendertracker.data.sync

import com.tendertracker.data.local.dao.SyncMetadataDao
import com.tendertracker.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncStatus {
    IDLE, SYNCING, ERROR, OFFLINE
}

@Singleton
class SyncStatusManager @Inject constructor(
    private val syncMetadataDao: SyncMetadataDao
) {
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    suspend fun getLastTenderSync(): Instant? {
        val metadata = syncMetadataDao.get(KEY_LAST_TENDER_SYNC)
        return metadata?.value?.let {
            try {
                Instant.parse(it)
            } catch (e: Exception) {
                null
            }
        }
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

    suspend fun getLastScoreSync(): Instant? {
        val metadata = syncMetadataDao.get(KEY_LAST_SCORE_SYNC)
        return metadata?.value?.let {
            try {
                Instant.parse(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun setLastScoreSync(time: Instant) {
        syncMetadataDao.set(
            SyncMetadataEntity(
                key = KEY_LAST_SCORE_SYNC,
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
