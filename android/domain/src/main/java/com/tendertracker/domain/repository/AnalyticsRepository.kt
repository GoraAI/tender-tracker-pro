package com.tendertracker.domain.repository

import com.tendertracker.domain.model.CompanyProfile
import com.tendertracker.domain.model.ComplianceItem
import com.tendertracker.domain.model.ComplianceStatus
import com.tendertracker.domain.model.CostAnalytics
import com.tendertracker.domain.model.CostItem
import com.tendertracker.domain.model.FunnelAnalytics
import com.tendertracker.domain.model.OutcomeAnalytics
import com.tendertracker.domain.model.PaymentDelayAnalytics
import com.tendertracker.domain.model.PipelineStatus
import com.tendertracker.domain.model.PipelineTender
import com.tendertracker.domain.model.TenderOutcome
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for analytics data.
 *
 * NO MOCK DATA - all data comes from the backend API.
 * Returns empty state for new users who haven't entered any data.
 */
interface AnalyticsRepository {

    // ==========================================================================
    // COMPANY PROFILE
    // ==========================================================================

    /**
     * Get user's company profile. Returns null if not set up.
     */
    suspend fun getCompanyProfile(): Result<CompanyProfile?>

    /**
     * Create user's company profile.
     */
    suspend fun createCompanyProfile(
        legalName: String,
        industry: String,
        tradingName: String? = null,
        capabilities: List<String>? = null,
        preferredSectors: List<String>? = null,
        preferredProvinces: List<String>? = null,
        keywords: List<String>? = null,
        valueRangeMin: Double? = null,
        valueRangeMax: Double? = null
    ): Result<CompanyProfile>

    /**
     * Update user's company profile.
     */
    suspend fun updateCompanyProfile(
        legalName: String,
        industry: String,
        tradingName: String? = null,
        capabilities: List<String>? = null,
        preferredSectors: List<String>? = null,
        preferredProvinces: List<String>? = null,
        keywords: List<String>? = null,
        valueRangeMin: Double? = null,
        valueRangeMax: Double? = null
    ): Result<CompanyProfile>

    // ==========================================================================
    // COMPLIANCE
    // ==========================================================================

    /**
     * Get user's compliance status with all registrations.
     * Returns empty list if user has no compliance registrations.
     */
    suspend fun getComplianceStatus(): Result<ComplianceStatus>

    /**
     * Add a compliance registration.
     */
    suspend fun addComplianceItem(
        type: String,
        name: String,
        shortName: String,
        registrationNumber: String,
        issueDate: String,
        expiryDate: String? = null,
        expiryType: String = "annual",
        renewalUrl: String? = null
    ): Result<ComplianceItem>

    /**
     * Update a compliance registration.
     */
    suspend fun updateComplianceItem(
        itemId: String,
        type: String,
        name: String,
        shortName: String,
        registrationNumber: String,
        issueDate: String,
        expiryDate: String? = null,
        expiryType: String = "annual",
        renewalUrl: String? = null
    ): Result<ComplianceItem>

    /**
     * Delete a compliance registration.
     */
    suspend fun deleteComplianceItem(itemId: String): Result<Unit>

    // ==========================================================================
    // COSTS
    // ==========================================================================

    /**
     * Get cost analytics aggregated by category.
     * Returns empty categories if user has no cost data.
     */
    suspend fun getCostAnalytics(periodMonths: Int = 12): Result<CostAnalytics>

    /**
     * Add a cost item.
     */
    suspend fun addCostItem(
        category: String,
        description: String,
        amount: Double,
        date: String,
        tenderId: String? = null,
        recurring: Boolean = false,
        frequency: String? = null
    ): Result<CostItem>

    /**
     * Delete a cost item.
     */
    suspend fun deleteCostItem(costId: String): Result<Unit>

    // ==========================================================================
    // FUNNEL
    // ==========================================================================

    /**
     * Get opportunity funnel analytics.
     * Returns zero counts if user has no pipeline/outcome data.
     */
    suspend fun getFunnelAnalytics(periodMonths: Int = 12): Result<FunnelAnalytics>

    // ==========================================================================
    // PAYMENT DELAYS
    // ==========================================================================

    /**
     * Get payment delay analytics.
     * Uses National Treasury reference data as baseline.
     * Supplements with user's actual payment experience over time.
     */
    suspend fun getPaymentDelayAnalytics(): Result<PaymentDelayAnalytics>

    // ==========================================================================
    // PIPELINE
    // ==========================================================================

    /**
     * Get user's tender pipeline status.
     * Returns empty list if user has no pipeline tenders.
     */
    suspend fun getPipelineStatus(): Result<PipelineStatus>

    /**
     * Add a tender to user's pipeline.
     */
    suspend fun addToPipeline(
        tenderId: String,
        status: String = "discovered",
        assignedTo: String? = null,
        notes: String? = null
    ): Result<PipelineTender>

    /**
     * Update a pipeline tender's status.
     */
    suspend fun updatePipelineStatus(
        pipelineId: String,
        status: String? = null,
        assignedTo: String? = null,
        notes: String? = null,
        phases: List<Map<String, Any>>? = null,
        hasConflict: Boolean? = null
    ): Result<PipelineTender>

    /**
     * Remove a tender from user's pipeline.
     */
    suspend fun removeFromPipeline(pipelineId: String): Result<Unit>

    // ==========================================================================
    // OUTCOMES
    // ==========================================================================

    /**
     * Get tender outcome analytics.
     * Returns zero counts if user has no outcome data.
     */
    suspend fun getOutcomeAnalytics(periodMonths: Int = 12): Result<OutcomeAnalytics>

    /**
     * Record a tender outcome (won, lost, withdrawn, cancelled).
     */
    suspend fun recordOutcome(
        tenderId: String,
        outcome: String,
        awardDate: String,
        reasonCode: String? = null,
        reasonDetails: String? = null,
        awardAmount: Double? = null,
        paymentReceivedDate: String? = null
    ): Result<TenderOutcome>
}
