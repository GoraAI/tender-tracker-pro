package com.tendertracker.data.repository

import com.tendertracker.data.local.dao.TenderDao
import com.tendertracker.data.local.entity.TenderEntity
import com.tendertracker.data.remote.api.TenderApi
import com.tendertracker.data.remote.dto.PipelineRequest
import com.tendertracker.domain.model.Tender
import com.tendertracker.domain.repository.TenderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TenderRepositoryImpl @Inject constructor(
    private val tenderDao: TenderDao,
    private val tenderApi: TenderApi
) : TenderRepository {

    override fun getAllTenders(): Flow<List<Tender>> =
        tenderDao.getAllTenders().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTenderById(id: String): Flow<Tender?> =
        tenderDao.getTenderById(id).map { it?.toDomain() }

    override fun searchTenders(query: String): Flow<List<Tender>> =
        tenderDao.searchTenders(query).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getHighPriorityTenders(minScore: Int): Flow<List<Tender>> =
        tenderDao.getHighPriorityTenders(minScore).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertTender(tender: Tender) {
        tenderDao.insertTender(TenderEntity.fromDomain(tender))
    }

    override suspend fun updateScore(id: String, score: Int, explanation: String) {
        tenderDao.updateScore(id, score, explanation)
    }

    override suspend fun deleteTender(id: String) {
        tenderDao.deleteTender(id)
    }

    /**
     * Sync tenders from the API and cache them locally.
     * Returns the number of new/updated tenders.
     */
    override suspend fun syncFromApi(): Result<Int> {
        return try {
            val response = tenderApi.getTenders(page = 1, limit = 100)
            if (response.isSuccessful) {
                val tenderList = response.body()!!
                val entities = tenderList.tenders.map { it.toEntity() }
                tenderDao.insertTenders(entities)
                Result.success(entities.size)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Failed to fetch tenders"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Run the pipeline to discover new tenders, wait for completion, then sync.
     */
    override suspend fun runPipelineAndSync(
        sourceId: String,
        onProgress: (Int) -> Unit
    ): Result<Int> {
        return try {
            // Step 1: Trigger the discover pipeline
            onProgress(5)
            val pipelineResponse = tenderApi.runDiscoverPipeline(PipelineRequest(sourceId))

            if (!pipelineResponse.isSuccessful) {
                val error = pipelineResponse.errorBody()?.string() ?: "Failed to start pipeline"
                return Result.failure(Exception(error))
            }

            val job = pipelineResponse.body()!!
            val jobId = job.id
            onProgress(10)

            // Step 2: Poll for job completion
            var currentJob = job
            var pollCount = 0
            val maxPolls = 60 // Max 60 polls * 2 seconds = 2 minutes timeout

            while (!currentJob.isComplete && pollCount < maxPolls) {
                delay(2000) // Wait 2 seconds between polls
                pollCount++

                val statusResponse = tenderApi.getPipelineJob(jobId)
                if (statusResponse.isSuccessful) {
                    currentJob = statusResponse.body()!!
                    // Map pipeline progress (0-100) to our progress range (10-80)
                    val mappedProgress = 10 + (currentJob.progress * 0.7).toInt()
                    onProgress(mappedProgress)
                }
            }

            if (!currentJob.isSuccess) {
                val error = currentJob.errorMessage ?: "Pipeline failed or timed out"
                return Result.failure(Exception(error))
            }

            onProgress(85)

            // Step 3: Fetch the discovered tenders
            val syncResult = syncFromApi()
            onProgress(100)

            syncResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
