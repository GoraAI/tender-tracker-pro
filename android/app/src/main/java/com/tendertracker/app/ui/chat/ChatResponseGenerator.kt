package com.tendertracker.app.ui.chat

import com.tendertracker.app.gemma.engine.GemmaInferenceEngine
import com.tendertracker.app.gemma.model.InferenceDefaults
import com.tendertracker.domain.model.Tender
import com.tendertracker.domain.repository.TenderRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class ChatContext(
    val tenderId: String? = null
)

@Singleton
class ChatResponseGenerator @Inject constructor(
    private val inferenceEngine: GemmaInferenceEngine,
    private val tenderRepository: TenderRepository,
    private val intentClassifier: IntentClassifier
) {

    suspend fun generateResponse(
        message: String,
        context: ChatContext
    ): Result<String> {
        val intent = intentClassifier.classify(message)

        return when (intent) {
            UserIntent.SEARCH_TENDERS -> handleSearch(message)
            UserIntent.SUMMARIZE_TENDER -> handleSummarize(context.tenderId)
            UserIntent.EXPLAIN_SCORE -> handleExplainScore(context.tenderId)
            UserIntent.LIST_HIGH_PRIORITY -> handleHighPriority()
            UserIntent.LIST_CLOSING_SOON -> handleClosingSoon()
            UserIntent.GENERAL_QUESTION -> handleGeneral(message, context)
        }
    }

    private suspend fun handleSearch(query: String): Result<String> {
        val searchTerms = extractSearchTerms(query)
        val tenders = tenderRepository.searchTenders(searchTerms).first()

        return if (tenders.isEmpty()) {
            Result.success("I couldn't find any tenders matching '$searchTerms'.")
        } else {
            val summary = tenders.take(5).joinToString("\n") {
                "- ${it.title} (${it.reference}) - Score: ${it.score ?: "N/A"}"
            }
            Result.success("Found ${tenders.size} tenders:\n\n$summary")
        }
    }

    private suspend fun handleSummarize(tenderId: String?): Result<String> {
        if (tenderId == null) {
            return Result.success(
                "Please select a tender first, or tell me which tender " +
                "you'd like summarized."
            )
        }

        val tender = tenderRepository.getTenderById(tenderId).first()
            ?: return Result.success("Tender not found.")

        if (!inferenceEngine.isReady()) {
            return Result.success(buildBasicSummary(tender))
        }

        val prompt = buildSummarizePrompt(tender)
        return inferenceEngine.generateText(prompt, InferenceDefaults.SUMMARY_CONFIG)
    }

    private suspend fun handleExplainScore(tenderId: String?): Result<String> {
        if (tenderId == null) {
            return Result.success(
                "Please select a tender first to explain its score."
            )
        }

        val tender = tenderRepository.getTenderById(tenderId).first()
            ?: return Result.success("Tender not found.")

        if (tender.score == null) {
            return Result.success("This tender hasn't been scored yet.")
        }

        return Result.success(
            tender.scoreExplanation ?: "Score: ${tender.score}/100"
        )
    }

    private suspend fun handleHighPriority(): Result<String> {
        val tenders = tenderRepository.getHighPriorityTenders(70).first()

        return if (tenders.isEmpty()) {
            Result.success("No high-priority tenders found (score >= 70).")
        } else {
            val summary = tenders.take(5).joinToString("\n") {
                "- [${it.score}] ${it.title} (${it.reference})"
            }
            Result.success("Top ${tenders.size.coerceAtMost(5)} high-priority tenders:\n\n$summary")
        }
    }

    private suspend fun handleClosingSoon(): Result<String> {
        // For now, return all tenders with deadlines
        val tenders = tenderRepository.getAllTenders().first()
            .filter { it.deadline != null }
            .take(5)

        return if (tenders.isEmpty()) {
            Result.success("No tenders with upcoming deadlines found.")
        } else {
            val summary = tenders.joinToString("\n") {
                "- ${it.title} - Deadline: ${it.deadline}"
            }
            Result.success("Tenders closing soon:\n\n$summary")
        }
    }

    private suspend fun handleGeneral(
        message: String,
        context: ChatContext
    ): Result<String> {
        if (!inferenceEngine.isReady()) {
            return Result.success(
                "AI model not loaded. Go to Settings > AI Models to download."
            )
        }

        val prompt = if (context.tenderId != null) {
            val tender = tenderRepository.getTenderById(context.tenderId).first()
            if (tender != null) {
                buildContextualPrompt(message, tender)
            } else {
                message
            }
        } else {
            buildGeneralPrompt(message)
        }

        return inferenceEngine.generateText(prompt, InferenceDefaults.CHAT_CONFIG)
    }

    private fun extractSearchTerms(query: String): String {
        val stopWords = listOf(
            "find", "search", "show", "list", "get", "me", "for",
            "tenders", "tender", "about", "the", "a", "an"
        )
        return query.lowercase()
            .split(" ")
            .filter { it !in stopWords && it.length > 2 }
            .joinToString(" ")
            .ifEmpty { query }
    }

    private fun buildBasicSummary(tender: Tender): String {
        return """
            **${tender.title}**
            Reference: ${tender.reference}
            Source: ${tender.sourceId}
            ${tender.deadline?.let { "Deadline: $it" } ?: ""}
            ${tender.estimatedValue?.let { "Est. Value: R ${"%,.0f".format(it)}" } ?: ""}

            ${tender.requirementsSummary ?: "No summary available."}
        """.trimIndent()
    }

    private fun buildSummarizePrompt(tender: Tender): String {
        return """
            Summarize this tender in 3-4 sentences:

            Title: ${tender.title}
            Reference: ${tender.reference}
            Requirements: ${tender.requirementsSummary ?: "Not specified"}
            ${tender.extractedText?.take(2000) ?: ""}
        """.trimIndent()
    }

    private fun buildContextualPrompt(message: String, tender: Tender): String {
        return """
            Context: The user is viewing tender "${tender.title}" (${tender.reference}).

            User question: $message

            Please provide a helpful response about this tender.
        """.trimIndent()
    }

    private fun buildGeneralPrompt(message: String): String {
        return """
            You are a helpful AI assistant for Tender Tracker Pro, an app
            that helps users find and evaluate government tenders.

            User question: $message

            Please provide a concise, helpful response.
        """.trimIndent()
    }
}
