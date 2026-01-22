# Phase 1 LLM Deployment - Testing Guide

**Status**: 95% Implementation Complete, Ready for Device Testing
**Date**: 2026-01-22
**Version**: 1.0.3

---

## Overview

Phase 1 implements on-device AI capabilities using Google's Gemma 3n E2B model (2B parameters, INT4 quantized). All AI processing happens locally on the Android device with no cloud dependency.

**Implementation Progress: 95% Complete**
- ✅ All code implemented and committed
- ✅ CI build passing
- ⚠️ End-to-end device testing pending (5% remaining)

---

## Prerequisites

### Device Requirements

**Minimum (Tier 2 - Degraded Performance)**:
- Android 12+ (API 31+)
- 4GB RAM
- 5GB free storage
- WiFi connection (for initial model download)

**Recommended (Tier 1 - Optimal Performance)**:
- Android 12+ (API 31+)
- 8GB+ RAM
- 5GB free storage
- WiFi connection (for initial model download)

### Installation

1. **Download Latest APK from GitHub Actions**:
   ```bash
   gh run list --limit 1
   gh run download <run-id> -n app-debug
   ```

2. **Install on Device**:
   ```bash
   adb devices  # Verify device connected
   adb install -r app-debug.apk
   ```

3. **Launch App**:
   - Find "Tender Tracker Pro" in app drawer
   - Create account or login

---

## Testing Checklist

### ✅ Test 1: Device Capability Detection

**Purpose**: Verify 3-tier device detection works correctly

**Steps**:
1. Launch app and login
2. Navigate to **Settings** → **AI Models**
3. Observe device capability message at top of screen

**Expected Results**:
- **Tier 1 (8GB+ RAM)**: No warning, shows "Your device is optimized for AI features"
- **Tier 2 (4-6GB RAM)**: Warning: "AI features may be slower on this device (XGB RAM detected)"
- **Tier 3 (<4GB or Android <12)**: Error: "AI features require at least 4GB RAM" or "AI features require Android 12 or newer"

**Pass Criteria**:
- [ ] Device capability correctly detected
- [ ] Appropriate message shown based on device specs
- [ ] RAM and storage info displayed accurately

---

### ✅ Test 2: Model Download

**Purpose**: Verify model downloads successfully with progress tracking

**Steps**:
1. Navigate to **Settings** → **AI Models**
2. Find "Gemma 3n E2B (Multimodal)" card
3. Tap **Download** button (gradient purple button)
4. Observe download progress

**Expected Results**:
- Progress bar appears with percentage (0-100%)
- Progress updates smoothly every few seconds
- Download completes successfully (3.4 GB file)
- Status changes to "Downloaded" when complete

**Pass Criteria**:
- [ ] Download starts without errors
- [ ] Progress bar updates (not stuck at 0%)
- [ ] Download completes successfully
- [ ] App remains responsive during download
- [ ] Download survives app backgrounding
- [ ] File exists at `/data/data/com.tendertracker.app/files/models/gemma-3n-e2b/model.litertlm`

**Performance Metrics** (Record for benchmarking):
- Download time: __________ minutes (on WiFi)
- Download speed: __________ MB/s

**Troubleshooting**:
- If download fails, check **fallback URL** is tried
- If progress stuck, check network connection
- If storage full, free up space and retry

---

### ✅ Test 3: Model Loading

**Purpose**: Verify model loads into memory successfully

**Steps**:
1. After download completes, tap **Load Model** button
2. Observe loading indicator
3. Wait for status to change to "Ready"

**Expected Results**:
- Circular progress indicator appears
- Status changes to "Ready" with green checkmark
- Loading completes within 30 seconds

**Pass Criteria**:
- [ ] Model loads without errors
- [ ] Status shows "Ready" with green checkmark icon
- [ ] Loading completes in <30 seconds
- [ ] No app crashes or ANR (Application Not Responding)

**Performance Metrics**:
- Model load time: __________ seconds

---

### ✅ Test 4: Chat Interface - General Questions

**Purpose**: Verify AI chat works for general tender questions

**Steps**:
1. Navigate to **Chat** screen (bottom navigation)
2. Type: "What is a government tender?"
3. Tap send and observe response

**Expected Results**:
- "AI is typing..." indicator appears
- Response generates within 5 seconds (Tier 1) or 10 seconds (Tier 2)
- Response is coherent and relevant
- Message appears in chat history

**Pass Criteria**:
- [ ] Chat input works
- [ ] Typing indicator shows during generation
- [ ] Response generated successfully
- [ ] Response is coherent and relevant to question
- [ ] No errors or crashes

