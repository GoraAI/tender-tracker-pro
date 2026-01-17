package com.tendertracker.data.repository

import com.tendertracker.data.remote.api.TenderApi
import com.tendertracker.data.remote.dto.CompanyProfileRequest
import com.tendertracker.data.remote.dto.ComplianceItemRequest
import com.tendertracker.data.remote.dto.CostItemRequest
import com.tendertracker.data.remote.dto.PipelineTenderCreateRequest
import com.tendertracker.data.remote.dto.PipelineTenderUpdateRequest
import com.tendertracker.data.remote.dto.TenderOutcomeRequest
import com.tendertracker.domain.model.CompanyProfile
import com.tendertracker.domain.model.ComplianceItem
import com.tendertracker.domain.model.ComplianceStatus
import com.tendertracker.domain.model.CostAnalytics
import com.tendertracker.domain.model.CostCategory
import com.tendertracker.domain.model.CostItem
import com.tendertracker.domain.model.FunnelAnalytics
import com.tendertracker.domain.model.FunnelStage
import com.tendertracker.domain.model.OutcomeAnalytics
import com.tendertracker.domain.model.PaymentDelayAnalytics
import com.tendertracker.domain.model.PaymentDelayData
import com.tendertracker.domain.model.PipelineStatus
import com.tendertracker.domain.model.PipelineTender
import com.tendertracker.domain.model.TenderOutcome
import com.tendertracker.domain.repository.AnalyticsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AnalyticsRepository.
 *
 * NO MOCK DATA - all data comes from the backend API.
 * Returns empty state for new users who haven't entered any data.
 */
