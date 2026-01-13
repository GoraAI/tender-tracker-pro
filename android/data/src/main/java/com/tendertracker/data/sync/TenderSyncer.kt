package com.tendertracker.data.sync

import com.tendertracker.data.local.dao.TenderDao
import com.tendertracker.data.remote.api.TenderApi
import com.tendertracker.data.remote.dto.TenderDto
import java.time.Instant
import javax.inject.Inject

data class SyncStats(val added: Int, val updated: Int, val deleted: Int)

class SyncException(message: String) : Exception(message)

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
            val response = tenderApi.getTenders(
                page = page,
                limit = SyncConfig.SYNC_PAGE_SIZE
            )
            if (!response.isSuccessful) {
                throw SyncException("Failed to fetch tenders: ${response.code()}")
            }

            val body = response.body()
                ?: throw SyncException("Empty response body")

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

        val body = response.body()
            ?: throw SyncException("Empty response body")

        val stats = mergeTenders(body.tenders)

        syncStatusManager.setLastTenderSync(Instant.now())
        stats
    }

    private suspend fun mergeTenders(remote: List<TenderDto>): SyncStats {
        var added = 0
        var updated = 0

        for (dto in remote) {
            val existing = tenderDao.getTenderCount()
            val entity = dto.toEntity()

            tenderDao.insertTender(entity)

            if (existing == 0) {
                added++
            } else {
                updated++
            }
        }

        return SyncStats(added, updated, 0)
    }
}
