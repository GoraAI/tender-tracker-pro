# Tasks: Fix Scoring Pipeline (End-to-End)

**Status**: IMPLEMENTING
**Total Tasks**: 18 tasks across 4 phases
**Estimated Time**: 1 day

---

## Phase 1: Backend Critical Fixes (Morning)

### P0 Fixes — Unblock Scoring Path (5 tasks)

- [x] **Task 1.1**: Fix `result.category` → `result.relevance_category` in pipeline_service.py
  - File: `src/api/services/pipeline_service.py:532`
  - Change: `relevance_category=result.category` → `relevance_category=result.relevance_category`

- [x] **Task 1.2**: Fix bare imports in `relevance_scorer.py` and `scoring/models.py`
  - File: `src/scoring/relevance_scorer.py:14-15`
  - File: `src/scoring/models.py:7`
  - Changed to `from src.extraction.models` and `from src.llm`

- [x] **Task 1.3**: Add upsert logic for Score in both scoring paths
  - File: `src/api/routers/tenders.py` (manual scoring)
  - File: `src/api/services/pipeline_service.py` (LLM + keyword paths)
  - Pattern: select existing → update if found, insert if not

- [x] **Task 1.4**: Fix silent failure when `raw_text` is None in manual scoring
  - File: `src/api/routers/tenders.py:236`
  - Guard now checks `(not raw_text and not requirements_summary)`
  - Text source: `raw_text or requirements_summary or ""`

- [x] **Task 1.5**: Make RelevanceScorer accept criteria dict parameter
  - File: `src/scoring/relevance_scorer.py`
  - Added `criteria: dict | None = None` param with graceful YAML fallback
  - Pipeline passes user's DB criteria as dict
  - Fallback prompt when no template file exists

---

## Phase 2: Android Critical Fixes (Afternoon)

### P1 Fixes — Make Scoring Accessible and Persistent (4 tasks)

- [x] **Task 2.1**: Move Score/Re-score button outside `tender.score?.let {}` block
  - File: `TenderDetailScreen.kt`
  - Scored tenders: score card + "Re-score" button
  - Unscored tenders: "Score with AI" button card (glassmorphism)

- [x] **Task 2.2**: Wire CriteriaRepository into TenderDetailViewModel
  - Injected `CriteriaRepository`, fetch default criteria in `rescore()`
  - Builds profile string from capabilities, sectors, locations, keywords, value range
  - Graceful fallback to empty string if no criteria

- [x] **Task 2.3**: Call submitScore() after successful on-device scoring
  - Added `submitScoreToBackend()` to `TenderRepository` interface and impl
  - Fire-and-forget coroutine in `rescore()` onSuccess
  - Best-effort with error logging

- [x] **Task 2.4**: Protect local scores from null overwrite during sync
  - Added `getScoredTenderIds()` DAO query
  - `syncFromApi()` snapshots local scores, preserves when backend returns null

---

## Phase 3: Tests (Late Afternoon)

### Verify All Fixes (5 tasks)

- [x] **Task 3.1**: Run existing backend test suite
  - Result: 119 passed, 0 failed (was 114, +5 new)

- [x] **Task 3.2**: Add test for score upsert in tenders.py
  - `test_score_upsert_logic_direct` — PASSED

- [x] **Task 3.3**: Add test for scoring with only requirements_summary
  - `test_score_tender_with_only_requirements_summary_accepted` — PASSED

- [ ] **Task 3.4**: Add test for RelevanceScorer with criteria dict
  - Skipped: requires `llama_cpp` which is not installed locally

- [ ] **Task 3.5**: Verify Android builds
  - Pending: push to branch for CI

---

## Phase 4: Verification (End of Day)

### End-to-End Validation (4 tasks)

- [ ] **Task 4.1**: Deploy backend fixes to Railway
- [ ] **Task 4.2**: Test pipeline scoring end-to-end
- [ ] **Task 4.3**: Test manual scoring end-to-end
- [ ] **Task 4.4**: Test Android scoring end-to-end
