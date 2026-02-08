package com.tendertracker.app.services

import com.tendertracker.app.gemma.ScoringService
import com.tendertracker.domain.model.CompanyProfile
import com.tendertracker.domain.model.Tender
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GemmaResponseGenerator @Inject constructor(
    private val scoringService: ScoringService
) {

    /**
     * Generate a technical response for a tender question using on-device Gemma.
     *
     * @param question The technical question to answer
     * @param tender The tender context
     * @param companyProfile User's company profile
     * @return Generated response text
     */
    suspend fun generateTechnicalResponse(
        question: String,
        tender: Tender,
        companyProfile: CompanyProfile?
    ): Result<String> {
        return try {
            val prompt = buildPrompt(question, tender, companyProfile)

            // Use existing ScoringService's Gemma integration
            val response = scoringService.generateResponse(prompt)

            if (response.isNotEmpty()) {
                Result.success(response)
            } else {
                Result.failure(Exception("Failed to generate response - empty output"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("AI generation failed: ${e.message}"))
        }
    }

    /**
     * Build prompt for Gemma with context and instructions.
     */
    private fun buildPrompt(
        question: String,
        tender: Tender,
        companyProfile: CompanyProfile?
    ): String {
        return buildString {
            appendLine("You are assisting with a tender application for ${companyProfile?.legalName ?: "a company"}.")
            appendLine()

            if (companyProfile != null) {
                appendLine("Company Profile:")
                appendLine("- Legal Name: ${companyProfile.legalName}")
                if (companyProfile.capabilities.isNotEmpty()) {
                    appendLine("- Capabilities: ${companyProfile.capabilities.joinToString(", ")}")
                }
                if (companyProfile.preferredSectors.isNotEmpty()) {
                    appendLine("- Sectors: ${companyProfile.preferredSectors.joinToString(", ")}")
                }
                appendLine()
            }

            appendLine("Tender Context:")
            appendLine("- Title: ${tender.title}")
            appendLine("- Reference: ${tender.reference}")
            if (tender.requirementsSummary != null) {
                val summary = tender.requirementsSummary.take(300) // Limit context
                appendLine("- Requirements: $summary...")
            }
            appendLine()

            appendLine("Question to Answer:")
            appendLine(question)
            appendLine()

            appendLine("Instructions:")
            appendLine("- Provide a professional, detailed response (200-500 words)")
            appendLine("- Demonstrate how our company's capabilities meet the tender requirements")
            appendLine("- Use specific examples where possible")
            appendLine("- Maintain formal business tone")
            appendLine("- Focus on how we can deliver value")
            appendLine()

            appendLine("Response:")
        }
    }

    /**
     * Generate a company overview response.
     */
    suspend fun generateCompanyOverview(
        companyProfile: CompanyProfile
    ): Result<String> {
        return try {
            val prompt = buildString {
                appendLine("Generate a professional company overview for tender applications.")
                appendLine()
                appendLine("Company Details:")
                appendLine("- Legal Name: ${companyProfile.legalName}")
                if (companyProfile.capabilities.isNotEmpty()) {
                    appendLine("- Capabilities: ${companyProfile.capabilities.joinToString(", ")}")
                }
                if (companyProfile.preferredSectors.isNotEmpty()) {
                    appendLine("- Experience in: ${companyProfile.preferredSectors.joinToString(", ")}")
                }
                appendLine()
                appendLine("Write a compelling 150-200 word company overview that highlights our strengths and experience.")
                appendLine()
                appendLine("Overview:")
            }

            val response = scoringService.generateResponse(prompt)

            if (response.isNotEmpty()) {
                Result.success(response)
            } else {
                Result.failure(Exception("Failed to generate overview - empty output"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("AI generation failed: ${e.message}"))
        }
    }
}
