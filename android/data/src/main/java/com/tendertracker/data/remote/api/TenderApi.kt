package com.tendertracker.data.remote.api

import com.tendertracker.data.remote.dto.CompanyProfileRequest
import com.tendertracker.data.remote.dto.CompanyProfileResponse
import com.tendertracker.data.remote.dto.ComplianceItemRequest
import com.tendertracker.data.remote.dto.ComplianceItemResponse
import com.tendertracker.data.remote.dto.CriteriaCreateRequest
import com.tendertracker.data.remote.dto.CriteriaResponse
import com.tendertracker.data.remote.dto.CriteriaUpdateRequest
import com.tendertracker.data.remote.dto.ComplianceStatusResponse
import com.tendertracker.data.remote.dto.CostAnalyticsResponse
import com.tendertracker.data.remote.dto.CostItemRequest
import com.tendertracker.data.remote.dto.CostItemResponse
import com.tendertracker.data.remote.dto.FunnelAnalyticsResponse
import com.tendertracker.data.remote.dto.LoginRequest
import com.tendertracker.data.remote.dto.OutcomeAnalyticsResponse
import com.tendertracker.data.remote.dto.PaymentDelayAnalyticsResponse
import com.tendertracker.data.remote.dto.PipelineJobResponse
import com.tendertracker.data.remote.dto.PipelineRequest
import com.tendertracker.data.remote.dto.PipelineStatusResponse
import com.tendertracker.data.remote.dto.PipelineTenderCreateRequest
import com.tendertracker.data.remote.dto.PipelineTenderResponse
import com.tendertracker.data.remote.dto.PipelineTenderUpdateRequest
import com.tendertracker.data.remote.dto.RegisterRequest
import com.tendertracker.data.remote.dto.ScoreResponse
import com.tendertracker.data.remote.dto.ScoreSubmission
import com.tendertracker.data.remote.dto.TenderListResponse
import com.tendertracker.data.remote.dto.TenderOutcomeRequest
import com.tendertracker.data.remote.dto.TenderOutcomeResponse
import com.tendertracker.data.remote.dto.TenderResponse
import com.tendertracker.data.remote.dto.TokenResponse
import com.tendertracker.data.remote.dto.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TenderApi {
    @GET("tenders")
    suspend fun getTenders(
        @Query("since") since: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): Response<TenderListResponse>

    @GET("tenders/{id}")
    suspend fun getTender(
        @Path("id") id: String
    ): Response<TenderResponse>

    @POST("tenders/{id}/score")
    suspend fun submitScore(
        @Path("id") tenderId: String,
        @Body score: ScoreSubmission
    ): Response<ScoreResponse>

    @POST("auth/login")
    suspend fun login(
        @Body credentials: LoginRequest
    ): Response<TokenResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<TokenResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(): Response<TokenResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    // Pipeline endpoints
    @POST("pipeline/discover")
    suspend fun runDiscoverPipeline(
        @Body request: PipelineRequest
    ): Response<PipelineJobResponse>

    @POST("pipeline/full")
    suspend fun runFullPipeline(
        @Body request: PipelineRequest
    ): Response<PipelineJobResponse>

    @GET("pipeline/jobs/{jobId}")
    suspend fun getPipelineJob(
        @Path("jobId") jobId: String
    ): Response<PipelineJobResponse>

    // ==========================================================================
    // ANALYTICS ENDPOINTS - NO MOCK DATA
    // ==========================================================================

    // Company Profile
    @GET("analytics/profile")
    suspend fun getCompanyProfile(): Response<CompanyProfileResponse?>

    @POST("analytics/profile")
    suspend fun createCompanyProfile(
        @Body request: CompanyProfileRequest
    ): Response<CompanyProfileResponse>

    @PUT("analytics/profile")
    suspend fun updateCompanyProfile(
        @Body request: CompanyProfileRequest
    ): Response<CompanyProfileResponse>

    // Compliance
    @GET("analytics/compliance")
    suspend fun getComplianceStatus(): Response<ComplianceStatusResponse>

    @POST("analytics/compliance")
    suspend fun addComplianceItem(
        @Body request: ComplianceItemRequest
    ): Response<ComplianceItemResponse>

    @PUT("analytics/compliance/{itemId}")
    suspend fun updateComplianceItem(
        @Path("itemId") itemId: String,
        @Body request: ComplianceItemRequest
    ): Response<ComplianceItemResponse>

    @DELETE("analytics/compliance/{itemId}")
    suspend fun deleteComplianceItem(
        @Path("itemId") itemId: String
    ): Response<Unit>

    // Costs
    @GET("analytics/costs")
    suspend fun getCostAnalytics(
        @Query("period_months") periodMonths: Int = 12
    ): Response<CostAnalyticsResponse>

    @POST("analytics/costs")
    suspend fun addCostItem(
        @Body request: CostItemRequest
    ): Response<CostItemResponse>

    @DELETE("analytics/costs/{costId}")
    suspend fun deleteCostItem(
        @Path("costId") costId: String
    ): Response<Unit>

    // Funnel
    @GET("analytics/funnel")
    suspend fun getFunnelAnalytics(
        @Query("period_months") periodMonths: Int = 12
    ): Response<FunnelAnalyticsResponse>

    // Payment Delays
    @GET("analytics/payment-delays")
    suspend fun getPaymentDelayAnalytics(): Response<PaymentDelayAnalyticsResponse>

    // Pipeline (User's tender pipeline)
    @GET("analytics/pipeline")
    suspend fun getPipelineStatus(): Response<PipelineStatusResponse>

    @POST("analytics/pipeline")
    suspend fun addToPipeline(
        @Body request: PipelineTenderCreateRequest
    ): Response<PipelineTenderResponse>

    @PATCH("analytics/pipeline/{pipelineId}")
    suspend fun updatePipelineStatus(
        @Path("pipelineId") pipelineId: String,
        @Body request: PipelineTenderUpdateRequest
    ): Response<PipelineTenderResponse>

    @DELETE("analytics/pipeline/{pipelineId}")
    suspend fun removeFromPipeline(
        @Path("pipelineId") pipelineId: String
    ): Response<Unit>

    // Outcomes
    @GET("analytics/outcomes")
    suspend fun getOutcomeAnalytics(
        @Query("period_months") periodMonths: Int = 12
    ): Response<OutcomeAnalyticsResponse>

    @POST("analytics/outcomes")
    suspend fun recordOutcome(
        @Body request: TenderOutcomeRequest
    ): Response<TenderOutcomeResponse>

    // ==========================================================================
    // CRITERIA ENDPOINTS - Company Profile / Scoring Criteria
    // ==========================================================================

    @GET("config/criteria")
    suspend fun getCriteria(): Response<List<CriteriaResponse>>

    @GET("config/criteria/{criteriaId}")
    suspend fun getCriteriaById(
        @Path("criteriaId") criteriaId: String
    ): Response<CriteriaResponse>

    @POST("config/criteria")
    suspend fun createCriteria(
        @Body request: CriteriaCreateRequest
    ): Response<CriteriaResponse>

    @PUT("config/criteria/{criteriaId}")
    suspend fun updateCriteria(
        @Path("criteriaId") criteriaId: String,
        @Body request: CriteriaUpdateRequest
    ): Response<CriteriaResponse>

    @DELETE("config/criteria/{criteriaId}")
    suspend fun deleteCriteria(
        @Path("criteriaId") criteriaId: String
    ): Response<Unit>
}
