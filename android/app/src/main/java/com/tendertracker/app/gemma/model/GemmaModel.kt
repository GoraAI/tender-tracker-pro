package com.tendertracker.app.gemma.model

data class GemmaModel(
    val id: String,
    val name: String,
    val variant: GemmaVariant,
    val version: String = "v1", // Model version for download path (like Google's pattern)
    val sizeBytes: Long,
    val downloadUrl: String,
    val fallbackUrl: String? = null,
    val description: String,
    val minRamGb: Int,
    val supportsGpu: Boolean = true,
    val supportsVision: Boolean = false,
    val supportsAudio: Boolean = false,
    val contextLength: Int = 4096
)

enum class GemmaVariant(val displayName: String, val paramCount: String) {
    E2B("Gemma 3n E2B", "2B"),
    E4B("Gemma 3n E4B", "4B")
}

object GemmaModelRegistry {
    val GEMMA_3N_E2B = GemmaModel(
        id = "gemma-3n-e2b",
        name = "Gemma 3n E2B (Multimodal)",
        variant = GemmaVariant.E2B,
        sizeBytes = 3_400_000_000L, // 3.4GB
        downloadUrl = "https://huggingface.co/na5h13/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm",
        fallbackUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/ba9ca88da013b537b6ed38108be609b8db1c3a16/gemma-3n-E2B-it-int4.litertlm",
        description = "Efficient 2B multimodal model with text, vision, and audio support",
        minRamGb = 8,
        supportsGpu = true,
        supportsVision = true,
        supportsAudio = true,
        contextLength = 4096
    )

    val GEMMA_3N_E4B = GemmaModel(
        id = "gemma-3n-e4b",
        name = "Gemma 3n E4B (Multimodal)",
        variant = GemmaVariant.E4B,
        sizeBytes = 4_600_000_000L, // 4.6GB
        downloadUrl = "https://huggingface.co/na5h13/gemma-3n-E4B-it-litert-lm/resolve/main/gemma-3n-E4B-it-int4.litertlm",
        fallbackUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm/resolve/297ed75955702dec3503e00c2c2ecbbf475300bc/gemma-3n-E4B-it-int4.litertlm",
        description = "Full 4B multimodal model for better quality with text, vision, and audio",
        minRamGb = 12,
        supportsGpu = true,
        supportsVision = true,
        supportsAudio = true,
        contextLength = 4096
    )

    val ALL_MODELS = listOf(GEMMA_3N_E2B, GEMMA_3N_E4B)

    fun getModelById(id: String): GemmaModel? =
        ALL_MODELS.find { it.id == id }
}
