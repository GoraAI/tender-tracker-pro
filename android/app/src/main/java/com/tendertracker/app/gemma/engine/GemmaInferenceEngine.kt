package com.tendertracker.app.gemma.engine

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.tendertracker.app.gemma.model.GemmaModel
import com.tendertracker.app.gemma.model.InferenceBackend
import com.tendertracker.app.gemma.model.InferenceConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

typealias ResultListener = (text: String, done: Boolean) -> Unit

/**
 * Data class to hold the LiteRT LM engine and conversation instances.
 * This follows the pattern from Google AI Edge Gallery.
 */
data class LlmModelInstance(
    val engine: Engine,
    var conversation: Conversation
)

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
    private var modelInstance: LlmModelInstance? = null

    suspend fun initialize(model: GemmaModel, path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing LLM with model: ${model.name} at path: $path")

                // Clean up any existing instance
                cleanup()

                // Determine backend based on GPU support
                val backend = if (model.supportsGpu) Backend.GPU else Backend.CPU
                Log.d(TAG, "Using backend: $backend")

                // Build engine config
                // NOTE: Vision and audio backends are disabled for now as they cause model load failures
                // with the current LiteRT LM API. Text-only inference works fine.
                val engineConfig = EngineConfig(
                    modelPath = path,
                    backend = backend,
                    visionBackend = null, // Disabled - causes "Failed to load model from buffer"
                    audioBackend = null,  // Disabled - causes "Failed to load model from buffer"
                    maxNumTokens = model.contextLength,
                    cacheDir = null // Let LiteRT use default cache location
                )

                // Create and initialize engine
                Log.d(TAG, "Creating LiteRT LM Engine...")
                val engine = Engine(engineConfig)
                engine.initialize()
                Log.d(TAG, "Engine initialized successfully")

                // Create conversation with default config
                Log.d(TAG, "Creating conversation...")
                val conversation = engine.createConversation(
                    ConversationConfig(
                        samplerConfig = SamplerConfig(
                            topK = 40,
                            topP = 0.95,
                            temperature = 0.7
                        )
                    )
                )
                Log.d(TAG, "Conversation created successfully")

                // Store the instance
                modelInstance = LlmModelInstance(engine = engine, conversation = conversation)
                currentModel = model
                modelPath = path
                isInitialized = true

                Log.d(TAG, "LLM initialized successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LLM", e)
                isInitialized = false
                cleanup()
                Result.failure(e)
            }
        }

    suspend fun generateText(
        prompt: String,
        config: InferenceConfig = InferenceConfig()
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isInitialized || modelInstance == null) {
            return@withContext Result.failure(
                IllegalStateException("Engine not initialized. Please load a model first.")
            )
        }

        try {
            Log.d(TAG, "Generating response for prompt: ${prompt.take(50)}...")

            // Use suspendCancellableCoroutine to convert callback to suspend function
            val response = suspendCancellableCoroutine { continuation ->
                val fullResponse = StringBuilder()

                modelInstance!!.conversation.sendMessageAsync(
                    Message.of(Content.Text(prompt)),
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            fullResponse.append(message.toString())
                        }

                        override fun onDone() {
                            continuation.resume(fullResponse.toString())
                        }

                        override fun onError(throwable: Throwable) {
                            if (throwable is CancellationException) {
                                continuation.cancel(throwable)
                            } else {
                                continuation.resume("Error: ${throwable.message}")
                            }
                        }
                    }
                )
            }

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
        if (!isInitialized || modelInstance == null) {
            throw IllegalStateException("Engine not initialized. Please load a model first.")
        }

        Log.d(TAG, "Starting streaming generation for prompt: ${prompt.take(50)}...")

        // Use sendMessageAsync with MessageCallback for streaming
        modelInstance!!.conversation.sendMessageAsync(
            Message.of(Content.Text(prompt)),
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    val partialResult = message.toString()
                    if (partialResult.isNotEmpty()) {
                        trySend(partialResult)
                    }
                }

                override fun onDone() {
                    Log.d(TAG, "Streaming generation complete")
                    close()
                }

                override fun onError(throwable: Throwable) {
                    if (throwable is CancellationException) {
                        Log.i(TAG, "The inference is cancelled.")
                    } else {
                        Log.e(TAG, "onError", throwable)
                    }
                    close(throwable)
                }
            }
        )

        awaitClose {
            Log.d(TAG, "Streaming flow closed")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateResponse(
        prompt: String,
        config: InferenceConfig,
        listener: ResultListener
    ) = withContext(Dispatchers.IO) {
        if (!isInitialized || modelInstance == null) {
            listener("Error: Engine not initialized. Please load a model first.", true)
            return@withContext
        }

        try {
            Log.d(TAG, "Generating response with listener for prompt: ${prompt.take(50)}...")

            modelInstance!!.conversation.sendMessageAsync(
                Message.of(Content.Text(prompt)),
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        val partialResult = message.toString()
                        listener(partialResult, false)
                    }

                    override fun onDone() {
                        listener("", true)
                    }

                    override fun onError(throwable: Throwable) {
                        if (throwable is CancellationException) {
                            Log.i(TAG, "The inference is cancelled.")
                            listener("", true)
                        } else {
                            Log.e(TAG, "onError", throwable)
                            listener("Error: ${throwable.message}", true)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate response", e)
            listener("Error: ${e.message}", true)
        }
    }

    /**
     * Resets the conversation while keeping the engine initialized.
     * This clears the conversation history and creates a new conversation instance.
     */
    suspend fun resetConversation(config: InferenceConfig = InferenceConfig()): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (modelInstance == null) {
                    return@withContext Result.failure(
                        IllegalStateException("Engine not initialized")
                    )
                }

                Log.d(TAG, "Resetting conversation...")

                // Close old conversation
                modelInstance!!.conversation.close()

                // Create new conversation with provided config
                val newConversation = modelInstance!!.engine.createConversation(
                    ConversationConfig(
                        samplerConfig = SamplerConfig(
                            topK = config.topK,
                            topP = config.topP.toDouble(),
                            temperature = config.temperature.toDouble()
                        )
                    )
                )

                modelInstance!!.conversation = newConversation
                Log.d(TAG, "Conversation reset successfully")

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset conversation", e)
                Result.failure(e)
            }
        }

    fun cleanup() {
        Log.d(TAG, "Cleaning up LLM inference engine")

        modelInstance?.let { instance ->
            try {
                instance.conversation.close()
                Log.d(TAG, "Conversation closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing conversation", e)
            }

            try {
                instance.engine.close()
                Log.d(TAG, "Engine closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing engine", e)
            }
        }

        modelInstance = null
        isInitialized = false
        currentModel = null
        modelPath = null

        Log.d(TAG, "Cleanup complete")
    }

    fun isReady(): Boolean = isInitialized && modelInstance != null

    fun getCurrentModel(): GemmaModel? = currentModel
}
