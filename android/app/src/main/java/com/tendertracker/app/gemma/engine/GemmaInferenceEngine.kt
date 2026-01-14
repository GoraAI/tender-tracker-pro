package com.tendertracker.app.gemma.engine

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.tendertracker.app.gemma.model.GemmaModel
import com.tendertracker.app.gemma.model.InferenceConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

typealias ResultListener = (text: String, done: Boolean) -> Unit

@Singleton
class GemmaInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GemmaInferenceEngine"
    }

    private var currentModel: GemmaModel? = null
    private var modelPath: String? = null
    private var isInitialized = false
    private var llmInference: LlmInference? = null

    suspend fun initialize(model: GemmaModel, path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing LLM with model: ${model.name} at path: $path")

                // Clean up any existing instance
                cleanup()

                // Build options for LLM inference
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(path)
                    .setMaxTokens(model.contextLength)
                    .build()

                // Create the inference instance
                llmInference = LlmInference.createFromOptions(context, options)

                currentModel = model
                modelPath = path
                isInitialized = true

                Log.d(TAG, "LLM initialized successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LLM", e)
                isInitialized = false
                Result.failure(e)
            }
        }

    suspend fun generateText(
        prompt: String,
        config: InferenceConfig = InferenceConfig()
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isInitialized || llmInference == null) {
            return@withContext Result.failure(
                IllegalStateException("Engine not initialized. Please load a model first.")
            )
        }

        try {
            Log.d(TAG, "Generating response for prompt: ${prompt.take(50)}...")

            val response = llmInference!!.generateResponse(prompt)

            Log.d(TAG, "Generated response: ${response.take(100)}...")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate response", e)
            Result.failure(e)
        }
    }

    fun generateTextStream(
        prompt: String,
        config: InferenceConfig = InferenceConfig()
    ): Flow<String> = callbackFlow {
        if (!isInitialized || llmInference == null) {
            throw IllegalStateException("Engine not initialized. Please load a model first.")
        }

        Log.d(TAG, "Starting streaming generation for prompt: ${prompt.take(50)}...")

        // Set up result listener for streaming
        llmInference!!.generateResponseAsync(prompt) { partialResult, done ->
            if (partialResult.isNotEmpty()) {
                trySend(partialResult)
            }
            if (done) {
                Log.d(TAG, "Streaming generation complete")
                close()
            }
        }

        awaitClose {
            Log.d(TAG, "Streaming flow closed")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateResponse(
        prompt: String,
        config: InferenceConfig,
        listener: ResultListener
    ) = withContext(Dispatchers.IO) {
        if (!isInitialized || llmInference == null) {
            listener("Error: Engine not initialized. Please load a model first.", true)
            return@withContext
        }

        try {
            Log.d(TAG, "Generating response with listener for prompt: ${prompt.take(50)}...")

            llmInference!!.generateResponseAsync(prompt) { partialResult, done ->
                listener(partialResult, done)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate response", e)
            listener("Error: ${e.message}", true)
        }
    }

    fun cleanup() {
        Log.d(TAG, "Cleaning up LLM inference engine")
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing LLM inference", e)
        }
        llmInference = null
        isInitialized = false
        currentModel = null
        modelPath = null
    }

    fun isReady(): Boolean = isInitialized && llmInference != null

    fun getCurrentModel(): GemmaModel? = currentModel
}
