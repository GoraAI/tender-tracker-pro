# FR-MOBILE-002: On-Device Gemma Integration

**Status**: PROPOSED
**Priority**: P0
**Created**: 2026-01-13
**Dependencies**: FR-MOBILE-001 (Android Foundation)

---

## Summary

Integrate Google's Gemma 3n models (E2B 2B params, E4B 4B params) for on-device AI inference using MediaPipe LLM Inference API, enabling offline tender analysis, extraction, and scoring directly on the mobile device.

---

## Motivation

On-device AI provides critical advantages:
- **Privacy**: Tender data never leaves the device
- **Offline**: Works without internet connectivity
- **Speed**: No network latency for inference
- **Cost**: No API costs for LLM calls
- **Compliance**: Data sovereignty for sensitive government tenders

Gemma 3n is ideal because:
- Optimized for mobile (E2B: 2B params, E4B: 4B params)
- Google's MediaPipe provides native Android support
- Quantized models fit in mobile memory
- Good performance on extraction/scoring tasks

---

## Scope

### In Scope
- MediaPipe LLM Inference integration
- Gemma model downloading and management
- Model selection (E2B for quick tasks, E4B for deep analysis)
- Tender information extraction on-device
- Relevance scoring on-device
- Memory management for model loading
- Background processing with WorkManager
- Progress tracking for downloads/inference

### Out of Scope
- Fine-tuning models on device
- Training new models
- GPU acceleration (CPU-only for broad compatibility)
- Cloud fallback (offline-first)
- Multiple simultaneous model loads

---

## Acceptance Criteria

- [ ] Can download Gemma E2B model (~1.5 GB)
- [ ] Can download Gemma E4B model (~3 GB)
- [ ] Shows download progress with pause/resume
- [ ] Can load model into memory for inference
- [ ] Inference completes within acceptable time (<10s for extraction)
- [ ] Can extract tender info from PDF text using Gemma
- [ ] Can score tender relevance using Gemma
- [ ] Memory usage stays within limits (< 4GB total)
- [ ] Model unloads properly when not needed
- [ ] Background inference with WorkManager works
- [ ] Handles low-memory conditions gracefully
- [ ] Unit tests for inference wrapper
- [ ] Integration tests with sample prompts

---

## Technical Approach

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    GEMMA MODULE                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                  GemmaManager                         │  │
│  │  - downloadModel(modelType)                          │  │
│  │  - loadModel(modelType)                              │  │
│  │  - unloadModel()                                     │  │
│  │  - isModelReady()                                    │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                GemmaInferenceEngine                   │  │
│  │  - generateText(prompt)                              │  │
│  │  - extractTenderInfo(text)                           │  │
│  │  - scoreTenderRelevance(tender, criteria)            │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              MediaPipe LLM Inference                  │  │
│  │  - LlmInference.createFromOptions()                  │  │
│  │  - generateResponse()                                │  │
│  │  - generateResponseAsync()                           │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Model Variants

| Model | Params | Size | Use Case | Memory |
|-------|--------|------|----------|--------|
| Gemma E2B | 2B | ~1.5 GB | Quick extraction, categorization | ~2 GB RAM |
| Gemma E4B | 4B | ~3 GB | Deep analysis, scoring | ~4 GB RAM |

**Default Strategy**:
- Load E2B for initial extraction (faster)
- Load E4B for final scoring (more accurate)
- Only one model loaded at a time

### MediaPipe Integration

```kotlin
// Model loading
val options = LlmInference.LlmInferenceOptions.builder()
    .setModelPath(modelPath)
    .setMaxTokens(2048)
    .setTemperature(0.1f)  // Low for extraction
    .setTopK(40)
    .setTopP(0.95f)
    .build()

val llmInference = LlmInference.createFromOptions(context, options)

// Inference
val response = llmInference.generateResponse(prompt)
```

### Prompt Templates

**Extraction Prompt**:
```
Extract tender information from this document and return JSON:

{tender_text}

Return JSON with fields: title, reference, deadline (YYYY-MM-DD),
estimated_value (number), requirements_summary (max 300 chars)
```

**Scoring Prompt**:
```
Score this tender's relevance (0-100) to the company:

COMPANY: {company_name}
CAPABILITIES: {capabilities}
KEYWORDS: {keywords}

TENDER:
Title: {title}
Requirements: {requirements}

Return JSON: {"score": <0-100>, "explanation": "<brief reason>"}
```

---

## Implementation Plan

### Phase 1: MediaPipe Setup (4 tasks)
- Add MediaPipe dependencies
- Create GemmaModelType enum (E2B, E4B)
- Create model paths/URLs configuration
- Test MediaPipe initialization

### Phase 2: Model Download (6 tasks)
- Create ModelDownloadManager
- Implement download with progress
- Implement pause/resume download
- Verify model integrity (checksum)
- Store models in app-specific storage
- Track download status in preferences

### Phase 3: Model Loading (5 tasks)
- Create GemmaManager class
- Implement loadModel() with options
- Implement unloadModel()
- Add memory checks before loading
- Handle OOM gracefully

