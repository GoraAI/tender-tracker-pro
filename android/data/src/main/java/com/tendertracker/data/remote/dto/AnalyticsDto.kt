package com.tendertracker.data.remote.dto

import com.google.gson.annotations.SerializedName

// =============================================================================
// COMPANY PROFILE
// =============================================================================

data class CompanyProfileResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("legal_name") val legalName: String,
    @SerializedName("trading_name") val tradingName: String?,
    @SerializedName("industry") val industry: String,
    @SerializedName("capabilities") val capabilities: List<String>?,
    @SerializedName("preferred_sectors") val preferredSectors: List<String>?,
    @SerializedName("preferred_provinces") val preferredProvinces: List<String>?,
    @SerializedName("keywords") val keywords: List<String>?,
    @SerializedName("value_range_min") val valueRangeMin: Double?,
    @SerializedName("value_range_max") val valueRangeMax: Double?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class CompanyProfileRequest(
    @SerializedName("legal_name") val legalName: String,
    @SerializedName("trading_name") val tradingName: String? = null,
    @SerializedName("industry") val industry: String,
    @SerializedName("capabilities") val capabilities: List<String>? = null,
    @SerializedName("preferred_sectors") val preferredSectors: List<String>? = null,
    @SerializedName("preferred_provinces") val preferredProvinces: List<String>? = null,
    @SerializedName("keywords") val keywords: List<String>? = null,
    @SerializedName("value_range_min") val valueRangeMin: Double? = null,
    @SerializedName("value_range_max") val valueRangeMax: Double? = null
)

// =============================================================================
// COMPLIANCE
// =============================================================================

data class ComplianceStatusResponse(
    @SerializedName("items") val items: List<ComplianceItemResponse>,
    @SerializedName("valid_count") val validCount: Int,
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("days_to_next_expiry") val daysToNextExpiry: Int?,
    @SerializedName("next_expiry_item") val nextExpiryItem: String?,
    @SerializedName("is_empty") val isEmpty: Boolean
)

data class ComplianceItemResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String,
    @SerializedName("short_name") val shortName: String,
    @SerializedName("registration_number") val registrationNumber: String,
    @SerializedName("issue_date") val issueDate: String,
    @SerializedName("expiry_date") val expiryDate: String?,
    @SerializedName("expiry_type") val expiryType: String,
    @SerializedName("renewal_url") val renewalUrl: String?,
    @SerializedName("documents") val documents: List<Map<String, Any>>?,
    @SerializedName("days_remaining") val daysRemaining: Int?,
    @SerializedName("is_expired") val isExpired: Boolean,
    @SerializedName("is_valid") val isValid: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class ComplianceItemRequest(
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String,
    @SerializedName("short_name") val shortName: String,
    @SerializedName("registration_number") val registrationNumber: String,
    @SerializedName("issue_date") val issueDate: String,
    @SerializedName("expiry_date") val expiryDate: String? = null,
    @SerializedName("expiry_type") val expiryType: String = "annual",
    @SerializedName("renewal_url") val renewalUrl: String? = null,
    @SerializedName("documents") val documents: List<Map<String, Any>>? = null
)

// =============================================================================
// COSTS
// =============================================================================

data class CostAnalyticsResponse(
    @SerializedName("categories") val categories: List<CostCategoryResponse>,
    @SerializedName("total_spent") val totalSpent: Double,
    @SerializedName("tenders_won") val tendersWon: Int,
    @SerializedName("cost_per_win") val costPerWin: Double,
    @SerializedName("period_start") val periodStart: String,
    @SerializedName("period_end") val periodEnd: String,
    @SerializedName("is_empty") val isEmpty: Boolean
)

data class CostCategoryResponse(
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("percentage") val percentage: Double,
    @SerializedName("items") val items: List<CostItemResponse>
)

data class CostItemResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("tender_id") val tenderId: String?,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("date") val date: String,
    @SerializedName("recurring") val recurring: Boolean,
    @SerializedName("frequency") val frequency: String?,
    @SerializedName("created_at") val createdAt: String
)

data class CostItemRequest(
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("date") val date: String,
    @SerializedName("tender_id") val tenderId: String? = null,
    @SerializedName("recurring") val recurring: Boolean = false,
    @SerializedName("frequency") val frequency: String? = null
)

// =============================================================================
// FUNNEL
// =============================================================================

