package com.tendertracker.domain.model

/**
 * User criteria for tender scoring.
 * Defines what the user/company is looking for in tenders.
 */
data class UserCriteria(
    val id: String,
    val userId: String,
    val name: String,
    val capabilities: List<String>,
    val preferredSectors: List<String>,
    val preferredLocations: List<String>,
    val valueRange: ValueRange,
    val keywords: List<String>,
    val weights: ScoringWeights,
    val isDefault: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class ValueRange(
    val min: Double,
    val max: Double?
)

data class ScoringWeights(
    val capabilityMatch: Double,
    val sectorMatch: Double,
    val locationMatch: Double,
    val valueMatch: Double,
    val keywordMatch: Double
) {
    companion object {
        val DEFAULT = ScoringWeights(
            capabilityMatch = 0.4,
            sectorMatch = 0.2,
            locationMatch = 0.1,
            valueMatch = 0.1,
            keywordMatch = 0.2
        )
    }
}