### Phase 4: Inference Engine (6 tasks)
- Create GemmaInferenceEngine class
- Implement generateText() base method
- Implement extractTenderInfo() with prompt
- Implement scoreTenderRelevance() with prompt
- Parse JSON responses
- Add timeout handling

### Phase 5: Background Processing (4 tasks)
- Create GemmaInferenceWorker (WorkManager)
- Implement batch extraction worker
- Implement batch scoring worker
- Add notification for progress

### Phase 6: Testing (5 tasks)
- Unit tests for prompt building
- Unit tests for response parsing
- Instrumented test for model loading
- Instrumented test for inference
- Performance benchmarks

---

## Configuration

```kotlin
// GemmaConfig.kt
object GemmaConfig {
    // Model URLs (Google AI Studio / HuggingFace)
    const val E2B_MODEL_URL = "https://storage.googleapis.com/gemma-models/gemma-2b-it-q4.bin"
    const val E4B_MODEL_URL = "https://storage.googleapis.com/gemma-models/gemma-4b-it-q4.bin"

    // Model sizes for download progress
    const val E2B_MODEL_SIZE = 1_500_000_000L  // ~1.5 GB
    const val E4B_MODEL_SIZE = 3_000_000_000L  // ~3 GB

    // Inference settings
    const val MAX_TOKENS = 2048
    const val TEMPERATURE_EXTRACTION = 0.1f
    const val TEMPERATURE_SCORING = 0.2f
    const val TOP_K = 40
    const val TOP_P = 0.95f

    // Memory thresholds
    const val MIN_FREE_MEMORY_E2B = 2_000_000_000L  // 2 GB
    const val MIN_FREE_MEMORY_E4B = 4_000_000_000L  // 4 GB
}
```

---

## API Design

### GemmaManager

```kotlin
class GemmaManager @Inject constructor(
    private val context: Context,
    private val downloadManager: ModelDownloadManager
) {
    private var loadedModel: LlmInference? = null
    private var currentModelType: GemmaModelType? = null

    suspend fun downloadModel(
        modelType: GemmaModelType,
        onProgress: (Float) -> Unit
    ): Result<Unit>

    fun isModelDownloaded(modelType: GemmaModelType): Boolean

    suspend fun loadModel(modelType: GemmaModelType): Result<Unit>

    fun isModelLoaded(): Boolean

    fun unloadModel()

    fun getLoadedModelType(): GemmaModelType?
}
```

### GemmaInferenceEngine

```kotlin
class GemmaInferenceEngine @Inject constructor(
    private val gemmaManager: GemmaManager
) {
    suspend fun generateText(
        prompt: String,
        maxTokens: Int = GemmaConfig.MAX_TOKENS
    ): Result<String>

    suspend fun extractTenderInfo(
        pdfText: String
    ): Result<TenderExtractionResult>

    suspend fun scoreTenderRelevance(
        tender: Tender,
        criteria: UserCriteria
    ): Result<TenderScore>
}
```

### Data Classes

```kotlin
data class TenderExtractionResult(
    val title: String,
    val reference: String,
    val deadline: LocalDate?,
    val estimatedValue: Double?,
    val requirementsSummary: String?,
    val confidence: Float,
    val rawResponse: String
)

data class TenderScore(
    val score: Int,  // 0-100
    val explanation: String,
    val scoringTimeMs: Long
)

enum class GemmaModelType(val displayName: String, val sizeBytes: Long) {
    E2B("Gemma 2B (Fast)", GemmaConfig.E2B_MODEL_SIZE),
    E4B("Gemma 4B (Accurate)", GemmaConfig.E4B_MODEL_SIZE)
}
```

---

## Performance Targets

- **Model download**: Show progress every 100ms
- **Model load time**: < 10 seconds
- **E2B extraction**: < 5 seconds per tender
- **E4B scoring**: < 8 seconds per tender
- **Memory overhead**: < 500 MB above model size

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Large model downloads | User abandonment | Pause/resume, Wi-Fi only option |
| Out of memory | App crash | Check memory before load, unload when done |
| Slow inference on old devices | Poor UX | Show progress, allow cancellation |
| Model updates break compatibility | Inference fails | Pin model versions, test upgrades |
| MediaPipe API changes | Build breaks | Pin MediaPipe version |

---

## Testing Strategy

1. **Unit Tests**:
   - Prompt building
   - Response parsing
   - Memory threshold checks
   - Download progress calculation

2. **Instrumented Tests**:
   - Model download (small test model)
   - Model loading/unloading
   - Basic inference
   - Memory usage validation

3. **Manual Tests**:
   - Full E2B download and inference
   - Full E4B download and inference
   - Memory pressure scenarios
   - Background processing

---

## Success Metrics

- [ ] E2B model downloads successfully
- [ ] E4B model downloads successfully
- [ ] Model loads without OOM
- [ ] Extraction produces valid JSON
- [ ] Scoring produces valid scores (0-100)
- [ ] Background inference works
- [ ] All tests pass
- [ ] Inference time within targets

---

## Future Enhancements

- GPU acceleration (when available)
- Model quantization options
- Streaming inference responses
- Multi-model parallel loading
- Model fine-tuning on user data
- Automatic model selection based on task
