package com.tendertracker.domain.model

/**
 * Domain models for analytics data.
 * These are used throughout the app to represent analytics data
 * retrieved from the backend API.
 *
 * NO MOCK DATA - all data comes from user input via API.
 */

// =============================================================================
// COMPANY PROFILE
// =============================================================================

data class CompanyProfile(
    val id: String,
    val userId: String,
    val legalName: String,
    val tradingName: String?,
    val industry: String,
    val capabilities: List<String>,
    val preferredSectors: List<String>,
    val preferredProvinces: List<String>,
    val keywords: List<String>,
    val valueRangeMin: Double?,
    val valueRangeMax: Double?
)

// =============================================================================
// COMPLIANCE
// =============================================================================

data class ComplianceItem(
    val id: String,
    val userId: String,
    val type: String,
    val name: String,
    val shortName: String,
    val registrationNumber: String,
    val issueDate: String,
    val expiryDate: String?,
    val expiryType: String,
    val renewalUrl: String?,
    val daysRemaining: Int?,
    val isExpired: Boolean,
    val isValid: Boolean
)

data class ComplianceStatus(
    val items: List<ComplianceItem>,
    val validCount: Int,
    val totalCount: Int,
    val daysToNextExpiry: Int?,
    val nextExpiryItem: String?,
    val isEmpty: Boolean
)

// =============================================================================
// COSTS
// =============================================================================

data class CostItem(
    val id: String,
    val userId: String,
    val tenderId: String?,
    val category: String,
    val description: String,
    val amount: Double,
    val date: String,
    val recurring: Boolean,
    val frequency: String?
)

data class CostCategory(
    val name: String,
    val amount: Double,
    val percentage: Double,
    val items: List<CostItem>
)

data class CostAnalytics(
    val categories: List<CostCategory>,
    val totalSpent: Double,
    val tendersWon: Int,
    val costPerWin: Double,
    val periodStart: String,
    val periodEnd: String,
    val isEmpty: Boolean
)

// =============================================================================
// FUNNEL
// =============================================================================

data class FunnelStage(
    val name: String,
    val count: Int,
    val dropOffCount: Int,
    val dropOffReason: String
)

data class FunnelAnalytics(
    val stages: List<FunnelStage>,
    val winRate: Double,
    val costPerWin: Double,
    val avgDaysToAward: Int,
    val periodStart: String,
    val periodEnd: String,
    val isEmpty: Boolean
)

// =============================================================================
// PAYMENT DELAYS
// =============================================================================

data class PaymentDelayData(
    val id: String,
    val name: String,
    val shortName: String,
    val avgDaysToPayment: Int,
    val sampleSize: Int,
    val trend: Double,
    val source: String
)

data class PaymentDelayAnalytics(
    val byProvince: List<PaymentDelayData>,
    val byDepartment: List<PaymentDelayData>,
    val overallAverage: Int,
    val userDataAvailable: Boolean,
    val referenceDataSource: String
)

// =============================================================================
// PIPELINE
// =============================================================================

data class PipelineTender(
    val id: String,
    val userId: String,
    val tenderId: String,
    val title: String?,
    val reference: String?,
    val deadline: String?,
    val status: String,
    val startDate: String?,
    val phases: List<Map<String, Any>>,
    val assignedTo: String?,
    val notes: String?,
    val hasConflict: Boolean
)

data class PipelineStatus(
    val tenders: List<PipelineTender>,
    val activeCount: Int,
    val dueThisWeek: Int,
    val hasConflicts: Boolean,
    val isEmpty: Boolean
)

// =============================================================================
// OUTCOMES
// =============================================================================

data class TenderOutcome(
    val id: String,
    val userId: String,
    val tenderId: String,
    val outcome: String,
    val reasonCode: String?,
    val reasonDetails: String?,
    val awardDate: String,
    val awardAmount: Double?,
    val paymentReceivedDate: String?,
    val daysToPayment: Int?
)

data class OutcomeAnalytics(
    val totalSubmitted: Int,
    val totalWon: Int,
    val totalLost: Int,
    val totalWithdrawn: Int,
    val totalCancelled: Int,
    val winRate: Double,
    val totalAwardAmount: Double,
    val avgDaysToPayment: Int?,
    val outcomesByReason: Map<String, Int>,
    val periodStart: String,
    val periodEnd: String,
    val isEmpty: Boolean
)
