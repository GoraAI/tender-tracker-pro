package com.tendertracker.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.tendertracker.data.local.entity.TenderEntity

data class TenderListResponse(
    @SerializedName("items") val tenders: List<TenderDto>,
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("per_page") val perPage: Int = 20,
    @SerializedName("pages") val pages: Int = 0
) {
    val hasMore: Boolean get() = page < pages
}

data class TenderResponse(
    @SerializedName("tender") val tender: TenderDto
)

data class TenderDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String?,
    @SerializedName("reference") val reference: String?,
    @SerializedName("source_id") val sourceId: String,
    @SerializedName("deadline") val deadline: String?,
    @SerializedName("estimated_value") val estimatedValue: String?, // API returns as string
    @SerializedName("requirements_summary") val requirementsSummary: String? = null,
    @SerializedName("score") val score: Int? = null,
    @SerializedName("score_explanation") val scoreExplanation: String? = null,
    @SerializedName("relevance_category") val relevanceCategory: String? = null,
    @SerializedName("pdf_url") val pdfUrl: String? = null,
    @SerializedName("discovered_at") val discoveredAt: String? = null
) {
    fun toEntity(): TenderEntity = TenderEntity(
        id = id,
        title = title ?: "Untitled Tender",
        reference = reference ?: "",
        sourceId = sourceId,
        deadline = deadline,
        estimatedValue = estimatedValue?.toDoubleOrNull(),
        requirementsSummary = requirementsSummary,
        score = score,
        scoreExplanation = scoreExplanation,
        pdfUrl = pdfUrl ?: "",
        localPdfPath = null,
        extractedText = null,
        createdAt = parseTimestamp(discoveredAt),
        syncedAt = System.currentTimeMillis()
    )

    private fun parseTimestamp(timestamp: String?): Long {
        if (timestamp == null) return System.currentTimeMillis()
        return try {
            java.time.Instant.parse(timestamp).toEpochMilli()
        } catch (e: Exception) {
            try {
                // Try parsing as local datetime
                java.time.LocalDateTime.parse(timestamp)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}

data class ScoreSubmission(
    @SerializedName("score") val score: Int,
    @SerializedName("explanation") val explanation: String,
    @SerializedName("scored_at") val scoredAt: String
)

data class ScoreResponse(
    @SerializedName("tender_id") val tenderId: String,
    @SerializedName("score") val score: Int,
    @SerializedName("explanation") val explanation: String,
    @SerializedName("synced_at") val syncedAt: String
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    @SerializedName("expires_in") val expiresIn: Long? = null
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("company_name") val companyName: String?
)

data class UserResponse(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("company_name") val companyName: String?,
    @SerializedName("is_active") val isActive: Boolean
)
