package com.tendertracker.domain.repository

import com.tendertracker.domain.model.Tender
import kotlinx.coroutines.flow.Flow

interface TenderRepository {
    fun getAllTenders(): Flow<List<Tender>>
    fun getTenderById(id: String): Flow<Tender?>
    fun searchTenders(query: String): Flow<List<Tender>>
    fun getHighPriorityTenders(minScore: Int = 70): Flow<List<Tender>>
    suspend fun insertTender(tender: Tender)
    suspend fun updateScore(id: String, score: Int, explanation: String)
    suspend fun deleteTender(id: String)
    suspend fun syncFromApi(): Result<Int>

    /**
     * Run the pipeline to discover new tenders, wait for completion, then sync.
     * @param sourceId The source to scrape (e.g., "jhb-water")
     * @param onProgress Callback for progress updates (0-100)
     * @return Result with number of tenders synced
     */
    suspend fun runPipelineAndSync(
        sourceId: String = "jhb-water",
        onProgress: (Int) -> Unit = {}
    ): Result<Int>
}
