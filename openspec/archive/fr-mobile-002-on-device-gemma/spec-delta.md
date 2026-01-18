# Spec Delta: FR-MOBILE-002 On-Device Gemma

## ADDED

### android/gemma/src/main/java/com/tendertracker/gemma/GemmaModelType.kt
```kotlin
enum class GemmaModelType(
    val displayName: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val minFreeMemory: Long
) {
    E2B(
        displayName = "Gemma 2B (Fast)",
        fileName = "gemma-2b-it-q4.bin",
        downloadUrl = GemmaConfig.E2B_MODEL_URL,
        sizeBytes = GemmaConfig.E2B_MODEL_SIZE,
        minFreeMemory = GemmaConfig.MIN_FREE_MEMORY_E2B
    ),
    E4B(
        displayName = "Gemma 4B (Accurate)",
        fileName = "gemma-4b-it-q4.bin",
        downloadUrl = GemmaConfig.E4B_MODEL_URL,
        sizeBytes = GemmaConfig.E4B_MODEL_SIZE,
        minFreeMemory = GemmaConfig.MIN_FREE_MEMORY_E4B
    )
}
```

### android/gemma/src/main/java/com/tendertracker/gemma/GemmaConfig.kt
```kotlin
object GemmaConfig {
    const val E2B_MODEL_URL = "https://storage.googleapis.com/gemma-models/gemma-2b-it-q4.bin"
    const val E4B_MODEL_URL = "https://storage.googleapis.com/gemma-models/gemma-4b-it-q4.bin"
    const val E2B_MODEL_SIZE = 1_500_000_000L
    const val E4B_MODEL_SIZE = 3_000_000_000L
    const val MAX_TOKENS = 2048
    const val TEMPERATURE_EXTRACTION = 0.1f
    const val TEMPERATURE_SCORING = 0.2f
    const val TOP_K = 40
    const val TOP_P = 0.95f
    const val MIN_FREE_MEMORY_E2B = 2_000_000_000L
    const val MIN_FREE_MEMORY_E4B = 4_000_000_000L
}
```

### android/gemma/src/main/java/com/tendertracker/gemma/ModelDownloadManager.kt
```kotlin
class ModelDownloadManager @Inject constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    suspend fun downloadModel(
        modelType: GemmaModelType,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File>

    fun pauseDownload()

    fun resumeDownload()

    fun cancelDownload()

    fun getModelFile(modelType: GemmaModelType): File?

    fun isModelDownloaded(modelType: GemmaModelType): Boolean

    fun deleteModel(modelType: GemmaModelType)
}
```

### android/gemma/src/main/java/com/tendertracker/gemma/GemmaManager.kt
```kotlin
class GemmaManager @Inject constructor(
    private val context: Context,
    private val downloadManager: ModelDownloadManager
) {
    private var llmInference: LlmInference? = null
    private var currentModelType: GemmaModelType? = null

    suspend fun downloadModel(
        modelType: GemmaModelType,
        onProgress: (Float) -> Unit
    ): Result<Unit>

    fun isModelDownloaded(modelType: GemmaModelType): Boolean

    suspend fun loadModel(modelType: GemmaModelType): Result<Unit>

    fun isModelLoaded(): Boolean

    fun getLoadedModelType(): GemmaModelType?

    fun unloadModel()

    private fun checkMemoryAvailable(modelType: GemmaModelType): Boolean
}
```

### android/gemma/src/main/java/com/tendertracker/gemma/GemmaInferenceEngine.kt
```kotlin
class GemmaInferenceEngine @Inject constructor(
    private val gemmaManager: GemmaManager
) {
    suspend fun generateText(
        prompt: String,
        maxTokens: Int = GemmaConfig.MAX_TOKENS,
        temperature: Float = GemmaConfig.TEMPERATURE_EXTRACTION
    ): Result<String>

    suspend fun extractTenderInfo(pdfText: String): Result<TenderExtractionResult>

    suspend fun scoreTenderRelevance(
        tender: Tender,
        criteria: UserCriteria
    ): Result<TenderScore>

    private fun buildExtractionPrompt(text: String): String

    private fun buildScoringPrompt(tender: Tender, criteria: UserCriteria): String

    private fun parseExtractionResponse(response: String): TenderExtractionResult

    private fun parseScoringResponse(response: String): TenderScore
}
```

### android/gemma/src/main/java/com/tendertracker/gemma/model/TenderExtractionResult.kt
```kotlin
data class TenderExtractionResult(
    val title: String,
    val reference: String,
    val deadline: LocalDate?,
    val estimatedValue: Double?,
    val requirementsSummary: String?,
    val confidence: Float,
    val rawResponse: String,
    val extractionTimeMs: Long
)
```

### android/gemma/src/main/java/com/tendertracker/gemma/model/TenderScore.kt
```kotlin
data class TenderScore(
    val score: Int,
    val explanation: String,
    val scoringTimeMs: Long
)
```

### android/gemma/src/main/java/com/tendertracker/gemma/worker/GemmaInferenceWorker.kt
```kotlin
@HiltWorker
class GemmaInferenceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val gemmaManager: GemmaManager,
    private val inferenceEngine: GemmaInferenceEngine,
    private val tenderRepository: TenderRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())

        val tenderIds = inputData.getStringArray(KEY_TENDER_IDS) ?: return Result.failure()
        val taskType = inputData.getString(KEY_TASK_TYPE) ?: return Result.failure()

        return when (taskType) {
            TASK_EXTRACTION -> processExtraction(tenderIds.toList())
            TASK_SCORING -> processScoring(tenderIds.toList())
            else -> Result.failure()
        }
    }

    private suspend fun processExtraction(tenderIds: List<String>): Result
    private suspend fun processScoring(tenderIds: List<String>): Result
    private fun createForegroundInfo(): ForegroundInfo

    companion object {
        const val KEY_TENDER_IDS = "tender_ids"
        const val KEY_TASK_TYPE = "task_type"
        const val TASK_EXTRACTION = "extraction"
        const val TASK_SCORING = "scoring"
    }
}
```

### android/gemma/src/main/res/raw/extraction_prompt.txt
```
Extract tender information from this document and return valid JSON only.

DOCUMENT:
{tender_text}

Return JSON with these exact fields:
{
  "title": "<tender title>",
  "reference": "<tender reference number>",
  "deadline": "<YYYY-MM-DD or null>",
  "estimated_value": <number or null>,
  "requirements_summary": "<max 300 characters>"
}

Return ONLY the JSON, no other text.
```

### android/gemma/src/main/res/raw/scoring_prompt.txt
```
Score this tender's relevance to the company on a scale of 0-100.

COMPANY PROFILE:
- Name: {company_name}
- Industry: {industry}
- Capabilities: {capabilities}
- Preferred sectors: {sectors}
- Keywords: {keywords}

TENDER DETAILS:
- Title: {tender_title}
- Reference: {tender_reference}
- Deadline: {tender_deadline}
- Value: {tender_value}
- Requirements: {tender_requirements}

Scoring guide:
- 90-100: Perfect match, core capability
- 70-89: Good match, relevant sector
- 50-69: Moderate match, some overlap
- 30-49: Weak match, limited relevance
- 0-29: Poor match, not suitable

Return JSON: {"score": <0-100>, "explanation": "<brief 100 char reason>"}
```

## MODIFIED

### android/data/build.gradle.kts
```diff
dependencies {
+   implementation(project(":gemma"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
}
```

### android/app/build.gradle.kts
```diff
dependencies {
+   implementation(project(":gemma"))
    implementation(project(":domain"))
    implementation(project(":data"))
}
```

## REMOVED

None.
