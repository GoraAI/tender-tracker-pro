# Proposal: Phase 1 - LLM Deployment (Android On-Device)

**Status**: PROPOSED
**Priority**: P0 (CRITICAL - Core Value Proposition)
**Created**: 2026-01-21
**Revised**: 2026-01-21 (Architecture correction: on-device only)
**Estimated Effort**: 1-2 days
**Impact**: 75% → 90% production readiness

---

## Summary

Deploy Gemma 3n E2B LLM (3.4 GB, LiteRT format) to Android app with lazy download during onboarding to enable on-device AI-powered tender analysis, scoring, and conversational chat features. Model already hosted on Hugging Face with fallback URL configured.

---

## Motivation

### Current State (The Problem)

**Android LLM Status**:
- ✅ Code: `android/.../gemma/` directory (Gemma inference implementation exists)
- ✅ Integration: MediaPipe Tasks GenAI configured in code
- ❌ **Model NOT Downloaded**: Gemma 3n E2B model (3.4 GB) not available
- ❌ **Onboarding Flow Missing**: No download UI during first launch
- ❌ **Testing**: On-device inference never tested on real device
- ❌ **Chat Non-Functional**: Chat interface shows UI but no AI responses

**Business Impact**:
- 🔴 **Core value proposition is broken**: "AI-powered tender analysis" doesn't work
- 🔴 **Can't score tenders**: Relevance scoring non-functional
- 🔴 **Can't analyze tenders**: Information extraction disabled
- 🔴 **Chat interface useless**: No LLM to power conversations
- 🔴 **No competitive advantage**: App is just a viewer, not an analyzer

### Desired State (The Solution)

**Android with On-Device LLM**:
- ✅ Gemma 3n E2B model (3.4 GB) downloaded to device storage
- ✅ Onboarding flow with download progress UI
- ✅ On-device inference working on real device (Android 12+, 4GB+ RAM)
- ✅ Chat interface functional with real AI responses
- ✅ Tender scoring generates 0-100 relevance scores with explanations
- ✅ Tender analysis extracts key information from PDFs
- ✅ Graceful degradation for low-end devices (feature disabled with message)

**Business Impact**:
- ✅ **Core value proposition works**: AI analysis functional
- ✅ **Tenders scored accurately**: 0-100 relevance with explanations
- ✅ **Information extracted**: Title, reference, deadline, value, requirements
- ✅ **Chat interface live**: Natural language Q&A about tenders
- ✅ **Competitive advantage**: On-device AI = privacy + speed

---

## Scope

### ✅ INCLUDED in This Change

**Model Acquisition**:
1. Download Gemma 3n E2B model (3.4 GB) in MediaPipe-compatible format
2. Verify model file integrity (checksum validation)
3. Host model on CDN or public URL for app download

**Android Onboarding Flow**:
4. Create onboarding screen with model download UI
5. Implement background download with progress indicator
6. Add download pause/resume functionality
7. Validate downloaded model before marking complete
8. Store model in app-private storage (`/data/data/.../files/gemma/`)
9. Persist download state (SharedPreferences) to resume after app restart
10. Show storage requirements before download (3.4 GB warning)

**Device Capability Detection**:
11. Check device RAM (minimum 4GB)
12. Check available storage (minimum 5GB free)
13. Check Android API level (minimum 31 / Android 12)
14. Show graceful error for unsupported devices

**On-Device Inference Integration**:
15. Update `GemmaInference` to load downloaded model from storage
16. Initialize MediaPipe LLM with model path
17. Configure inference parameters (temperature, max tokens, context length)
18. Implement proper error handling for model loading failures

**Chat Interface Integration**:
19. Connect `ChatViewModel` to `GemmaInference`
20. Implement streaming responses (if MediaPipe supports)
21. Add loading state during inference
22. Add error state with retry functionality
23. Display inference source indicator ("On-device AI")

**Tender Scoring Integration**:
24. Connect scoring logic to on-device LLM
25. Generate 0-100 relevance scores with explanations
26. Update `TenderDetailScreen` to display scores
27. Add confidence indicator for low-confidence scores

**Tender Analysis Integration**:
28. Connect extraction logic to on-device LLM
29. Extract title, reference, deadline, value, requirements from tender text
30. Update tender detail view to show extracted information