data class FunnelAnalyticsResponse(
    @SerializedName("stages") val stages: List<FunnelStageResponse>,
    @SerializedName("win_rate") val winRate: Double,
    @SerializedName("cost_per_win") val costPerWin: Double,
    @SerializedName("avg_days_to_award") val avgDaysToAward: Int,
    @SerializedName("period_start") val periodStart: String,
    @SerializedName("period_end") val periodEnd: String,
    @SerializedName("is_empty") val isEmpty: Boolean
)

data class FunnelStageResponse(
    @SerializedName("name") val name: String,
    @SerializedName("count") val count: Int,
    @SerializedName("drop_off_count") val dropOffCount: Int,
    @SerializedName("drop_off_reason") val dropOffReason: String
)

// =============================================================================
// PAYMENT DELAYS
// =============================================================================

data class PaymentDelayAnalyticsResponse(
    @SerializedName("by_province") val byProvince: List<PaymentDelayDataResponse>,
    @SerializedName("by_department") val byDepartment: List<PaymentDelayDataResponse>,
    @SerializedName("overall_average") val overallAverage: Int,
    @SerializedName("user_data_available") val userDataAvailable: Boolean,
    @SerializedName("reference_data_source") val referenceDataSource: String
)

data class PaymentDelayDataResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("short_name") val shortName: String,
    @SerializedName("avg_days_to_payment") val avgDaysToPayment: Int,
    @SerializedName("sample_size") val sampleSize: Int,
    @SerializedName("trend") val trend: Double,
    @SerializedName("source") val source: String
)

// =============================================================================
// PIPELINE
// =============================================================================

data class PipelineStatusResponse(
    @SerializedName("tenders") val tenders: List<PipelineTenderResponse>,
    @SerializedName("active_count") val activeCount: Int,
    @SerializedName("due_this_week") val dueThisWeek: Int,
    @SerializedName("has_conflicts") val hasConflicts: Boolean,
    @SerializedName("is_empty") val isEmpty: Boolean
)

data class PipelineTenderResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("tender_id") val tenderId: String,
    @SerializedName("title") val title: String?,
    @SerializedName("reference") val reference: String?,
    @SerializedName("deadline") val deadline: String?,
    @SerializedName("status") val status: String,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("phases") val phases: List<Map<String, Any>>?,
    @SerializedName("assigned_to") val assignedTo: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("has_conflict") val hasConflict: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class PipelineTenderCreateRequest(
    @SerializedName("tender_id") val tenderId: String,
    @SerializedName("status") val status: String = "discovered",
    @SerializedName("assigned_to") val assignedTo: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class PipelineTenderUpdateRequest(
    @SerializedName("status") val status: String? = null,
    @SerializedName("assigned_to") val assignedTo: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("phases") val phases: List<Map<String, Any>>? = null,
    @SerializedName("has_conflict") val hasConflict: Boolean? = null
)

// =============================================================================
// OUTCOMES
// =============================================================================

data class OutcomeAnalyticsResponse(
    @SerializedName("total_submitted") val totalSubmitted: Int,
    @SerializedName("total_won") val totalWon: Int,
    @SerializedName("total_lost") val totalLost: Int,
    @SerializedName("total_withdrawn") val totalWithdrawn: Int,
    @SerializedName("total_cancelled") val totalCancelled: Int,
    @SerializedName("win_rate") val winRate: Double,
    @SerializedName("total_award_amount") val totalAwardAmount: Double,
    @SerializedName("avg_days_to_payment") val avgDaysToPayment: Int?,
    @SerializedName("outcomes_by_reason") val outcomesByReason: Map<String, Int>,
    @SerializedName("period_start") val periodStart: String,
    @SerializedName("period_end") val periodEnd: String,
    @SerializedName("is_empty") val isEmpty: Boolean
)

data class TenderOutcomeResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("tender_id") val tenderId: String,
    @SerializedName("outcome") val outcome: String,
    @SerializedName("reason_code") val reasonCode: String?,
    @SerializedName("reason_details") val reasonDetails: String?,
    @SerializedName("award_date") val awardDate: String,
    @SerializedName("award_amount") val awardAmount: Double?,
    @SerializedName("payment_received_date") val paymentReceivedDate: String?,
    @SerializedName("days_to_payment") val daysToPayment: Int?,
    @SerializedName("created_at") val createdAt: String
)

data class TenderOutcomeRequest(
    @SerializedName("tender_id") val tenderId: String,
    @SerializedName("outcome") val outcome: String,
    @SerializedName("reason_code") val reasonCode: String? = null,
    @SerializedName("reason_details") val reasonDetails: String? = null,
    @SerializedName("award_date") val awardDate: String,
    @SerializedName("award_amount") val awardAmount: Double? = null,
    @SerializedName("payment_received_date") val paymentReceivedDate: String? = null
)
