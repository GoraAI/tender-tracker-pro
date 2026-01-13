package com.tendertracker.app.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class IntentClassifierTest {

    private lateinit var classifier: IntentClassifier

    @Before
    fun setup() {
        classifier = IntentClassifier()
    }

    @Test
    fun `classify search intent`() {
        assertEquals(
            UserIntent.SEARCH_TENDERS,
            classifier.classify("find tenders about water")
        )
        assertEquals(
            UserIntent.SEARCH_TENDERS,
            classifier.classify("search for construction tenders")
        )
        assertEquals(
            UserIntent.SEARCH_TENDERS,
            classifier.classify("show me IT tenders")
        )
    }

    @Test
    fun `classify summarize intent`() {
        assertEquals(
            UserIntent.SUMMARIZE_TENDER,
            classifier.classify("summarize this tender")
        )
        assertEquals(
            UserIntent.SUMMARIZE_TENDER,
            classifier.classify("give me a summary")
        )
        assertEquals(
            UserIntent.SUMMARIZE_TENDER,
            classifier.classify("provide an overview")
        )
    }

    @Test
    fun `classify explain score intent`() {
        assertEquals(
            UserIntent.EXPLAIN_SCORE,
            classifier.classify("why is this score so high?")
        )
        assertEquals(
            UserIntent.EXPLAIN_SCORE,
            classifier.classify("explain the score")
        )
        assertEquals(
            UserIntent.EXPLAIN_SCORE,
            classifier.classify("what's the reason for this rating?")
        )
    }

    @Test
    fun `classify high priority intent`() {
        assertEquals(
            UserIntent.LIST_HIGH_PRIORITY,
            classifier.classify("show high priority tenders")
        )
        assertEquals(
            UserIntent.LIST_HIGH_PRIORITY,
            classifier.classify("what are the most important ones?")
        )
        assertEquals(
            UserIntent.LIST_HIGH_PRIORITY,
            classifier.classify("list top tenders")
        )
    }

    @Test
    fun `classify deadline intent`() {
        assertEquals(
            UserIntent.LIST_CLOSING_SOON,
            classifier.classify("tenders closing soon")
        )
        assertEquals(
            UserIntent.LIST_CLOSING_SOON,
            classifier.classify("what deadlines are coming up?")
        )
        assertEquals(
            UserIntent.LIST_CLOSING_SOON,
            classifier.classify("urgent tenders")
        )
    }

    @Test
    fun `classify general question`() {
        assertEquals(
            UserIntent.GENERAL_QUESTION,
            classifier.classify("hello")
        )
        assertEquals(
            UserIntent.GENERAL_QUESTION,
            classifier.classify("what can you do?")
        )
        assertEquals(
            UserIntent.GENERAL_QUESTION,
            classifier.classify("help me understand procurement")
        )
    }
}