**Testing & Validation**:
31. Test onboarding download flow on real device
32. Test model loading on device with 4GB RAM
33. Test chat interface with various prompts
34. Test tender scoring accuracy (manual validation)
35. Test tender extraction accuracy (manual validation)
36. Performance test: Inference latency (target <5s for chat, <10s for scoring)
37. Memory test: Model loading doesn't crash app (<1GB additional RAM)
38. Storage test: Model persists across app restarts
39. Edge case test: Low-end device shows graceful error

**Documentation**:
40. Update README.md with LLM deployment status
41. Update PROJECT_STATUS.md: 75% → 90%
42. Add LESSONS_LEARNED.md entries for model deployment
43. Document model download URL and hosting strategy

### ❌ NOT INCLUDED (Explicitly Out of Scope)

**Backend LLM Deployment**:
- ❌ No Railway LLM deployment
- ❌ No `llama-cpp-python` dependency
- ❌ No backend inference endpoints
- ❌ No backend fallback (on-device only)

**Advanced Features**:
- ❌ Model updates / versioning (use initial version only)
- ❌ Model compression / quantization (use E2B as-is)
- ❌ Multi-model support (Gemma 3n only)
- ❌ Fine-tuning or custom training
- ❌ A/B testing different prompts

**Future Enhancements**:
- ❌ Cloud fallback for unsupported devices (Phase 2)
- ❌ Model download over Wi-Fi only enforcement (Phase 2)
- ❌ Model deletion to free storage (Phase 2)

---

## Architecture

### Before (Current State)

```
┌─────────────────────────────────────────────┐
│ Android App                                 │
│                                             │
│  ┌──────────────┐                          │
│  │ Chat UI      │ ──X──> No LLM           │
│  └──────────────┘                          │
│                                             │
│  ┌──────────────┐                          │
│  │ Scoring UI   │ ──X──> No LLM           │
│  └──────────────┘                          │
│                                             │
│  ┌──────────────┐                          │
│  │ GemmaInference│ ──X──> No Model File   │
│  └──────────────┘                          │
│                                             │
│  Status: 40% - Code exists, not functional │
└─────────────────────────────────────────────┘
```

### After (Target State)

```
┌─────────────────────────────────────────────────────────┐
│ Android App                                             │
│                                                         │
│  ┌──────────────────────────────────────┐              │
│  │ Onboarding Flow (First Launch)       │              │
│  │                                      │              │
│  │  1. Check device capability         │              │
│  │  2. Show 3.7GB storage warning      │              │
│  │  3. Download model from CDN         │              │
│  │  4. Show progress bar               │              │
│  │  5. Validate checksum               │              │
│  │  6. Save to app storage             │              │
│  └──────────────────────────────────────┘              │
│                  │                                      │
│                  ▼                                      │
│  ┌──────────────────────────────────────┐              │
│  │ /data/data/.../files/gemma/          │              │
│  │   gemma-3n-e2b.bin (3.4 GB)         │              │
│  └──────────────────────────────────────┘              │
│                  │                                      │
│                  ▼                                      │
│  ┌──────────────────────────────────────┐              │
│  │ GemmaInference (MediaPipe)           │              │
│  │  - Loads model from storage          │              │
│  │  - Runs inference on device          │              │
│  │  - No network required               │              │
│  └──────────────────────────────────────┘              │
│         │              │              │                 │
│         ▼              ▼              ▼                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Chat UI  │  │ Scoring  │  │ Analysis │             │
│  │          │  │ 0-100    │  │ Extract  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
│                                                         │
│  Status: 90% - Fully functional AI features            │
└─────────────────────────────────────────────────────────┘

                     ▲
                     │ (One-time download)
                     │
             ┌───────────────┐
             │  CDN / Public │
             │  Model Host   │
             │  (3.4 GB)     │
             └───────────────┘
```

---

## Acceptance Criteria

### Model Download & Storage (AC-M1 through AC-M5)

- [ ] **AC-M1**: Gemma 3n E2B model (3.4 GB) downloaded and hosted on accessible URL
- [ ] **AC-M2**: Onboarding screen shows "Download AI Model (3.4 GB required)" with progress bar
- [ ] **AC-M3**: Model downloads successfully on real device over Wi-Fi
- [ ] **AC-M4**: Downloaded model saved to `/data/data/com.tendertracker.app/files/gemma/gemma-3n-e2b.bin`
- [ ] **AC-M5**: Download state persists across app restart (can resume after interruption)

### Device Capability (AC-D1 through AC-D3)

