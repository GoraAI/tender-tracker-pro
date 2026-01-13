package com.tendertracker.app.ui.chat

import javax.inject.Inject
import javax.inject.Singleton

enum class UserIntent {
    SEARCH_TENDERS,
    SUMMARIZE_TENDER,
    EXPLAIN_SCORE,
    LIST_HIGH_PRIORITY,
    LIST_CLOSING_SOON,
    GENERAL_QUESTION
}

@Singleton
class IntentClassifier @Inject constructor() {

    fun classify(message: String): UserIntent {
        val lower = message.lowercase()

        return when {
            containsAny(lower, SUMMARIZE_KEYWORDS) -> UserIntent.SUMMARIZE_TENDER
            containsAny(lower, EXPLAIN_KEYWORDS) -> UserIntent.EXPLAIN_SCORE
            containsAny(lower, PRIORITY_KEYWORDS) -> UserIntent.LIST_HIGH_PRIORITY
            containsAny(lower, DEADLINE_KEYWORDS) -> UserIntent.LIST_CLOSING_SOON
            containsAny(lower, SEARCH_KEYWORDS) -> UserIntent.SEARCH_TENDERS
            else -> UserIntent.GENERAL_QUESTION
        }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    companion object {
        private val SEARCH_KEYWORDS = listOf(
            "find", "search", "show", "list", "get", "look for"
        )
        private val SUMMARIZE_KEYWORDS = listOf(
            "summarize", "summary", "overview", "brief", "describe"
        )
        private val EXPLAIN_KEYWORDS = listOf(
            "why", "explain", "reason", "how come", "score"
        )
        private val PRIORITY_KEYWORDS = listOf(
            "high priority", "important", "top", "best", "recommended"
        )
        private val DEADLINE_KEYWORDS = listOf(
            "closing", "deadline", "due", "expiring", "urgent", "soon"
        )
    }
}
