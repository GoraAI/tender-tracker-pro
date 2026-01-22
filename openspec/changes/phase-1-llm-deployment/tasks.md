# Tasks: Phase 1 - LLM Deployment (Android On-Device)

**Status**: PROPOSED
**Total Tasks**: 28 tasks (revised from 45)
**Estimated Time**: 1-2 days

---

## Phase 1: Model Acquisition & Hosting (Day 1 Morning)

### Model Setup Tasks (3 tasks)

- [ ] **Task 1.1**: Verify existing model configuration ✅ ALREADY DONE
  - Model: Gemma 3n E2B (2B params, INT4 quantized, LiteRT format)
  - Format: `.litertlm` (3.4 GB)
  - Primary URL: `https://huggingface.co/na5h13/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm`
  - Fallback URL: `https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/ba9ca88da013b537b6ed38108be609b8db1c3a16/gemma-3n-E2B-it-int4.litertlm`
  - Status: Already configured in `GemmaModelRegistry.kt`
  - Acceptance: Verify URLs are accessible
  - Acceptance: Test download speed from Android device

- [ ] **Task 1.2**: Test model download infrastructure
  - Code already exists: `ModelDownloadManager.kt` and `ModelDownloadWorker.kt`
  - Test download from Hugging Face URL
  - Verify fallback URL works if primary fails
  - Verify checksum validation (if implemented)
  - Acceptance: Model downloads successfully to `/data/data/.../files/models/gemma-3n-e2b/model.litertlm`
  - Acceptance: Download progress tracking works

- [ ] **Task 1.3**: Review and update model configuration if needed
  - Review `GemmaModelRegistry.kt` for accuracy
  - Verify RAM requirements (currently set to 8GB minimum)
  - Update if needed based on real device testing
  - Acceptance: Configuration matches actual model requirements
  - Acceptance: Constants documented

---

## Phase 2: Device Capability Detection (Day 1 Afternoon)

### Capability Detection Tasks (3 tasks)

- [ ] **Task 2.1**: Implement `DeviceCapabilityChecker`
  - Check: Total RAM ≥ 4GB
  - Check: Available storage ≥ 5GB
  - Check: Android API level ≥ 31 (Android 12)
  - Acceptance: Returns `Capability` data class with boolean flags
  - Acceptance: Unit test passes for various device configs

- [ ] **Task 2.2**: Add RAM detection logic
  - Use `ActivityManager.MemoryInfo` to get total RAM
  - Convert to MB for comparison
  - Acceptance: Correctly detects 4GB+ devices
  - Acceptance: Logs RAM info for debugging

- [ ] **Task 2.3**: Add storage detection logic
  - Use `StatFs` to get available storage on internal storage
  - Calculate free space in bytes
  - Acceptance: Correctly detects ≥5GB free space
  - Acceptance: Shows user-friendly storage warning if insufficient

---

## Phase 3: Onboarding Flow Implementation (Day 1 Afternoon)

### Onboarding UI Tasks (5 tasks)

- [ ] **Task 3.1**: Create `OnboardingViewModel`
  - Sealed class `OnboardingState` (CheckingDevice, ReadyToDownload, Downloading, Complete, Error)
  - Function `checkDeviceAndDownload()`
  - Inject `DeviceCapabilityChecker` and `ModelDownloader`
  - Acceptance: ViewModel compiles and follows MVI pattern
  - Acceptance: Unit test covers all states

- [ ] **Task 3.2**: Create `OnboardingScreen` Composable
  - Show device capability check results
  - Display "3.4 GB required" storage warning
  - Show download button when ready
  - Display progress bar during download
  - Show completion checkmark and "Continue" button
  - Acceptance: UI compiles and renders correctly
  - Acceptance: Screenshot shows all states

- [ ] **Task 3.3**: Implement `ModelDownloader` service
  - Use `OkHttpClient` for download with progress callback
  - Download to cache directory first
  - Validate SHA256 checksum after download
  - Move to app files directory on success
  - Acceptance: Downloads 3.4 GB file successfully
  - Acceptance: Validates checksum correctly