- [ ] **AC-D1**: App detects device RAM < 4GB and shows "AI features unavailable on this device" message
- [ ] **AC-D2**: App detects storage < 5GB free and shows "Insufficient storage" warning before download
- [ ] **AC-D3**: App checks Android API level < 31 and shows "Android 12+ required for AI features"

### On-Device Inference (AC-I1 through AC-I5)

- [ ] **AC-I1**: Model loads successfully from storage on app startup (or lazy load on first use)
- [ ] **AC-I2**: Model loading completes in <30 seconds on mid-range device
- [ ] **AC-I3**: Inference generates response in <5 seconds for chat messages (P95)
- [ ] **AC-I4**: Inference uses <1GB additional RAM during operation
- [ ] **AC-I5**: Model loading errors show graceful error message with retry option

### Chat Interface (AC-C1 through AC-C3)

- [ ] **AC-C1**: Chat interface sends message and receives AI-generated response
- [ ] **AC-C2**: Chat shows loading indicator during inference
- [ ] **AC-C3**: Chat displays "🤖 On-device AI" indicator below responses

### Tender Scoring (AC-S1 through AC-S3)

- [ ] **AC-S1**: Tender detail screen shows relevance score (0-100) generated by on-device LLM
- [ ] **AC-S2**: Score includes text explanation (e.g., "Highly relevant: matches your criteria for software tenders")
- [ ] **AC-S3**: Score generation completes in <10 seconds (P95)

### Tender Analysis (AC-A1 through AC-A2)

- [ ] **AC-A1**: Tender detail screen shows extracted fields: title, reference, deadline, estimated value
- [ ] **AC-A2**: Extraction accuracy ≥70% (manually validated on 10 sample tenders)

### Testing & Quality (AC-T1 through AC-T3)

- [ ] **AC-T1**: End-to-end test passes: Download model → Load model → Chat response → Score tender
- [ ] **AC-T2**: App doesn't crash on low-end device (shows graceful degradation)
- [ ] **AC-T3**: Visual verification screenshots show working chat and scoring features

---

## Technical Approach

### Phase 1: Model Acquisition & Hosting (Day 1 Morning)

1. **Download Gemma 3n E2B** from Google AI Edge
   - Model: `gemma-2b-it-gpu-int4.bin` or similar MediaPipe-compatible format
   - Size: 3.4 GB
   - Format: Quantized for on-device inference

2. **Host Model on CDN**
   - Option A: Upload to Google Cloud Storage with public URL
   - Option B: Upload to GitHub Releases (if <2GB limit allows multi-part)
   - Option C: Use Hugging Face model hosting
   - Verify: Download URL is accessible and fast

3. **Generate Checksum**
   - Create SHA256 checksum for integrity validation
   - Store checksum in app code for verification after download

### Phase 2: Onboarding Flow Implementation (Day 1 Afternoon)

4. **Create OnboardingViewModel**
   ```kotlin
   class OnboardingViewModel(
       private val modelDownloader: ModelDownloader,
       private val deviceChecker: DeviceCapabilityChecker,
       private val preferenceManager: PreferenceManager
   ) : ViewModel() {

       sealed class OnboardingState {
           object CheckingDevice : OnboardingState()
           data class DeviceIncompatible(val reason: String) : OnboardingState()
           object ReadyToDownload : OnboardingState()
           data class Downloading(val progress: Int) : OnboardingState()
           object DownloadComplete : OnboardingState()
           data class DownloadError(val error: String) : OnboardingState()
       }

       suspend fun checkDeviceAndDownload() {
           // Check device capability
           // Download model if compatible
           // Validate checksum
           // Save to storage
       }
   }
   ```

5. **Create OnboardingScreen**
   - Show device compatibility check results
   - Display storage warning (3.4 GB required)
   - Show download progress bar
   - Handle errors with retry button
   - Navigate to main app when complete

6. **Implement ModelDownloader**
   ```kotlin
   class ModelDownloader(
       private val context: Context,
       private val httpClient: OkHttpClient
   ) {
       suspend fun downloadModel(
           url: String,
           onProgress: (Int) -> Unit
       ): Result<File> {
           // Download to cache first
           // Validate checksum
           // Move to app files directory
           // Return file path
       }
   }
   ```

### Phase 3: Device Capability Detection (Day 1 Afternoon)

