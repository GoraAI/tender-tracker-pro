package com.tendertracker.app.gemma.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        assertEquals("Gemma 3n E2B (Multimodal)", e2b?.name)
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
        assertEquals(8, model.minRamGb)
        assertEquals(true, model.supportsGpu)
        assertEquals(true, model.supportsVision)
        assertEquals(true, model.supportsAudio)
        assertEquals(4096, model.contextLength)
        assertEquals(3_400_000_000L, model.sizeBytes)
        assertNotNull(model.fallbackUrl)
    }

    @Test
    fun `e4b model has correct properties`() {
        val model = GemmaModelRegistry.GEMMA_3N_E4B
        assertEquals("gemma-3n-e4b", model.id)
        assertEquals(12, model.minRamGb)
        assertEquals(true, model.supportsGpu)
        assertEquals(true, model.supportsVision)
        assertEquals(true, model.supportsAudio)
        assertEquals(4096, model.contextLength)
        assertEquals(4_600_000_000L, model.sizeBytes)
        assertNotNull(model.fallbackUrl)
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

    @Test
    fun `models have huggingface download urls`() {
        assertTrue(GemmaModelRegistry.GEMMA_3N_E2B.downloadUrl.contains("huggingface.co"))
        assertTrue(GemmaModelRegistry.GEMMA_3N_E4B.downloadUrl.contains("huggingface.co"))
    }
}
