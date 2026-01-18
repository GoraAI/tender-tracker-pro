# FR-MOBILE-002: On-Device Gemma - Implementation Tasks

**Status**: COMPLETE
**Total Tasks**: 30/30

---

## Phase 1: MediaPipe Setup (4 tasks)

- [x] Add MediaPipe tasks-genai dependency to build.gradle
- [x] Create GemmaModelType enum (E2B, E4B)
- [x] Create GemmaConfig object with model URLs and settings
- [x] Verify MediaPipe initializes on test device

## Phase 2: Model Download (6 tasks)

- [x] Create ModelDownloadManager class
- [x] Implement downloadModel() with OkHttp streaming
- [x] Add download progress callback
- [x] Implement pause/resume with Range headers
- [x] Store models in getExternalFilesDir()
- [x] Track download state in DataStore preferences

## Phase 3: Model Loading (5 tasks)

- [x] Create GemmaManager class
- [x] Implement loadModel() with LlmInference.createFromOptions()
- [x] Implement unloadModel() with proper cleanup
- [x] Add checkMemory() before loading
- [x] Handle OutOfMemoryError gracefully

## Phase 4: Inference Engine (6 tasks)

- [x] Create GemmaInferenceEngine class
- [x] Implement generateText() with timeout
- [x] Create extraction prompt template
- [x] Implement extractTenderInfo() method
- [x] Create scoring prompt template
- [x] Implement scoreTenderRelevance() method

## Phase 5: Background Processing (4 tasks)

- [x] Create GemmaInferenceWorker extends CoroutineWorker
- [x] Implement batch extraction in worker
- [x] Implement batch scoring in worker
- [x] Add foreground notification for long-running work

## Phase 6: Testing (5 tasks)

- [x] Unit test: Prompt building
- [x] Unit test: JSON response parsing
- [x] Instrumented test: Model loading/unloading
- [x] Instrumented test: Basic inference with test prompt
- [x] Run all tests and fix failures

---

**Total**: 30/30 tasks complete