7. **Implement DeviceCapabilityChecker**
   ```kotlin
   class DeviceCapabilityChecker(private val context: Context) {

       data class Capability(
           val hasEnoughRAM: Boolean,
           val hasEnoughStorage: Boolean,
           val hasMinAPILevel: Boolean,
           val isCapable: Boolean
       )

       fun checkCapability(): Capability {
           val totalRAM = getTotalRAM()
           val freeStorage = getAvailableStorage()
           val apiLevel = Build.VERSION.SDK_INT

           return Capability(
               hasEnoughRAM = totalRAM >= 4096, // 4GB
               hasEnoughStorage = freeStorage >= 5 * 1024 * 1024 * 1024L, // 5GB
               hasMinAPILevel = apiLevel >= 31, // Android 12
               isCapable = totalRAM >= 4096 && freeStorage >= 5 * 1024 * 1024 * 1024L && apiLevel >= 31
           )
       }

       private fun getTotalRAM(): Long {
           val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
           val memInfo = ActivityManager.MemoryInfo()
           activityManager.getMemoryInfo(memInfo)
           return memInfo.totalMem / (1024 * 1024) // MB
       }
   }
   ```

### Phase 4: On-Device Inference Integration (Day 2 Morning)

8. **Update GemmaInference to Load Downloaded Model**
   ```kotlin
   class GemmaInference(private val context: Context) {

       private var llmInference: LlmInference? = null

       fun initialize(): Result<Unit> {
           return try {
               val modelPath = context.filesDir.resolve("gemma/gemma-3n-e2b.bin")

               if (!modelPath.exists()) {
                   return Result.failure(Exception("Model not downloaded"))
               }

               val options = LlmInference.LlmInferenceOptions.builder()
                   .setModelPath(modelPath.absolutePath)
                   .setMaxTokens(1024)
                   .setTemperature(0.7f)
                   .setTopK(40)
                   .build()

               llmInference = LlmInference.createFromOptions(context, options)

               Result.success(Unit)
           } catch (e: Exception) {
               Result.failure(e)
           }
       }

       suspend fun generateResponse(prompt: String): Result<String> {
           val inference = llmInference ?: return Result.failure(Exception("Model not initialized"))

           return withContext(Dispatchers.IO) {
               try {
                   val response = inference.generateResponse(prompt)
                   Result.success(response)
               } catch (e: Exception) {
                   Result.failure(e)
               }
           }
       }
   }
   ```

9. **Configure Inference Parameters**
   - Temperature: 0.7 (balanced creativity/accuracy)
   - Max Tokens: 1024 (sufficient for scoring + chat)
   - Top K: 40 (quality responses)
   - Context Length: 2048 tokens

### Phase 5: Chat Interface Integration (Day 2 Morning)

10. **Update ChatViewModel**
    ```kotlin
    class ChatViewModel(
        private val gemmaInference: GemmaInference,
        private val tenderRepository: TenderRepository
    ) : ViewModel() {

        private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        fun sendMessage(message: String, tenderId: String?) {
            viewModelScope.launch {
                // Add user message
                _messages.value += ChatMessage.User(message)
                _isLoading.value = true

                // Get tender context if tenderId provided
                val context = tenderId?.let { tenderRepository.getTender(it) }

                // Build prompt with context
                val prompt = buildPrompt(message, context)

                // Generate response
                when (val result = gemmaInference.generateResponse(prompt)) {
                    is Result.Success -> {
                        _messages.value += ChatMessage.AI(result.data, source = "On-device AI")
                    }
                    is Result.Error -> {
                        _messages.value += ChatMessage.Error("Failed to generate response: ${result.error}")
                    }
                }

                _isLoading.value = false
            }
        }
    }
    ```

### Phase 6: Tender Scoring Integration (Day 2 Afternoon)

11. **Update ScoringService**
    ```kotlin
    class ScoringService(
        private val gemmaInference: GemmaInference
    ) {

        suspend fun scoreTender(tender: Tender, userProfile: UserProfile): Result<TenderScore> {
            val prompt = """
            Analyze this tender and score its relevance (0-100) to the user profile.

            Tender:
            - Title: ${tender.title}
            - Description: ${tender.description}
            - Requirements: ${tender.requirements}

            User Profile:
            - Industry: ${userProfile.industry}
            - Capabilities: ${userProfile.capabilities.joinToString()}
            - Previous wins: ${userProfile.previousWins.joinToString()}

            Provide:
            1. Relevance score (0-100)
            2. Brief explanation (2-3 sentences)
            3. Key matching factors

            Format: SCORE: [number]\nEXPLANATION: [text]
            """.trimIndent()

            return when (val result = gemmaInference.generateResponse(prompt)) {
                is Result.Success -> {
                    parseScoringResponse(result.data)
                }
                is Result.Error -> {
                    Result.failure(result.error)
                }
            }
        }

        private fun parseScoringResponse(response: String): Result<TenderScore> {
            // Parse LLM response into structured TenderScore
            // Extract score, explanation, confidence
        }
    }
    ```

