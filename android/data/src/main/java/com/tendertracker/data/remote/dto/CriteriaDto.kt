package com.tendertracker.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.tendertracker.domain.model.ScoringWeights
import com.tendertracker.domain.model.UserCriteria
import com.tendertracker.domain.model.ValueRange

// Response DTO
data class CriteriaResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("capabilities") val capabilities: List<String>,
    @SerializedName("preferred_sectors") val preferredSectors: List<String>,
    @SerializedName("preferred_locations") val preferredLocations: List<String>,
    @SerializedName("value_range") val valueRange: ValueRangeDto,
    @SerializedName("keywords") val keywords: List<String>,
    @SerializedName("weights") val weights: ScoringWeightsDto,
    @SerializedName("is_default") val isDefault: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class ValueRangeDto(
    @SerializedName("min") val min: Double,
    @SerializedName("max") val max: Double?
)

data class ScoringWeightsDto(
    @SerializedName("capability_match") val capabilityMatch: Double,
    @SerializedName("sector_match") val sectorMatch: Double,
    @SerializedName("location_match") val locationMatch: Double,
    @SerializedName("value_match") val valueMatch: Double,
    @SerializedName("keyword_match") val keywordMatch: Double
)

// Request DTOs
data class CriteriaCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("capabilities") val capabilities: List<String>,
    @SerializedName("preferred_sectors") val preferredSectors: List<String>,
    @SerializedName("preferred_locations") val preferredLocations: List<String>,
    @SerializedName("value_range") val valueRange: ValueRangeDto,
    @SerializedName("keywords") val keywords: List<String>,
    @SerializedName("weights") val weights: ScoringWeightsDto,
    @SerializedName("is_default") val isDefault: Boolean
)

data class CriteriaUpdateRequest(
    @SerializedName("name") val name: String?,
    @SerializedName("capabilities") val capabilities: List<String>?,
    @SerializedName("preferred_sectors") val preferredSectors: List<String>?,
    @SerializedName("preferred_locations") val preferredLocations: List<String>?,
    @SerializedName("value_range") val valueRange: ValueRangeDto?,
    @SerializedName("keywords") val keywords: List<String>?,
    @SerializedName("weights") val weights: ScoringWeightsDto?,
    @SerializedName("is_default") val isDefault: Boolean?
)

// Mappers
fun CriteriaResponse.toDomain(): UserCriteria = UserCriteria(
    id = id,
    userId = userId,
    name = name,
    capabilities = capabilities,
    preferredSectors = preferredSectors,
    preferredLocations = preferredLocations,
    valueRange = ValueRange(
        min = valueRange.min,
        max = valueRange.max
    ),
    keywords = keywords,
    weights = ScoringWeights(
        capabilityMatch = weights.capabilityMatch,
        sectorMatch = weights.sectorMatch,
        locationMatch = weights.locationMatch,
        valueMatch = weights.valueMatch,
        keywordMatch = weights.keywordMatch
    ),
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun UserCriteria.toCreateRequest(): CriteriaCreateRequest = CriteriaCreateRequest(
    name = name,
    capabilities = capabilities,
    preferredSectors = preferredSectors,
    preferredLocations = preferredLocations,
    valueRange = ValueRangeDto(
        min = valueRange.min,
        max = valueRange.max
    ),
    keywords = keywords,
    weights = ScoringWeightsDto(
        capabilityMatch = weights.capabilityMatch,
        sectorMatch = weights.sectorMatch,
        locationMatch = weights.locationMatch,
        valueMatch = weights.valueMatch,
        keywordMatch = weights.keywordMatch
    ),
    isDefault = isDefault
)
