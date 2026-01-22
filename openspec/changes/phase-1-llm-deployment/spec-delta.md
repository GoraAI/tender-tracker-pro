# Spec Delta: Phase 1 - LLM Deployment (Android On-Device)

This document describes the specification changes for Android on-device LLM deployment.

---

## ADDED

### 1. Model Configuration Specification

#### `ModelConfig.kt`

**Purpose**: Centralized configuration for Gemma 3n E2B model

**Location**: `android/app/src/main/java/com/tendertracker/app/gemma/ModelConfig.kt`

**Implementation**:
```kotlin
// NOTE: Model configuration already exists in GemmaModelRegistry.kt
// This is the actual configuration from the codebase:

object GemmaModelRegistry {
    val GEMMA_3N_E2B = GemmaModel(
        id = "gemma-3n-e2b",
        name = "Gemma 3n E2B (Multimodal)",
        variant = GemmaVariant.E2B,
        sizeBytes = 3_400_000_000L, // 3.4 GB
        downloadUrl = "https://huggingface.co/na5h13/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm",
        fallbackUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/ba9ca88da013b537b6ed38108be609b8db1c3a16/gemma-3n-E2B-it-int4.litertlm",
        description = "Efficient 2B multimodal model with text, vision, and audio support",
        minRamGb = 8,
        supportsGpu = true,
        supportsVision = true,
        supportsAudio = true,
        contextLength = 4096
    )
}

// Device capability constants (to be added if not present)
object DeviceRequirements {
    const val MIN_RAM_MB = 4096 // 4 GB (lower than model spec for Tier 2 support)
    const val RECOMMENDED_RAM_MB = 8192 // 8 GB (model spec)
    const val MIN_STORAGE_BYTES = 5_000_000_000L // 5 GB
    const val MIN_API_LEVEL = 31 // Android 12
}

// Inference parameters
object InferenceConfig {
    const val TEMPERATURE = 0.7f
    const val MAX_TOKENS = 1024
    const val TOP_K = 40
}
```

---

### 2. Device Capability Checker Specification

#### `DeviceCapabilityChecker.kt`

**Purpose**: Detect if device can run on-device LLM

**Location**: `android/app/src/main/java/com/tendertracker/app/gemma/DeviceCapabilityChecker.kt`

**Interface**:
```kotlin
data class Capability(
    val hasEnoughRAM: Boolean,
    val hasEnoughStorage: Boolean,
    val hasMinAPILevel: Boolean,
    val isCapable: Boolean,
    val ramMB: Long,
    val storageBytes: Long,
    val apiLevel: Int
)

class DeviceCapabilityChecker(private val context: Context) {

    fun checkCapability(): Capability

    fun getRAMInfo(): Long // Returns total RAM in MB

    fun getStorageInfo(): Long // Returns available storage in bytes

    fun getAPILevel(): Int // Returns Build.VERSION.SDK_INT
}
```

**Behavior**:
- Checks total device RAM ≥ 4GB
- Checks available storage ≥ 5GB
- Checks Android API level ≥ 31
- Returns comprehensive capability report

---

### 3. Model Downloader Specification

#### `ModelDownloader.kt`

**Purpose**: Download, validate, and store Gemma model

**Location**: `android/app/src/main/java/com/tendertracker/app/gemma/ModelDownloader.kt`

**Interface**:
```kotlin
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    object Validating : DownloadState()
    object Complete : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ModelDownloader(
    private val context: Context,
    private val httpClient: OkHttpClient
) {

    suspend fun downloadModel(
        url: String,
        onProgress: (Int) -> Unit
    ): Result<File>

    suspend fun validateChecksum(file: File, expectedChecksum: String): Boolean

    suspend fun resumeDownload(url: String, partialFile: File): Result<File>

    fun getModelFile(): File? // Returns file if exists
}
```

**Behavior**:
1. Downloads model to cache directory first
2. Updates progress callback (0-100%)
3. Validates SHA256 checksum after download
4. Moves to app files directory on success: `/data/data/com.tendertracker.app/files/gemma/`
5. Supports pause/resume via HTTP range requests
6. Saves download state to SharedPreferences

**Storage Location**:
- Download target: `/data/data/com.tendertracker.app/files/gemma/gemma-3n-e2b.bin`

---

### 4. Onboarding ViewModel Specification

#### `OnboardingViewModel.kt`

**Purpose**: Manage onboarding flow state

**Location**: `android/app/src/main/java/com/tendertracker/app/ui/onboarding/OnboardingViewModel.kt`