### Phase 7: Testing & Validation (Day 2 Afternoon)

12. **Test on Real Device**
    - Install APK on Android 12+ device with 4GB+ RAM
    - Run through onboarding flow
    - Verify model downloads successfully
    - Test chat with various prompts
    - Test tender scoring on real tender data
    - Measure inference latency (target <5s chat, <10s scoring)
    - Monitor memory usage (target <1GB additional)

13. **Visual Verification**
    - Screenshot onboarding flow
    - Screenshot chat with AI responses
    - Screenshot tender detail with score
    - Save to `screenshots/v1.0.4-phase1/`

14. **Performance Benchmarks**
    - Measure download time (3.4 GB over Wi-Fi)
    - Measure model load time (target <30s)
    - Measure inference latency (P50, P95, P99)
    - Measure memory footprint (baseline vs with model)

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| **Model too large for devices** | Medium | High | Show storage warning before download; enable deletion to free space in future |
| **Inference too slow** | Medium | Medium | Set expectation with loading indicators; optimize prompt lengths; consider smaller model variant in Phase 2 |
| **Model download fails** | Medium | Medium | Implement pause/resume; retry logic; checksum validation; fallback to manual download link |
| **Low-end devices crash** | Low | High | Device capability check before download; graceful degradation with clear error message |
| **MediaPipe compatibility issues** | Low | High | Test on multiple devices early; have backup model format ready |

---

## Success Metrics

### Functional Metrics
- ✅ Model downloads successfully on ≥90% of attempts
- ✅ On-device inference works on ≥80% of Android 12+ devices with 4GB+ RAM
- ✅ Chat generates coherent responses (manual validation)
- ✅ Scoring accuracy ≥70% (manually validated on 10 tenders)

### Performance Metrics
- ✅ Chat inference latency: <5s (P95)
- ✅ Scoring inference latency: <10s (P95)
- ✅ Model loading time: <30s
- ✅ Memory overhead: <1GB additional RAM
- ✅ Model download time: <10 minutes over Wi-Fi (3.4 GB)

### User Experience Metrics
- ✅ Onboarding completion rate: >70%
- ✅ Zero crashes during model download
- ✅ Zero crashes during inference
- ✅ Clear error messages for unsupported devices

---

## Timeline

### Day 1: Model & Onboarding
- **Morning (3 hours)**: Download model, host on CDN, generate checksum
- **Afternoon (4 hours)**: Implement onboarding flow, device capability check, model downloader

### Day 2: Inference & Testing
- **Morning (4 hours)**: Integrate on-device inference, connect chat interface, implement scoring
- **Afternoon (3 hours)**: Test on real device, performance benchmarks, visual verification

**Total**: 14 hours over 2 days

---

## Rollback Procedure

If critical issues discovered:

1. **Disable onboarding flow**:
   ```kotlin
   // In OnboardingActivity
   if (BuildConfig.LLM_ENABLED == false) {
       navigateToMain()
   }
   ```

2. **Show feature unavailable message**:
   - Chat screen: "AI features coming soon"
   - Scoring: Hide score section, show "Scoring unavailable"

3. **Release hotfix APK** without LLM integration

4. **Investigate and fix** in separate branch

---

## Dependencies

### External Dependencies
- Gemma 3n E2B model (3.4 GB) from Google AI Edge
- CDN or public URL for model hosting (Google Cloud Storage / Hugging Face)
- MediaPipe Tasks GenAI library (already in `build.gradle`)

### Code Dependencies
- Existing `GemmaInference` class
- Existing `ChatViewModel` and `ChatScreen`
- Existing `TenderDetailScreen`
- Existing Hilt DI setup

### Infrastructure Dependencies
- CDN for model hosting (new requirement)
- Android device with 4GB+ RAM and Android 12+ for testing

---

## References

