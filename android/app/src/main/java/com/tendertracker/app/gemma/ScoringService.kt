package com.tendertracker.app.gemma

import android.util.Log
import com.tendertracker.app.gemma.engine.GemmaInferenceEngine
import com.tendertracker.app.gemma.model.InferenceDefaults
import com.tendertracker.domain.model.Tender
import javax.inject.Inject
import javax.inject.Singleton

data class TenderScore(
    val score: Int,  // 0-100
    val explanation: String,
    val confidence: String  // "high", "medium", "low"
)

@Singleton
class ScoringService @Inject constructor(
    private val inferenceEngine: GemmaInferenceEngine
) {

    companion object {
        private const val TAG = "ScoringService"
    }

    suspend fun scoreTender(tender: Tender, userProfile: String = ""): Result<TenderScore> {
        if (!inferenceEngine.isReady()) {
            return Result.failure(
                IllegalStateException("AI model not loaded. Please download and load a model first.")
            )
        }

        val prompt = buildScoringPrompt(tender, userProfile)
        Log.d(TAG, "Scoring tender ${tender.id} with prompt: ${prompt.take(200)}...")

        return try {
            val response = inferenceEngine.generateText(prompt, InferenceDefaults.SCORING_CONFIG)

            response.fold(
                onSuccess = { text ->
                    val tenderScore = parseScoreResponse(text)
                    Log.d(TAG, "Scored tender ${tender.id}: ${tenderScore.score}/100")
                    Result.success(tenderScore)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to score tender ${tender.id}", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception while scoring tender ${tender.id}", e)
            Result.failure(e)
        }
    }

    private fun buildScoringPrompt(tender: Tender, userProfile: String): String {
        val profileContext = if (userProfile.isNotBlank()) {
            "User Profile: $userProfile\n\n"
        } else {
            "User Profile: General business looking for government contracts\n\n"
        }

        return """
You are an AI assistant that evaluates government tender opportunities for businesses.

$profileContext
Tender Information:
Title: ${tender.title}
Reference: ${tender.reference}
Source: ${tender.sourceId}
${tender.deadline?.let { "Deadline: $it" } ?: ""}
${tender.estimatedValue?.let { "Estimated Value: R ${"%,.0f".format(it)}" } ?: ""}

Requirements Summary:
${tender.requirementsSummary ?: "Not specified"}

${tender.extractedText?.take(2000)?.let { "Full Description:\n$it" } ?: ""}

Task: Evaluate this tender opportunity and provide:
1. A relevance score from 0-100 (where 100 is highly relevant and 0 is not relevant)
2. A brief explanation of why this score was given
3. Your confidence level (high/medium/low)

Format your response exactly as:
SCORE: [number]
EXPLANATION: [2-3 sentences explaining the score]
CONFIDENCE: [high/medium/low]
        """.trimIndent()
    }

    private fun parseScoreResponse(response: String): TenderScore {
        var score = 50  // Default moderate score
        var explanation = "Unable to parse AI response"
        var confidence = "low"

        try {
            // Extract score
            val scorePattern = Regex("SCORE:\\s*(\\d+)", RegexOption.IGNORE_CASE)
            scorePattern.find(response)?.let { match ->
                score = match.groupValues[1].toIntOrNull()?.coerceIn(0, 100) ?: score
            }

            // Extract explanation
            val explanationPattern = Regex("EXPLANATION:\\s*(.+?)(?=CONFIDENCE:|$)", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            explanationPattern.find(response)?.let { match ->
                explanation = match.groupValues[1].trim()
            }

            // Extract confidence
            val confidencePattern = Regex("CONFIDENCE:\\s*(high|medium|low)", RegexOption.IGNORE_CASE)
            confidencePattern.find(response)?.let { match ->
                confidence = match.groupValues[1].lowercase()
            }

            Log.d(TAG, "Parsed score=$score, confidence=$confidence")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing score response", e)
            // Return defaults
        }

        return TenderScore(
            score = score,
            explanation = explanation,
            confidence = confidence
        )
    }
}
