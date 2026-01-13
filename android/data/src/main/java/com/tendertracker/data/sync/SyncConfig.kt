package com.tendertracker.data.sync

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