**Interface**:
```kotlin
sealed class OnboardingState {
    object CheckingDevice : OnboardingState()
    data class DeviceIncompatible(val reason: String) : OnboardingState()
    object ReadyToDownload : OnboardingState()
    data class Downloading(val progress: Int) : OnboardingState()
    object DownloadComplete : OnboardingState()
    data class DownloadError(val error: String, val canRetry: Boolean) : OnboardingState()
}

class OnboardingViewModel(
    private val deviceChecker: DeviceCapabilityChecker,
    private val modelDownloader: ModelDownloader,
    private val preferences: SharedPreferences
) : ViewModel() {

    val state: StateFlow<OnboardingState>

    fun checkDevice()

    fun startDownload()

    fun retryDownload()

    fun skipOnboarding() // Navigate to app (mark model not required)
}
```

**State Transitions**:
```
CheckingDevice → DeviceIncompatible (if device fails checks)
CheckingDevice → ReadyToDownload (if device passes)
ReadyToDownload → Downloading (user taps "Download")
Downloading → DownloadComplete (success)
Downloading → DownloadError (failure)
DownloadError → Downloading (user taps "Retry")
```

---

### 5. Onboarding Screen Specification

#### `OnboardingScreen.kt`

**Purpose**: UI for model download onboarding

**Location**: `android/app/src/main/java/com/tendertracker/app/ui/onboarding/OnboardingScreen.kt`

**UI States**:

1. **Checking Device**:
   - Spinner with "Checking device compatibility..."

2. **Device Incompatible**:
   - Warning icon
   - Message explaining why (RAM/Storage/API)
   - "Continue without AI" button

3. **Ready to Download**:
   - Info card: "AI features require 3.4 GB download"
   - Storage warning if <10 GB free
   - "Download AI Model" primary button
   - "Skip" secondary button

4. **Downloading**:
   - Linear progress bar (0-100%)
   - "Downloading AI model... 42%" text
   - File size: "1.5 GB / 3.4 GB"
   - "Pause" button (optional)

5. **Download Complete**:
   - Checkmark icon
   - "AI model ready!"
   - "Continue" button

6. **Download Error**:
   - Error icon
   - Error message
   - "Retry" button
   - "Skip" button

**Navigation**:
- After completion or skip → Navigate to MainActivity
- Save onboarding completion flag to SharedPreferences

---

## MODIFIED

### 1. GemmaInference Implementation

#### `GemmaInference.kt`

**Location**: `android/app/src/main/java/com/tendertracker/app/gemma/GemmaInference.kt`

**Before**: Mock/stub implementation
**After**: Real MediaPipe LLM integration

**Changes**:
```kotlin
class GemmaInference @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var llmInference: LlmInference? = null

    // NEW: Initialize from downloaded model
    suspend fun initialize(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val modelPath = context.filesDir.resolve("gemma/${ModelConfig.MODEL_FILENAME}")

                if (!modelPath.exists()) {
                    return@withContext Result.failure(
                        ModelNotFoundException("Model not found. Complete onboarding first.")
                    )
                }

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath.absolutePath)
                    .setMaxTokens(ModelConfig.MAX_TOKENS)
                    .setTemperature(ModelConfig.TEMPERATURE)
                    .setTopK(ModelConfig.TOP_K)
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // MODIFIED: Real inference instead of mock
    suspend fun generateResponse(prompt: String): Result<String> {
        val inference = llmInference ?: return Result.failure(
            IllegalStateException("Model not initialized. Call initialize() first.")
        )

        return withContext(Dispatchers.IO) {
            try {
                val response = inference.generateResponse(prompt)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // NEW: Cleanup
    fun release() {
        llmInference?.close()
        llmInference = null
    }
}
```

**Performance Characteristics**:
- Initialization: <30 seconds on mid-range device
- Inference latency: <5 seconds for chat (P95), <10 seconds for scoring (P95)
- Memory overhead: <1 GB additional RAM

---

### 2. ChatViewModel Integration

#### `ChatViewModel.kt`

**Location**: `android/app/src/main/java/com/tendertracker/app/ui/chat/ChatViewModel.kt`

**Before**: No LLM integration, chat non-functional
**After**: Real LLM inference