**Additional Test Prompts**:
- "How do I find tenders?"
- "What information is in a tender document?"
- "Explain tender requirements"

**Performance Metrics**:
- Chat inference time: __________ seconds (P50), __________ seconds (P95)

---

### ✅ Test 5: Chat Interface - Tender Context

**Purpose**: Verify chat uses tender context when navigating from detail screen

**Steps**:
1. Navigate to **Tenders** → Select any tender
2. On Tender Detail screen, tap **"Ask AI"** button
3. Chat screen opens with tender context set
4. Type: "What is this tender about?"
5. Observe response

**Expected Results**:
- Chat screen opens automatically
- AI response references the specific tender
- Response includes tender title, reference, or requirements
- Context-aware responses (not generic)

**Pass Criteria**:
- [ ] Navigation from TenderDetail → Chat works
- [ ] AI response mentions specific tender details
- [ ] Response is context-aware (not generic)
- [ ] Tender context persists across multiple messages

**Additional Test Prompts** (with tender context):
- "Summarize the requirements"
- "What is the deadline?"
- "Is this relevant for my business?"

---

### ✅ Test 6: Tender Scoring

**Purpose**: Verify on-device tender scoring works correctly

**Steps**:
1. Navigate to **Tenders** → Select any tender
2. Scroll to score section (if tender has no score, continue)
3. Tap **"Re-score"** button
4. Observe scoring process

**Expected Results**:
- Loading indicator appears during scoring
- Score generated within 10 seconds (Tier 1) or 15 seconds (Tier 2)
- Score displayed as number (0-100)
- Explanation text appears below score
- Tender detail updates with new score

**Pass Criteria**:
- [ ] Re-score button works
- [ ] Loading indicator shows during scoring
- [ ] Score generated successfully (0-100)
- [ ] Explanation is relevant to tender content
- [ ] Score persists after closing and reopening tender

**Additional Tests**:
- Score multiple different tenders (variety of content)
- Compare scores (high-value tenders should score higher)
- Verify score explanation makes sense

**Performance Metrics**:
- Scoring inference time: __________ seconds (P50), __________ seconds (P95)

---

### ✅ Test 7: Memory and Battery Usage

**Purpose**: Verify app doesn't consume excessive resources

**Steps**:
1. Open Android Settings → Developer Options → Memory
2. Find "Tender Tracker Pro" in running services
3. Perform chat and scoring operations
4. Monitor memory usage

**Expected Results** (Tier 1, 8GB RAM device):
- App memory usage: <500 MB idle, <2 GB during inference
- No memory leaks (memory returns to baseline after inference)
- No app crashes due to OOM (Out of Memory)
- Battery drain reasonable (<10% per hour of active use)

**Pass Criteria**:
- [ ] Memory usage stays within reasonable limits
- [ ] No app crashes during extended use
- [ ] Memory released after inference completes
- [ ] Battery drain acceptable

**Performance Metrics**:
- Idle memory: __________ MB
- Active inference memory: __________ MB
- Battery drain: __________% per hour

---

### ✅ Test 8: Error Handling

**Purpose**: Verify graceful error handling when model not loaded

**Steps**:
1. Navigate to **Settings** → **AI Models**
2. If model is loaded, tap **Delete** (trash icon)
3. Navigate to **Chat** screen
4. Try to send a message
5. Observe error handling

**Expected Results**:
- Error message: "AI model not loaded. Go to Settings > AI Models to download."
- App doesn't crash
- User can still navigate normally

**Pass Criteria**:
- [ ] Error message shown when model not loaded
- [ ] Error message actionable (tells user what to do)
- [ ] No app crashes
- [ ] Chat input disabled or shows placeholder

---

### ✅ Test 9: Model Management

**Purpose**: Verify all model management operations work

**Steps**:
1. Navigate to **Settings** → **AI Models**
2. Test all model operations:
   - Download (if not downloaded)
   - Load (if downloaded but not loaded)
   - Delete (if downloaded)

**Expected Results**:
- Download: Progress tracking, successful completion
- Load: Loading indicator, model becomes ready
- Delete: Confirmation dialog (?), model removed, storage freed

**Pass Criteria**:
- [ ] Download works (tested in Test 2)
- [ ] Load works (tested in Test 3)
- [ ] Delete works and frees storage
- [ ] Can re-download after delete
- [ ] Model state persists after app restart

---

### ✅ Test 10: Edge Cases

**Purpose**: Test unusual or edge case scenarios

**Edge Case Tests**:

1. **App Restart During Download**:
   - Start model download
   - Force stop app (Settings → Apps → Tender Tracker Pro → Force Stop)
   - Relaunch app
   - **Expected**: Download resumes from last checkpoint

