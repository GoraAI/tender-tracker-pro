package com.tendertracker.app.gemma.model

data class GemmaModel(
    val id: String,
    val name: String,
    val variant: GemmaVariant,
    val sizeBytes: Long,
    val downloadUrl: String,
    val description: String,
    val minRamGb: Int,
    val supportsGpu: Boolean = true
)

enum class GemmaVariant(val displayName: String, val paramCount: String) {
    E2B("Gemma 3n E2B", "2B"),
    E4B("Gemma 3n E4B", "4B")
}

object GemmaModelRegistry {
    val GEMMA_3N_E2B = GemmaModel(
        id = "gemma-3n-e2b",
        name = "Gemma 3n E2B",
        variant = GemmaVariant.E2B,
        sizeBytes = 1_500_000_000L, // ~1.5GB
        downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it",
        description = "Efficient 2B parameter model for on-device inference",
        minRamGb = 4,
        supportsGpu = true
    )

    val GEMMA_3N_E4B = GemmaModel(
        id = "gemma-3n-e4b",
        name = "Gemma 3n E4B",
        variant = GemmaVariant.E4B,
        sizeBytes = 3_000_000_000L, // ~3GB
        downloadUrl = "https://huggingface.co/google/gemma-3n-E4B-it",
        description = "Full 4B parameter model for better quality",
        minRamGb = 6,
        supportsGpu = true
    )

    val ALL_MODELS = listOf(GEMMA_3N_E2B, GEMMA_3N_E4B)

    fun getModelById(id: String): GemmaModel? =
        ALL_MODELS.find { it.id == id }
}