**Changes**:
```kotlin
class ChatViewModel @Inject constructor(
    private val gemmaInference: GemmaInference, // INJECTED
    private val tenderRepository: TenderRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Initialize model on first access (lazy)
        viewModelScope.launch {
            gemmaInference.initialize()
        }
    }

    // MODIFIED: Real inference
    fun sendMessage(message: String, tenderId: String? = null) {
        viewModelScope.launch {
            _messages.value += ChatMessage.User(message)
            _isLoading.value = true
            _error.value = null

            // Build prompt with tender context
            val prompt = buildPromptWithContext(message, tenderId)

            when (val result = gemmaInference.generateResponse(prompt)) {
                is Result.Success -> {
                    _messages.value += ChatMessage.AI(
                        text = result.data,
                        source = "On-device AI"
                    )
                }
                is Result.Error -> {
                    _error.value = "Failed to generate response"
                    _messages.value += ChatMessage.Error(result.error.message ?: "Unknown error")
                }
            }

            _isLoading.value = false
        }
    }

    private suspend fun buildPromptWithContext(message: String, tenderId: String?): String {
        val tender = tenderId?.let { tenderRepository.getTender(it) }

        return if (tender != null) {
            """
            You are analyzing a tender. Here is the context:

            Title: ${tender.title}
            Reference: ${tender.reference}
            Description: ${tender.description}

            User question: $message

            Provide a helpful, concise answer based on the tender information.
            """.trimIndent()
        } else {
            message
        }
    }
}
```

---

### 3. Tender Detail Screen

#### `TenderDetailScreen.kt`

**Location**: `android/app/src/main/java/com/tendertracker/app/ui/tender/TenderDetailScreen.kt`

**Before**: Shows placeholder/null for score
**After**: Shows LLM-generated score with explanation

**Changes**:
- Add "Analyze Tender" button
- Show score (0-100) prominently when available
- Display LLM-generated explanation below score
- Show loading indicator during scoring
- Add confidence indicator for low-confidence scores (<70%)

**UI Example**:
```
┌─────────────────────────────────────┐
│ Tender Details                      │
│                                     │
│ [Analyze Tender] Button             │
│                                     │
│ Relevance Score                     │
│ ┌─────────────────────────────────┐ │
│ │          85                      │ │
│ │      ★★★★☆                      │ │
│ │                                 │ │
│ │ Highly relevant: This tender    │ │
│ │ matches your software           │ │
│ │ development capabilities and    │ │
│ │ experience with government      │ │
│ │ projects.                       │ │
│ │                                 │ │
│ │ 🤖 On-device AI                 │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

---

### 4. Navigation Graph

#### `NavGraph.kt`

**Location**: `android/app/src/main/java/com/tendertracker/app/navigation/NavGraph.kt`

**Before**: No onboarding screen
**After**: Add onboarding as initial destination if model not downloaded

**Changes**:
```kotlin
@Composable
fun TenderTrackerNavGraph(
    navController: NavHostController,
    startDestination: String = determineStartDestination()
) {
    NavHost(navController, startDestination) {
        // NEW: Onboarding screen
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("login") { LoginScreen(...) }
        composable("dashboard") { DashboardScreen(...) }
        // ... other screens
    }
}

private fun determineStartDestination(): String {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val modelDownloaded = prefs.getBoolean("model_downloaded", false)

    return if (!modelDownloaded) {
        "onboarding"
    } else {
        "login"
    }
}
```

---

### 5. Dependency Injection Module

#### `GemmaModule.kt`

**Location**: `android/app/src/main/java/com/tendertracker/app/di/GemmaModule.kt`

**Addition**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object GemmaModule {

    @Provides
    @Singleton
    fun provideDeviceCapabilityChecker(
        @ApplicationContext context: Context
    ): DeviceCapabilityChecker {
        return DeviceCapabilityChecker(context)
    }

    @Provides
    @Singleton
    fun provideModelDownloader(
        @ApplicationContext context: Context,
        httpClient: OkHttpClient
    ): ModelDownloader {
        return ModelDownloader(context, httpClient)
    }

    @Provides
    @Singleton
    fun provideGemmaInference(
        @ApplicationContext context: Context
    ): GemmaInference {
        return GemmaInference(context)
    }
}
```

---

## REMOVED

None. This change is purely additive - no existing functionality is removed.

---

## Configuration Changes Summary

### Android Changes