2. **Network Loss During Download**:
   - Start model download
   - Turn off WiFi mid-download
   - Turn WiFi back on
   - **Expected**: Download resumes or shows retry option

3. **Very Long Chat Session**:
   - Send 20+ messages in chat
   - **Expected**: Chat history scrolls correctly, no memory issues

4. **Multiple Tender Scorings**:
   - Score 5-10 different tenders rapidly
   - **Expected**: All scores generate correctly, no queuing issues

**Pass Criteria**:
- [ ] Download resume works
- [ ] Network loss handled gracefully
- [ ] Long chat sessions work
- [ ] Multiple scorings work

---

## Performance Benchmarks

**Target Performance** (to be verified on real devices):

| Metric | Tier 1 (8GB+) | Tier 2 (4-6GB) |
|--------|---------------|----------------|
| Model Download | 10-15 min (WiFi) | 10-15 min (WiFi) |
| Model Load Time | <20s | <30s |
| Chat Inference (P95) | <5s | <10s |
| Scoring Inference (P95) | <10s | <15s |
| Memory Usage (Inference) | <2GB | <2GB |
| Memory Usage (Idle) | <500MB | <500MB |

**Record Actual Performance**:

- Device: _________________ (model, RAM, Android version)
- Model Download Time: __________ minutes
- Model Load Time: __________ seconds
- Chat Inference (P50): __________ seconds
- Chat Inference (P95): __________ seconds
- Scoring Inference (P50): __________ seconds
- Scoring Inference (P95): __________ seconds
- Memory Usage (Idle): __________ MB
- Memory Usage (Inference): __________ MB
- Battery Drain: __________% per hour

---

## Known Limitations

1. **First Response Latency**: First inference after model load may be slower (model warming up)
2. **Download Interruption**: If download is cancelled mid-way, partial file needs to be deleted manually
3. **Model Size**: 3.4 GB is large; ensure WiFi for download
4. **Device Requirements**: Tier 3 devices (<4GB RAM) cannot use AI features

---

## Troubleshooting

### Issue: "Download Failed"
**Solution**:
- Check internet connection
- Verify 5GB free storage
- Try fallback URL (automatic)
- Check Logcat for specific error

### Issue: "Model Load Failed"
**Solution**:
- Verify model file exists and is complete (3.4 GB)
- Check RAM availability (close other apps)
- Clear app cache and retry
- Re-download model if corrupted

### Issue: "Inference Very Slow (>30s)"
**Solution**:
- Check device tier (may be Tier 2 or 3)
- Close background apps to free RAM
- Verify model is loaded (not loading on each request)
- Check CPU throttling due to heat

### Issue: "App Crashes During Inference"
**Solution**:
- Check memory usage (likely OOM)
- Verify device meets minimum requirements
- Update to latest APK version
- Report crash logs for investigation

---

## Reporting Test Results

After completing testing, please report:

1. **Device Information**:
   - Model: _________________
   - RAM: _________________
   - Android Version: _________________
   - Tier: _________________

2. **Test Results**:
   - Tests Passed: _____ / 10
   - Tests Failed: _____ / 10
   - Performance Benchmarks: (see table above)

3. **Issues Encountered**:
   - Issue 1: _________________
   - Issue 2: _________________
   - Issue 3: _________________

4. **Overall Assessment**:
   - [ ] Ready for production
   - [ ] Needs minor fixes
   - [ ] Needs major rework

---

## Next Steps After Testing

1. **If All Tests Pass** (100% complete):
   - Archive Phase 1 OpenSpec change: `/openspec-archive phase-1-llm-deployment`
   - Update PROJECT_STATUS.md: Production readiness 80% → 90%
   - Move to Phase 2 (future features)

2. **If Tests Fail**:
   - Document failures in this guide
   - Investigate root cause
   - Implement fixes
   - Re-test

3. **Performance Optimization** (if needed):
   - Optimize prompts for shorter generation
   - Reduce context window if too slow
   - Implement response caching
   - Add streaming UI for longer responses

---

## Success Criteria

Phase 1 is considered **100% complete** when:

- [ ] All 10 tests pass on at least 2 different devices (1 Tier 1, 1 Tier 2)
- [ ] Performance benchmarks meet or exceed targets
- [ ] No critical bugs or crashes
- [ ] User experience is smooth and responsive
- [ ] Documentation is complete and accurate

**Current Status**: 95% Implementation Complete → Waiting for device testing to reach 100%

---

**End of Testing Guide**

For questions or issues, refer to:
- `README.md` - General project documentation
- `PROJECT_STATUS.md` - Current status and roadmap
- `LESSONS_LEARNED.md` - Development patterns and best practices
