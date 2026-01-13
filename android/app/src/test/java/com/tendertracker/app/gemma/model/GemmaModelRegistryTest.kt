package com.tendertracker.app.gemma.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GemmaModelRegistryTest {

    @Test
    fun `registry contains all expected models`() {
        assertEquals(2, GemmaModelRegistry.ALL_MODELS.size)
    }

    @Test
    fun `get model by id returns correct model`() {
        val e2b = GemmaModelRegistry.getModelById("gemma-3n-e2b")
        assertNotNull(e2b)
        assertEquals("Gemma 3n E2B", e2b?.name)
        assertEquals(GemmaVariant.E2B, e2b?.variant)
    }

    @Test
    fun `get model by invalid id returns null`() {
        val model = GemmaModelRegistry.getModelById("invalid-id")
        assertNull(model)
    }

    @Test
    fun `e2b model has correct properties`() {
        val model = GemmaModelRegistry.GEMMA_3N_E2B
        assertEquals("gemma-3n-e2b", model.id)
        assertEquals(4, model.minRamGb)
        assertEquals(true, model.supportsGpu)
    }

    @Test
    fun `e4b model has correct properties`() {
        val model = GemmaModelRegistry.GEMMA_3N_E4B
        assertEquals("gemma-3n-e4b", model.id)
        assertEquals(6, model.minRamGb)
        assertEquals(true, model.supportsGpu)
    }

    @Test
    fun `model variants have correct display names`() {
        assertEquals("Gemma 3n E2B", GemmaVariant.E2B.displayName)
        assertEquals("Gemma 3n E4B", GemmaVariant.E4B.displayName)
    }

    @Test
    fun `model variants have correct param counts`() {
        assertEquals("2B", GemmaVariant.E2B.paramCount)
        assertEquals("4B", GemmaVariant.E4B.paramCount)
    }
}
