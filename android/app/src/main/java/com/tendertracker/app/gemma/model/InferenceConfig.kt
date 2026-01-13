package com.tendertracker.app.gemma.model

data class InferenceConfig(
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val backend: InferenceBackend = InferenceBackend.CPU
)

enum class InferenceBackend {
    CPU, GPU
}

object InferenceDefaults {
    val CHAT_CONFIG = InferenceConfig(
        maxTokens = 512,
        temperature = 0.7f,
        topK = 40,
        topP = 0.95f
    )

    val SCORING_CONFIG = InferenceConfig(
        maxTokens = 256,
        temperature = 0.3f,
        topK = 20,
        topP = 0.9f
    )

    val SUMMARY_CONFIG = InferenceConfig(
        maxTokens = 1024,
        temperature = 0.5f,
        topK = 30,
        topP = 0.95f
    )
}
