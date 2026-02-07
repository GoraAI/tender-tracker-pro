package com.tendertracker.data.repository

import com.tendertracker.data.remote.api.TenderApi
import com.tendertracker.data.remote.dto.CriteriaUpdateRequest
import com.tendertracker.data.remote.dto.ScoringWeightsDto
import com.tendertracker.data.remote.dto.ValueRangeDto
import com.tendertracker.data.remote.dto.toCreateRequest
import com.tendertracker.data.remote.dto.toDomain
import com.tendertracker.domain.model.UserCriteria
import com.tendertracker.domain.repository.CriteriaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CriteriaRepositoryImpl @Inject constructor(
    private val api: TenderApi
) : CriteriaRepository {

    override suspend fun getCriteria(): Result<List<UserCriteria>> {
        return try {
            val response = api.getCriteria()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.failure(Exception("Failed to fetch criteria: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    override suspend fun getCriteriaById(id: String): Result<UserCriteria> {
        return try {
            val response = api.getCriteriaById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to fetch criteria: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    override suspend fun createCriteria(criteria: UserCriteria): Result<UserCriteria> {
        return try {
            val request = criteria.toCreateRequest()
            val response = api.createCriteria(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to create criteria: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    override suspend fun updateCriteria(criteria: UserCriteria): Result<UserCriteria> {
        return try {
            val request = CriteriaUpdateRequest(
                name = criteria.name,
                capabilities = criteria.capabilities,
                preferredSectors = criteria.preferredSectors,
                preferredLocations = criteria.preferredLocations,
                valueRange = ValueRangeDto(
                    min = criteria.valueRange.min,
                    max = criteria.valueRange.max
                ),
                keywords = criteria.keywords,
                weights = ScoringWeightsDto(
                    capabilityMatch = criteria.weights.capabilityMatch,
                    sectorMatch = criteria.weights.sectorMatch,
                    locationMatch = criteria.weights.locationMatch,
                    valueMatch = criteria.weights.valueMatch,
                    keywordMatch = criteria.weights.keywordMatch
                ),
                isDefault = criteria.isDefault
            )
            val response = api.updateCriteria(criteria.id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to update criteria: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    override suspend fun deleteCriteria(id: String): Result<Unit> {
        return try {
            val response = api.deleteCriteria(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete criteria: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    override suspend fun getDefaultCriteria(): Result<UserCriteria?> {
        return getCriteria().fold(
            onSuccess = { criteriaList ->
                val defaultCriteria = criteriaList.firstOrNull { it.isDefault }
                Result.success(defaultCriteria)
            },
            onFailure = { exception ->
                Result.failure(exception)
            }
        )
    }
}