- [ ] **Task 3.4**: Add download progress tracking
  - Update ViewModel state with download percentage (0-100)
  - Show progress in UI with `LinearProgressIndicator`
  - Display estimated time remaining (optional)
  - Acceptance: Progress updates smoothly
  - Acceptance: UI shows real-time progress

- [ ] **Task 3.5**: Implement download pause/resume
  - Save download state to SharedPreferences
  - Resume download from last byte on retry
  - Show "Resume" button if download interrupted
  - Acceptance: Download resumes after app restart
  - Acceptance: Partial download not re-downloaded

---

## Phase 4: Model Loading Integration (Day 2 Morning)

### Model Loading Tasks (4 tasks)

- [ ] **Task 4.1**: Update `GemmaInference` to load from storage
  - Check if model exists at `/data/data/.../files/gemma/gemma-3n-e2b.bin`
  - Load model using MediaPipe `LlmInference.createFromOptions()`
  - Set model path, max tokens, temperature, top K
  - Acceptance: Model loads successfully on real device
  - Acceptance: Model loading completes in <30 seconds

- [ ] **Task 4.2**: Configure inference parameters
  - Temperature: 0.7 (balanced creativity/accuracy)
  - Max Tokens: 1024 (sufficient for chat + scoring)
  - Top K: 40 (quality responses)
  - Context window: Use MediaPipe default (usually 2048)
  - Acceptance: Parameters documented in code
  - Acceptance: Inference generates coherent responses

- [ ] **Task 4.3**: Implement lazy model loading
  - Don't load model on app startup (too slow)
  - Load on first use (chat open or scoring request)
  - Show loading indicator during model initialization
  - Cache loaded model in memory (singleton pattern)
  - Acceptance: Model loads only once per app session
  - Acceptance: Subsequent inferences use cached model

- [ ] **Task 4.4**: Add error handling for model loading
  - Catch model file not found error → show onboarding again
  - Catch model corrupted error → re-download
  - Catch insufficient memory error → show graceful degradation
  - Acceptance: All error cases handled gracefully
  - Acceptance: User sees actionable error messages

---

## Phase 5: Chat Interface Integration (Day 2 Morning)

### Chat Integration Tasks (4 tasks)

- [ ] **Task 5.1**: Update `ChatViewModel` to use `GemmaInference`
  - Inject `GemmaInference` via Hilt
  - Call `generateResponse()` when user sends message
  - Update `_messages` StateFlow with AI response
  - Acceptance: Chat sends message and receives AI response
  - Acceptance: Response appears in UI within 5 seconds

- [ ] **Task 5.2**: Add loading state during inference
  - Show `CircularProgressIndicator` while generating response
  - Disable input field during generation
  - Show "AI is thinking..." text
  - Acceptance: Loading indicator appears immediately
  - Acceptance: Input re-enabled after response

- [ ] **Task 5.3**: Add "On-device AI" indicator
  - Display "🤖 On-device AI" below each AI message
  - Style with subtle color/size to indicate source
  - Acceptance: Indicator visible on all AI messages
  - Acceptance: Screenshot shows indicator

- [ ] **Task 5.4**: Implement error handling in chat
  - Show error message if inference fails
  - Add "Retry" button to re-send last message
  - Log errors for debugging
  - Acceptance: Error state renders correctly
  - Acceptance: Retry button works

---

## Phase 6: Tender Scoring Integration (Day 2 Afternoon)

### Scoring Integration Tasks (3 tasks)

- [ ] **Task 6.1**: Create `ScoringService` with LLM integration
  - Function `scoreTender(tender, userProfile): TenderScore`
  - Build prompt with tender details + user profile
  - Call `GemmaInference.generateResponse()`
  - Parse response to extract score (0-100) and explanation
  - Acceptance: Generates score for sample tender
  - Acceptance: Score is realistic (validated manually)