- **Gemma Model**: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference
- **MediaPipe LLM Inference**: https://developers.google.com/mediapipe/solutions/genai/llm_inference/android
- **Existing Code**: `android/app/src/main/java/com/tendertracker/app/gemma/GemmaInference.kt`
- **PROJECT_STATUS.md**: Current production readiness: 75%
- **LESSONS_LEARNED.md**: No prior LLM deployment lessons (this will be first)

---

## Configuration Answers

### 1. Model Hosting ✅ RESOLVED

**Using existing Hugging Face hosted model:**
- **Primary URL**: `https://huggingface.co/na5h13/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm`
- **Fallback URL**: `https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/ba9ca88da013b537b6ed38108be609b8db1c3a16/gemma-3n-E2B-it-int4.litertlm`
- **Size**: 3.4 GB (LiteRT LM format)
- **Format**: `.litertlm` (LiteRT optimized, not `.bin`)

**Status**: Already configured in `GemmaModelRegistry.kt` - no changes needed.

### 2. Download Timing ✅ CONFIRMED

**Option A: During onboarding (first launch)**

**Rationale**:
- Sets clear expectations upfront (3.4 GB download required)
- User makes conscious decision to enable AI features
- Prevents surprise downloads during critical workflows
- Allows users to skip if not interested in AI features
- Better UX than "feature suddenly unavailable" mid-session

**Implementation**:
- Show onboarding screen on first app launch (after login)
- Display storage warning: "AI features require 3.4 GB download"
- User taps "Download AI Model" to proceed or "Skip" to continue without AI
- Download happens with clear progress indicator
- User can pause/resume or restart download
- After completion, AI features become available

### 3. Graceful Degradation ✅ EXPANDED

Three-tier degradation strategy based on device capability:

#### **Tier 1: Full AI Features (Supported Devices)**

**Requirements**:
- Android 12+ (API 31+)
- 8GB+ RAM (as per model spec)
- 5GB+ available storage
- GPU support (optional but recommended)

**User Experience**:
- Full onboarding flow with model download
- All AI features enabled:
  - Chat with AI assistant
  - Tender relevance scoring (0-100)
  - Tender analysis and extraction
  - Natural language Q&A about tenders
- "🤖 On-device AI" badge visible in UI
- Inference happens locally, no network required after download

---

#### **Tier 2: Degraded Experience (Mid-Tier Devices)**

**Device Characteristics**:
- Android 12+ but only 4-6GB RAM
- Or insufficient storage (2-5GB free)
- Or older GPU

**Strategy**: **Conditional Features**

**User Experience**:
1. **During Onboarding**:
   - Warning: "Your device has 4GB RAM. AI features may be slower."
   - Option to proceed with download or skip
   - If user proceeds:
     - Model downloads normally
     - Additional warning: "AI features will use 1-2GB RAM. Close other apps for best performance."

2. **During Use**:
   - Chat works but may be slower (8-10s response time vs 3-5s)
   - Scoring works but may take 15-20s vs 5-10s
   - Show warning banner: "⚠️ AI features may be slow on this device"
   - Provide "Disable AI Features" toggle in settings to free RAM

3. **If Model Fails to Load** (insufficient RAM at runtime):
   - Graceful fallback: Disable AI features automatically
   - Show message: "AI features temporarily disabled to preserve device performance"
   - Offer "Try Again" button in settings

**Why Not Cloud Fallback Here?**:
- Requires backend LLM deployment (out of scope for Phase 1)
- Adds complexity (dual inference paths)
- Defeats privacy benefit of on-device AI
- Can be added in Phase 2 if needed

---

#### **Tier 3: No AI Features (Unsupported Devices)**

**Device Characteristics**:
- Android 11 or lower (API <31)
- Less than 4GB RAM
- Less than 2GB available storage

**Strategy**: **Disable with Clear Communication**

**User Experience**:
1. **During Onboarding**:
   - Show compatibility check screen
   - Red X icon with clear message:
     - If RAM insufficient: "Your device has 2GB RAM. AI features require at least 4GB RAM."
     - If storage insufficient: "Not enough storage. AI features require 5GB free space. Free up storage and try again."
     - If Android too old: "AI features require Android 12 or newer. Your device is running Android 11."
   - Primary button: "Continue Without AI"
   - Secondary link: "Learn More" (explains what AI features do, device requirements)

2. **Throughout App**:
   - AI features hidden from navigation (no chat tab, no scoring button)
   - App works fully for core functionality:
     - View tenders
     - Search tenders
     - Filter by source/date
     - View PDF attachments
   - Subtle banner in settings: "AI features unavailable. Requires Android 12+ and 4GB+ RAM."