| File | Change Type | Description |
|------|-------------|-------------|
| `gemma/ModelConfig.kt` | ADDED | Model URL, checksum, device requirements |
| `gemma/DeviceCapabilityChecker.kt` | ADDED | Device RAM/storage/API checks |
| `gemma/ModelDownloader.kt` | ADDED | Model download with progress and validation |
| `ui/onboarding/OnboardingViewModel.kt` | ADDED | Onboarding state management |
| `ui/onboarding/OnboardingScreen.kt` | ADDED | Onboarding UI with download progress |
| `gemma/GemmaInference.kt` | MODIFIED | Load model from storage, real inference |
| `ui/chat/ChatViewModel.kt` | MODIFIED | Integrate LLM for chat responses |
| `ui/tender/TenderDetailScreen.kt` | MODIFIED | Display LLM-generated scores |
| `navigation/NavGraph.kt` | MODIFIED | Add onboarding as initial screen |
| `di/GemmaModule.kt` | ADDED | Hilt DI for Gemma classes |

### No Backend Changes

- ❌ No Railway configuration changes
- ❌ No requirements-prod.txt changes
- ❌ No backend endpoint changes
- ❌ No environment variable changes

---

## Backward Compatibility

**API Compatibility**: N/A (no backend changes)

**Android Compatibility**: ✅ Graceful degradation
- Devices with <4GB RAM: Show "AI features unavailable" message
- Devices with <5GB storage: Warn before download, allow skip
- Devices with Android <12: Show "Android 12+ required" message
- App works without model (AI features disabled but core functionality intact)

**User Experience**:
- Existing users: Onboarding shown on first launch after update
- New users: Onboarding shown on first launch
- Users can skip onboarding and use app without AI features
- Onboarding can be re-triggered from settings (future)

---

## Testing Requirements

### Android Testing

#### Unit Tests
- [ ] DeviceCapabilityChecker: RAM/storage/API detection
- [ ] ModelDownloader: Download, checksum validation, pause/resume
- [ ] OnboardingViewModel: State transitions
- [ ] GemmaInference: Model loading, error handling

#### Integration Tests
- [ ] Full onboarding flow: Check device → Download → Complete
- [ ] Model persistence across app restarts
- [ ] Chat with real LLM inference
- [ ] Scoring with real LLM inference

#### UI Tests (Compose)
- [ ] OnboardingScreen: All states render correctly
- [ ] ChatScreen: AI responses appear
- [ ] TenderDetailScreen: Score displays correctly

#### Performance Tests
- [ ] Model download time (3.4 GB over Wi-Fi)
- [ ] Model load time (<30s target)
- [ ] Chat inference latency (<5s P95)
- [ ] Scoring inference latency (<10s P95)
- [ ] Memory usage during inference (<1GB overhead)

#### Device Testing
- [ ] Low-end device (4GB RAM): Graceful degradation
- [ ] Mid-range device (6GB RAM): Full functionality
- [ ] High-end device (8GB+ RAM): Optimal performance
- [ ] Various Android versions (12, 13, 14)

---

## Migration Guide

### For Users (App Update)

1. **Update app** from Play Store or install new APK
2. **First launch** shows onboarding screen
3. **Download model** (3.4 GB) over Wi-Fi
4. **AI features** become available after download
5. **Can skip** onboarding and use app without AI

### For Developers (Testing)

1. **Install APK** on test device:
   ```bash
   adb install -r app-debug.apk
   ```

2. **Complete onboarding** on first launch

3. **Verify model downloaded**:
   ```bash
   adb shell ls -lh /data/data/com.tendertracker.app/files/gemma/
   ```

4. **Test chat** and **scoring** features

5. **Clear data to reset**:
   ```bash
   adb shell pm clear com.tendertracker.app
   ```

---

## Performance Benchmarks (Expected)

### Android

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Model Download Time | <10 min | Stopwatch over Wi-Fi |
| Model Load Time | <30s | Startup timing |
| Chat Inference Latency (P95) | <5s | UI timing |
| Scoring Inference Latency (P95) | <10s | UI timing |
| Memory Usage | +300-800 MB | Android Profiler |
| APK Size Increase | 0 MB | APK analyzer (model not bundled) |
| Storage Usage | 3.4 GB | Device storage |

### No Backend Metrics

- ❌ No backend performance impact (on-device only)

---

## Rollback Procedure

If deployment fails or critical bugs discovered:

1. **Release APK without onboarding**:
   - Remove onboarding screen from nav graph
   - Skip model download
   - Disable AI features (show "Coming soon")

2. **Or use feature flag**:
   ```kotlin
   object FeatureFlags {
       const val ONBOARDING_ENABLED = false // Disable onboarding
   }
   ```

3. **Users with downloaded model**:
   - Model remains in storage (no harm)
   - AI features show "Temporarily unavailable"

---

**Spec Status**: ⏸️ **PROPOSED**

These specifications will be implemented upon approval of the Phase 1 proposal (revised for Android on-device only).