- [ ] **Task 6.2**: Update `TenderDetailScreen` to display score
  - Show score prominently (large number 0-100)
  - Display explanation text below score
  - Add "Analyze" button to trigger scoring
  - Show loading state during scoring
  - Acceptance: Score displayed correctly
  - Acceptance: Screenshot shows score UI

- [ ] **Task 6.3**: Add confidence indicator
  - If LLM response indicates low confidence, show warning icon
  - Add tooltip explaining confidence level
  - Acceptance: Low confidence scores show indicator
  - Acceptance: Tooltip accessible

---

## Phase 7: Testing & Validation (Day 2 Afternoon)

### Testing Tasks (4 tasks)

- [ ] **Task 7.1**: Test onboarding flow on real device
  - Install APK on Android 12+ device with 4GB+ RAM
  - Complete full onboarding flow
  - Verify model downloads successfully (3.4 GB)
  - Verify model persists after app restart
  - Acceptance: Onboarding completes without errors
  - Acceptance: Model file exists in app storage

- [ ] **Task 7.2**: Test chat interface with various prompts
  - Test: "What is this tender about?" (with tender context)
  - Test: "Summarize the requirements"
  - Test: "What is the deadline?"
  - Test: General questions like "What is a tender?"
  - Acceptance: All responses are coherent
  - Acceptance: Responses use tender context when provided

- [ ] **Task 7.3**: Test tender scoring accuracy
  - Score 10 sample tenders manually (human baseline)
  - Score same tenders with LLM
  - Calculate correlation (target ≥0.7)
  - Acceptance: LLM scores align with human judgment
  - Acceptance: Explanations are relevant

- [ ] **Task 7.4**: Performance benchmarking
  - Measure: Model download time over Wi-Fi
  - Measure: Model load time on mid-range device
  - Measure: Chat inference latency (P50, P95)
  - Measure: Scoring inference latency (P50, P95)
  - Measure: Memory usage during inference
  - Acceptance: All metrics meet targets (chat <5s P95, scoring <10s P95)
  - Acceptance: Metrics documented in completion report

---

## Phase 8: Documentation & Deployment (Day 2 End)

### Documentation Tasks (2 tasks)

- [ ] **Task 8.1**: Update README.md
  - Add: LLM deployment status (✅ deployed)
  - Add: Model information (Gemma 3n E2B, 3.4 GB)
  - Add: Performance benchmarks
  - Update: Features list to show AI capabilities
  - Acceptance: README accurately describes LLM integration

- [ ] **Task 8.2**: Update PROJECT_STATUS.md
  - Update: Production readiness 75% → 90%
  - Update: LLM integration 40% → 95%
  - Add: Phase 1 completion date
  - Add: Performance metrics
  - Acceptance: Status reflects reality
  - Acceptance: Metrics match benchmarks

---

## Summary

**Total Tasks**: 28 (Android only)
- Phase 1 (Model Acquisition): 3 tasks
- Phase 2 (Device Capability): 3 tasks
- Phase 3 (Onboarding Flow): 5 tasks
- Phase 4 (Model Loading): 4 tasks
- Phase 5 (Chat Integration): 4 tasks
- Phase 6 (Scoring Integration): 3 tasks
- Phase 7 (Testing): 4 tasks
- Phase 8 (Documentation): 2 tasks

**Critical Path**:
1. Tasks 1.1-1.3 (Model hosting) - Must complete first
2. Tasks 2.1-2.3 (Device capability) - Parallel with Phase 1
3. Tasks 3.1-3.5 (Onboarding) - Depends on Phase 1 & 2
4. Tasks 4.1-4.4 (Model loading) - Depends on Phase 3
5. Tasks 5.1-5.4 (Chat) & 6.1-6.3 (Scoring) - Parallel, depends on Phase 4
6. Tasks 7.1-7.4 (Testing) - Depends on Phase 5 & 6
7. Tasks 8.1-8.2 (Documentation) - Final phase

**Estimated Total Time**: 12-16 hours over 1-2 days

---

**Status**: ⏸️ **WAITING FOR APPROVAL**

All tasks are ready. Waiting for user to approve revised proposal before implementation begins.
