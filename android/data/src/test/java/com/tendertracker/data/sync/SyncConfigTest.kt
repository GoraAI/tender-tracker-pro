package com.tendertracker.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncConfigTest {

    @Test
    fun `periodic sync interval is reasonable`() {
        assertTrue(SyncConfig.PERIODIC_SYNC_INTERVAL_MINUTES >= 15)
    }

    @Test
    fun `min sync interval is less than periodic`() {
        assertTrue(
            SyncConfig.MIN_SYNC_INTERVAL_MINUTES <
            SyncConfig.PERIODIC_SYNC_INTERVAL_MINUTES
        )
    }

    @Test
    fun `max retry attempts is positive`() {
        assertTrue(SyncConfig.MAX_RETRY_ATTEMPTS > 0)
    }

    @Test
    fun `page size is reasonable`() {
        assertEquals(100, SyncConfig.SYNC_PAGE_SIZE)
    }

    @Test
    fun `max tenders per sync is capped`() {
        assertTrue(SyncConfig.MAX_TENDERS_PER_SYNC <= 1000)
    }

    @Test
    fun `api timeout is reasonable`() {
        assertTrue(SyncConfig.API_TIMEOUT_SECONDS >= 10)
        assertTrue(SyncConfig.API_TIMEOUT_SECONDS <= 60)
    }
}