@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val api: TenderApi
) : AnalyticsRepository {

    // ==========================================================================
    // COMPANY PROFILE
    // ==========================================================================

    override suspend fun getCompanyProfile(): Result<CompanyProfile?> {
        return try {
            val response = api.getCompanyProfile()
            if (response.isSuccessful) {
                val dto = response.body()
                Result.success(dto?.toDomain())
            } else {
                Result.failure(Exception("Failed to get company profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createCompanyProfile(
        legalName: String,
        industry: String,
        tradingName: String?,
        capabilities: List<String>?,
        preferredSectors: List<String>?,
        preferredProvinces: List<String>?,
        keywords: List<String>?,
        valueRangeMin: Double?,
        valueRangeMax: Double?
    ): Result<CompanyProfile> {
        return try {
            val request = CompanyProfileRequest(
                legalName = legalName,
                industry = industry,
                tradingName = tradingName,
                capabilities = capabilities,
                preferredSectors = preferredSectors,
                preferredProvinces = preferredProvinces,
                keywords = keywords,
                valueRangeMin = valueRangeMin,
                valueRangeMax = valueRangeMax
            )
            val response = api.createCompanyProfile(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to create profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCompanyProfile(
        legalName: String,
        industry: String,
        tradingName: String?,
        capabilities: List<String>?,
        preferredSectors: List<String>?,
        preferredProvinces: List<String>?,
        keywords: List<String>?,
        valueRangeMin: Double?,
        valueRangeMax: Double?
    ): Result<CompanyProfile> {
        return try {
            val request = CompanyProfileRequest(
                legalName = legalName,
                industry = industry,
                tradingName = tradingName,
                capabilities = capabilities,
                preferredSectors = preferredSectors,
                preferredProvinces = preferredProvinces,
                keywords = keywords,
                valueRangeMin = valueRangeMin,
                valueRangeMax = valueRangeMax
            )
            val response = api.updateCompanyProfile(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to update profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================================
    // COMPLIANCE
    // ==========================================================================

    override suspend fun getComplianceStatus(): Result<ComplianceStatus> {
        return try {
            val response = api.getComplianceStatus()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to get compliance status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addComplianceItem(
        type: String,
        name: String,
        shortName: String,
        registrationNumber: String,
        issueDate: String,
        expiryDate: String?,
        expiryType: String,
        renewalUrl: String?
    ): Result<ComplianceItem> {
        return try {
            val request = ComplianceItemRequest(
                type = type,
                name = name,
                shortName = shortName,
                registrationNumber = registrationNumber,
                issueDate = issueDate,
                expiryDate = expiryDate,
                expiryType = expiryType,
                renewalUrl = renewalUrl
            )
            val response = api.addComplianceItem(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to add compliance item: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateComplianceItem(
        itemId: String,
        type: String,
        name: String,
        shortName: String,
        registrationNumber: String,
        issueDate: String,
        expiryDate: String?,
        expiryType: String,
        renewalUrl: String?
    ): Result<ComplianceItem> {
        return try {
            val request = ComplianceItemRequest(
                type = type,
                name = name,
                shortName = shortName,
                registrationNumber = registrationNumber,
                issueDate = issueDate,
                expiryDate = expiryDate,
                expiryType = expiryType,
                renewalUrl = renewalUrl
            )
            val response = api.updateComplianceItem(itemId, request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to update compliance item: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteComplianceItem(itemId: String): Result<Unit> {
        return try {
            val response = api.deleteComplianceItem(itemId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete compliance item: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================================
    // COSTS
    // ==========================================================================

    override suspend fun getCostAnalytics(periodMonths: Int): Result<CostAnalytics> {
        return try {
            val response = api.getCostAnalytics(periodMonths)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to get cost analytics: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCostItem(
        category: String,
        description: String,
        amount: Double,
        date: String,
        tenderId: String?,
        recurring: Boolean,
        frequency: String?
    ): Result<CostItem> {
        return try {
            val request = CostItemRequest(
                category = category,
                description = description,
                amount = amount,
                date = date,
                tenderId = tenderId,
                recurring = recurring,
                frequency = frequency
            )
            val response = api.addCostItem(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to add cost item: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCostItem(costId: String): Result<Unit> {
        return try {
            val response = api.deleteCostItem(costId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete cost item: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================================
    // FUNNEL
    // ==========================================================================

    override suspend fun getFunnelAnalytics(periodMonths: Int): Result<FunnelAnalytics> {
        return try {
            val response = api.getFunnelAnalytics(periodMonths)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to get funnel analytics: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================================
    // PAYMENT DELAYS
    // ==========================================================================

    override suspend fun getPaymentDelayAnalytics(): Result<PaymentDelayAnalytics> {
        return try {
            val response = api.getPaymentDelayAnalytics()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to get payment delay analytics: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================================
    // PIPELINE
    // ==========================================================================

    override suspend fun getPipelineStatus(): Result<PipelineStatus> {
        return try {
            val response = api.getPipelineStatus()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to get pipeline status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addToPipeline(
        tenderId: String,
        status: String,
        assignedTo: String?,
        notes: String?
    ): Result<PipelineTender> {
        return try {
            val request = PipelineTenderCreateRequest(
                tenderId = tenderId,
                status = status,
                assignedTo = assignedTo,
                notes = notes
            )
            val response = api.addToPipeline(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to add to pipeline: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePipelineStatus(
        pipelineId: String,
        status: String?,
        assignedTo: String?,
        notes: String?,
        phases: List<Map<String, Any>>?,
        hasConflict: Boolean?
    ): Result<PipelineTender> {
        return try {
            val request = PipelineTenderUpdateRequest(
                status = status,
                assignedTo = assignedTo,
                notes = notes,
                phases = phases,
                hasConflict = hasConflict
            )
            val response = api.updatePipelineStatus(pipelineId, request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to update pipeline status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFromPipeline(pipelineId: String): Result<Unit> {
        return try {
            val response = api.removeFromPipeline(pipelineId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove from pipeline: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================================
    // OUTCOMES
    // ==========================================================================

    override suspend fun getOutcomeAnalytics(periodMonths: Int): Result<OutcomeAnalytics> {
        return try {
            val response = api.getOutcomeAnalytics(periodMonths)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to get outcome analytics: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordOutcome(
        tenderId: String,
        outcome: String,
        awardDate: String,
        reasonCode: String?,
        reasonDetails: String?,
        awardAmount: Double?,
        paymentReceivedDate: String?
    ): Result<TenderOutcome> {
        return try {
            val request = TenderOutcomeRequest(
                tenderId = tenderId,
                outcome = outcome,
                awardDate = awardDate,
                reasonCode = reasonCode,
                reasonDetails = reasonDetails,
                awardAmount = awardAmount,
                paymentReceivedDate = paymentReceivedDate
            )
            val response = api.recordOutcome(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to record outcome: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// =============================================================================
// DTO TO DOMAIN CONVERSION EXTENSIONS
// =============================================================================

private fun com.tendertracker.data.remote.dto.CompanyProfileResponse.toDomain() = CompanyProfile(
    id = id,
    userId = userId,
    legalName = legalName,
    tradingName = tradingName,
    industry = industry,
    capabilities = capabilities ?: emptyList(),
    preferredSectors = preferredSectors ?: emptyList(),
    preferredProvinces = preferredProvinces ?: emptyList(),
    keywords = keywords ?: emptyList(),
    valueRangeMin = valueRangeMin,
    valueRangeMax = valueRangeMax
)

private fun com.tendertracker.data.remote.dto.ComplianceStatusResponse.toDomain() = ComplianceStatus(
    items = items.map { it.toDomain() },
    validCount = validCount,
    totalCount = totalCount,
    daysToNextExpiry = daysToNextExpiry,
    nextExpiryItem = nextExpiryItem,
    isEmpty = isEmpty
)

private fun com.tendertracker.data.remote.dto.ComplianceItemResponse.toDomain() = ComplianceItem(
    id = id,
    userId = userId,
    type = type,
    name = name,
    shortName = shortName,
    registrationNumber = registrationNumber,
    issueDate = issueDate,
    expiryDate = expiryDate,
    expiryType = expiryType,
    renewalUrl = renewalUrl,
    daysRemaining = daysRemaining,
    isExpired = isExpired,
    isValid = isValid
)

private fun com.tendertracker.data.remote.dto.CostAnalyticsResponse.toDomain() = CostAnalytics(
    categories = categories.map { it.toDomain() },
    totalSpent = totalSpent,
    tendersWon = tendersWon,
    costPerWin = costPerWin,
    periodStart = periodStart,
    periodEnd = periodEnd,
    isEmpty = isEmpty
)

private fun com.tendertracker.data.remote.dto.CostCategoryResponse.toDomain() = CostCategory(
    name = name,
    amount = amount,
    percentage = percentage,
    items = items.map { it.toDomain() }
)

private fun com.tendertracker.data.remote.dto.CostItemResponse.toDomain() = CostItem(
    id = id,
    userId = userId,
    tenderId = tenderId,
    category = category,
    description = description,
    amount = amount,
    date = date,
    recurring = recurring,
    frequency = frequency
)

private fun com.tendertracker.data.remote.dto.FunnelAnalyticsResponse.toDomain() = FunnelAnalytics(
    stages = stages.map { it.toDomain() },
    winRate = winRate,
    costPerWin = costPerWin,
    avgDaysToAward = avgDaysToAward,
    periodStart = periodStart,
    periodEnd = periodEnd,
    isEmpty = isEmpty
)

private fun com.tendertracker.data.remote.dto.FunnelStageResponse.toDomain() = FunnelStage(
    name = name,
    count = count,
    dropOffCount = dropOffCount,
    dropOffReason = dropOffReason
)

private fun com.tendertracker.data.remote.dto.PaymentDelayAnalyticsResponse.toDomain() = PaymentDelayAnalytics(
    byProvince = byProvince.map { it.toDomain() },
    byDepartment = byDepartment.map { it.toDomain() },
    overallAverage = overallAverage,
    userDataAvailable = userDataAvailable,
    referenceDataSource = referenceDataSource
)

private fun com.tendertracker.data.remote.dto.PaymentDelayDataResponse.toDomain() = PaymentDelayData(
    id = id,
    name = name,
    shortName = shortName,
    avgDaysToPayment = avgDaysToPayment,
    sampleSize = sampleSize,
    trend = trend,
    source = source
)

private fun com.tendertracker.data.remote.dto.PipelineStatusResponse.toDomain() = PipelineStatus(
    tenders = tenders.map { it.toDomain() },
    activeCount = activeCount,
    dueThisWeek = dueThisWeek,
    hasConflicts = hasConflicts,
    isEmpty = isEmpty
)

private fun com.tendertracker.data.remote.dto.PipelineTenderResponse.toDomain() = PipelineTender(
    id = id,
    userId = userId,
    tenderId = tenderId,
    title = title,
    reference = reference,
    deadline = deadline,
    status = status,
    startDate = startDate,
    phases = phases ?: emptyList(),
    assignedTo = assignedTo,
    notes = notes,
    hasConflict = hasConflict
)

private fun com.tendertracker.data.remote.dto.OutcomeAnalyticsResponse.toDomain() = OutcomeAnalytics(
    totalSubmitted = totalSubmitted,
    totalWon = totalWon,
    totalLost = totalLost,
    totalWithdrawn = totalWithdrawn,
    totalCancelled = totalCancelled,
    winRate = winRate,
    totalAwardAmount = totalAwardAmount,
    avgDaysToPayment = avgDaysToPayment,
    outcomesByReason = outcomesByReason,
    periodStart = periodStart,
    periodEnd = periodEnd,
    isEmpty = isEmpty
)

private fun com.tendertracker.data.remote.dto.TenderOutcomeResponse.toDomain() = TenderOutcome(
    id = id,
    userId = userId,
    tenderId = tenderId,
    outcome = outcome,
    reasonCode = reasonCode,
    reasonDetails = reasonDetails,
    awardDate = awardDate,
    awardAmount = awardAmount,
    paymentReceivedDate = paymentReceivedDate,
    daysToPayment = daysToPayment
)
