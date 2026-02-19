package com.tendertracker.data.repository

import com.tendertracker.data.local.dao.TenderDao
import com.tendertracker.data.local.entity.TenderEntity
import com.tendertracker.data.remote.api.TenderApi
import com.tendertracker.data.remote.dto.PipelineRequest
import com.tendertracker.data.remote.dto.ScoreSubmission
import com.tendertracker.domain.model.Tender
import com.tendertracker.domain.repository.TenderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
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

    override suspend fun submitScoreToBackend(tenderId: String, score: Int, explanation: String) {
        try {
            tenderApi.submitScore(
                tenderId,
                ScoreSubmission(
                    score = score,
                    explanation = explanation,
                    scoredAt = Instant.now().toString()
                )
            )
        } catch (e: Exception) {
            // Best-effort: log but don't fail
            android.util.Log.w("TenderRepo", "Failed to submit score to backend: ${e.message}")
        }
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
            // Snapshot local scores before overwrite
            val localScores = tenderDao.getScoredTenderIds()
                .associateBy({ it.id }, { it })

            val response = tenderApi.getTenders(page = 1, perPage = 100)
            if (response.isSuccessful) {
                val tenderList = response.body()!!
                val entities = tenderList.tenders.map { dto ->
                    val entity = dto.toEntity()
                    // Preserve local score if backend returns null
                    if (entity.score == null) {
                        localScores[entity.id]?.let { local ->
                            entity.copy(
                                score = local.score,
                                scoreExplanation = local.scoreExplanation
                            )
                        } ?: entity
                    } else {
                        entity
                    }
                }
                tenderDao.insertTenders(entities)
                Result.success(entities.size)
            } else {
                val message = parseApiError(response.errorBody()?.string())
                    ?: "Failed to fetch tenders (${response.code()})"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Run the full pipeline (discover → download → extract → score), then sync.
     * This ensures tenders have scores when synced to the app.
     */
    override suspend fun runPipelineAndSync(
        sourceId: String,
        onProgress: (Int) -> Unit
    ): Result<Int> {
        return try {
            // Step 1: Trigger the FULL pipeline (includes scoring)
            onProgress(5)
            val pipelineResponse = tenderApi.runFullPipeline(PipelineRequest(sourceId))

            if (!pipelineResponse.isSuccessful) {
                val message = parseApiError(pipelineResponse.errorBody()?.string())
                    ?: "Server unavailable. Please try again later. (${pipelineResponse.code()})"
                return Result.failure(Exception(message))
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

    private fun parseApiError(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val json = org.json.JSONObject(body)
            json.optString("message").takeIf { it.isNotEmpty() }
                ?: json.optString("detail").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
}