3. **Future Upgrade Path**:
   - If user upgrades to supported device or OS, show prompt: "Your device now supports AI features! Download AI model to enable."

---

#### **Implementation Strategy**

**Device Detection** (`DeviceCapabilityChecker.kt`):
```kotlin
data class Capability(
    val tier: DeviceTier,
    val hasMinAPILevel: Boolean,
    val hasEnoughRAM: Boolean,
    val hasEnoughStorage: Boolean,
    val warningMessage: String?
)

enum class DeviceTier {
    FULL_SUPPORT,      // Tier 1: 8GB+ RAM, Android 12+
    DEGRADED,          // Tier 2: 4-6GB RAM, may be slow
    UNSUPPORTED        // Tier 3: <4GB RAM or Android <12
}

fun checkCapability(): Capability {
    val ramMB = getTotalRAM()
    val apiLevel = Build.VERSION.SDK_INT
    val storageBytes = getAvailableStorage()

    return when {
        apiLevel < 31 -> Capability(
            tier = DeviceTier.UNSUPPORTED,
            hasMinAPILevel = false,
            hasEnoughRAM = ramMB >= 4096,
            hasEnoughStorage = storageBytes >= 5_000_000_000L,
            warningMessage = "AI features require Android 12 or newer"
        )
        ramMB < 4096 -> Capability(
            tier = DeviceTier.UNSUPPORTED,
            hasMinAPILevel = true,
            hasEnoughRAM = false,
            hasEnoughStorage = storageBytes >= 5_000_000_000L,
            warningMessage = "AI features require at least 4GB RAM"
        )
        storageBytes < 2_000_000_000L -> Capability(
            tier = DeviceTier.UNSUPPORTED,
            hasMinAPILevel = true,
            hasEnoughRAM = true,
            hasEnoughStorage = false,
            warningMessage = "Not enough storage (need 5GB free)"
        )
        ramMB < 8192 -> Capability(
            tier = DeviceTier.DEGRADED,
            hasMinAPILevel = true,
            hasEnoughRAM = true,
            hasEnoughStorage = storageBytes >= 5_000_000_000L,
            warningMessage = "AI features may be slower on this device (${ramMB/1024}GB RAM detected)"
        )
        else -> Capability(
            tier = DeviceTier.FULL_SUPPORT,
            hasMinAPILevel = true,
            hasEnoughRAM = true,
            hasEnoughStorage = storageBytes >= 5_000_000_000L,
            warningMessage = null
        )
    }
}
```

**Onboarding Flow Updates**:
- Tier 1: Standard onboarding with download
- Tier 2: Onboarding with performance warning, user choice to proceed
- Tier 3: Capability check → Skip onboarding → Navigate directly to main app

**Settings Toggle** (for Tier 2):
```kotlin
// In SettingsScreen.kt
SwitchPreference(
    title = "AI Features",
    summary = if (aiEnabled) "On-device AI enabled" else "Disabled to improve performance",
    checked = aiEnabled,
    onCheckedChange = { enabled ->
        if (enabled) {
            // Re-initialize model
            gemmaInference.initialize()
        } else {
            // Release model from memory
            gemmaInference.release()
        }
    }
)
```

---

#### **Why This Approach?**

**User-Centric**:
- Clear communication about what works and what doesn't
- No silent failures or confusing error messages
- Users understand why AI features are unavailable
- Option to enable/disable on mid-tier devices (user control)

**Performance**:
- Prevents crashes on low-RAM devices
- Allows mid-tier devices to opt-in with warnings
- High-tier devices get optimal experience

**Maintainability**:
- Single codebase with feature flags
- No separate APK variants needed
- Easy to adjust thresholds based on real-world data

**Future-Proof**:
- Can add cloud fallback in Phase 2 for Tier 2/3 devices
- Can add model compression in Phase 2 for Tier 2
- Can add progressive download (smaller model first) in Phase 3

---

**Status**: ⏸️ **WAITING FOR APPROVAL**

**Next Steps**:
1. User reviews this revised proposal
2. User answers open questions (model hosting, download timing, degradation)
3. User approves with "approved" or "proceed"
4. Run `/openspec-apply phase-1-llm-deployment` to implement
5. Complete all tasks with TDD approach
6. Run `/openspec-archive phase-1-llm-deployment` when complete
