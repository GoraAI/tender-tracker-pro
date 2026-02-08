package com.tendertracker.data.repository

import com.tendertracker.data.remote.api.TenderApi
import com.tendertracker.data.remote.dto.*
import com.tendertracker.domain.model.QualificationResult
import com.tendertracker.domain.model.TenderApplication
import com.tendertracker.domain.repository.ApplicationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationRepositoryImpl @Inject constructor(
    private val api: TenderApi
) : ApplicationRepository {

    override suspend fun checkQualification(tenderId: String): Result<QualificationResult> {
        return try {
            val response = api.checkQualification(tenderId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to check qualification: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    override suspend fun createApplication(tenderId: String): Result<TenderApplication> {
        return try {
            val request = ApplicationCreateRequest(tenderId)
            val response = api.createApplication(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to create application: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    override suspend fun getApplication(applicationId: String): Result<TenderApplication> {
        return try {
            val response = api.getApplication(applicationId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to get application: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    override suspend fun updateFields(
        applicationId: String,
        fieldUpdates: Map<String, Any>
    ): Result<TenderApplication> {
        return try {
            val request = ApplicationUpdateRequest(fieldUpdates)
            val response = api.updateApplicationFields(applicationId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to update fields: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    override suspend fun updatePricing(
        applicationId: String,
        pricingData: Map<String, Any>
    ): Result<TenderApplication> {
        return try {
            val request = PricingUpdateRequest(pricingData)
            val response = api.updateApplicationPricing(applicationId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to update pricing: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    override suspend fun updateTodoStatus(
        applicationId: String,
        todoId: String,
        completed: Boolean
    ): Result<TenderApplication> {
        return try {
            val request = TodoUpdateRequest(todoId, completed)
            val response = api.updateTodoStatus(applicationId, todoId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to update todo: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    override suspend fun submitApplication(applicationId: String): Result<TenderApplication> {
        return try {
            val response = api.submitApplication(applicationId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Failed to submit application: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    override suspend fun deleteApplication(applicationId: String): Result<Unit> {
        return try {
            val response = api.deleteApplication(applicationId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete application: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
}
