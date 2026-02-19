# Proposal: Fix Scoring Pipeline (End-to-End)

**Status**: PROPOSED
**Priority**: P0 (CRITICAL - Scoring is the core value proposition)
**Created**: 2026-02-19
**Estimated Effort**: 1 day
**Impact**: Scoring goes from broken to fully functional across backend + Android

---

## Summary

Fix 9 bugs across the backend and Android app that collectively make scoring non-functional. The LLM scoring path has never successfully saved a score (AttributeError + import failures). The keyword fallback works once per tender but silently fails on re-score (UniqueConstraint). The Android app hides the Re-score button when tenders are unscored, never passes user criteria to Gemma, and never persists on-device scores to the backend.

---

## Motivation

### Current State (Broken)

**Backend**:
- LLM scoring path (`RelevanceScorer`) always crashes: bare imports (`from extraction.models` instead of `from src.extraction.models`), missing YAML config files, and `result.category` AttributeError (property is `result.relevance_category`)
- Falls back to keyword scoring, which works **once** then silently crashes on re-score due to `UniqueConstraint(tender_id, user_id, criteria_id)` with no upsert logic
- Manual `/tenders/{id}/score` silently does nothing when tender has `requirements_summary` but no `raw_text`

**Android**:
- Re-score button is **inside** `tender.score?.let {}` block — invisible for unscored tenders (line 200 of `TenderDetailScreen.kt`)
- `ScoringService.scoreTender()` receives `userProfile = ""` — user's criteria never fetched from `CriteriaRepository`
- `TenderApi.submitScore()` defined but **never called** — on-device Gemma scores saved to local Room only
- Next `syncFromApi()` overwrites local scores with backend values (often null) via `OnConflictStrategy.REPLACE`

### Desired State (Working)

- Backend LLM scoring path loads and saves scores correctly
- Re-scoring uses upsert logic (update existing, insert new)
- Manual scoring works with `raw_text` OR `requirements_summary`
- Android shows Score/Re-score button for ALL tenders regardless of existing score
- On-device scoring uses user's actual criteria from `CriteriaRepository`
- On-device scores are submitted to backend via `submitScore()` API
- Sync preserves locally-scored tenders (doesn't overwrite with null)

---

## Scope

### INCLUDED in This Change

1. **Backend Bug Fixes** (5 fixes in Python):
   - Fix `result.category` → `result.relevance_category` in `pipeline_service.py:532`
   - Fix bare imports in `relevance_scorer.py` and `scoring/models.py`
   - Add upsert logic for Score (replace `db.add()` with merge/upsert) in both `tenders.py` and `pipeline_service.py`
   - Fix `_score_tender` to accept `raw_text` OR `requirements_summary`
   - Make `RelevanceScorer` accept criteria dict parameter (not just YAML file)

2. **Android Bug Fixes** (4 fixes in Kotlin):
   - Move Re-score button (and add "Score" button) outside `tender.score?.let {}` block
   - Wire `CriteriaRepository` into `TenderDetailViewModel` to pass user criteria to `ScoringService`
   - Call `submitScore()` API after successful on-device scoring
   - Protect local scores from being overwritten by null backend values during sync

### EXCLUDED from This Change

- Adding new tender sources
- Changing the scoring algorithm or prompts
- Adding notification for scoring completion
- Modifying the chat system (already working)
- Backend CI pipeline (separate concern)

---

## Technical Approach

### Backend Fix 1: AttributeError in pipeline_service.py

**File**: `src/api/services/pipeline_service.py:532`
**Change**: `result.category` → `result.relevance_category`
**Reason**: `ScoredTender` (in `src/scoring/models.py:29`) defines `relevance_category` as a `@property`, not `category`. This has caused every LLM-scored tender to raise `AttributeError`, silently caught at line 546.

### Backend Fix 2: Import failures in relevance_scorer.py

**File**: `src/scoring/relevance_scorer.py:14-15` and `src/scoring/models.py:7`
**Change**:
```python
# relevance_scorer.py
from src.extraction.models import TenderInfo  # was: from extraction.models
from src.llm import ModelManager              # was: from llm

# scoring/models.py
from src.extraction.models import TenderInfo  # was: from extraction.models
```
**Reason**: Bare imports fail with `ModuleNotFoundError` in FastAPI's `src.*` package context.

### Backend Fix 3: Upsert logic for Score

**Files**: `src/api/routers/tenders.py:261-270` and `src/api/services/pipeline_service.py:526-535, 571-580`
**Change**: Before `db.add(score)`, check for existing score with same `(tender_id, user_id, criteria_id)`. If exists, update `score`, `explanation`, `relevance_category`, `scored_at`. If not, insert new.
**Reason**: `UniqueConstraint` on `(tender_id, user_id, criteria_id)` causes `IntegrityError` on second score, silently caught. Scores can never be updated.

### Backend Fix 4: Silent failure on missing raw_text

**File**: `src/api/routers/tenders.py:236`
**Change**: `if not _t or not _t.raw_text:` → `if not _t or (not _t.raw_text and not _t.requirements_summary):`
Also use `_t.raw_text or _t.requirements_summary or ""` as the text source for keyword matching.
**Reason**: The HTTP guard at line 226 allows tenders with only `requirements_summary`, but the background task requires `raw_text`. This causes a silent no-op after the user sees "Scoring queued".

### Backend Fix 5: RelevanceScorer criteria parameter

**File**: `src/scoring/relevance_scorer.py:24-57`
**Change**: Allow `__init__` to accept an optional `criteria` dict parameter that overrides YAML file loading. If `criteria` is passed, skip YAML. If not, fall back to YAML (with graceful error if file missing).
**Reason**: Pipeline should pass the user's DB `UserCriteria` to the scorer, not rely on a static YAML file that may not exist in production.

### Android Fix 1: Score button visibility

**File**: `android/app/.../ui/screens/tender/TenderDetailScreen.kt:200-247`
**Change**: Restructure the score card section:
- If `tender.score != null`: show the score card with score badge + "Re-score" button (current behavior)
- If `tender.score == null`: show a "Score with AI" button card that triggers `viewModel.rescore()`
Both paths render outside the `tender.score?.let {}` block so scoring is always accessible.

### Android Fix 2: Pass user criteria to ScoringService

**File**: `android/app/.../ui/screens/tender/TenderDetailViewModel.kt`
**Change**: Inject `CriteriaRepository` into `TenderDetailViewModel`. In `rescore()`, fetch the default criteria via `criteriaRepository.getCriteria()`, find `isDefault == true`, build a profile string from its capabilities/sectors/locations/keywords, and pass it to `scoringService.scoreTender(tender, profileString)`.

### Android Fix 3: Submit score to backend

**File**: `android/data/.../repository/TenderRepositoryImpl.kt`
**Change**: Add a `submitScore()` method that calls `tenderApi.submitScore(tenderId, ScoreSubmission(score, explanation))`. Call this from `TenderDetailViewModel.rescore()` after successful on-device scoring (after `updateScore()` local call). Fire-and-forget with error logging (don't block the UI on backend submission).

### Android Fix 4: Protect local scores during sync

**File**: `android/data/.../repository/TenderRepositoryImpl.kt`
**Change**: In `syncFromApi()`, before overwriting, check if the local tender has a non-null score and the incoming DTO has a null score. If so, preserve the local score. Simplest approach: after `insertTenders()`, re-apply any locally-scored values. Alternative: use a custom insert strategy that merges rather than replaces scores.

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Upsert logic differs between SQLite (dev) and PostgreSQL (prod) | Use SQLAlchemy `merge()` or explicit select-then-update pattern that works on both |
| RelevanceScorer still fails if LLM dependencies not in prod requirements | Existing try/except fallback to keyword scoring is preserved |
| Score submission fails if backend is unreachable | Fire-and-forget with retry on next sync; local score is preserved regardless |

---

## Acceptance Criteria

1. Backend pipeline scoring completes without silent failures in job logs
2. Re-running pipeline scoring updates existing scores (no IntegrityError)
3. Manual `/tenders/{id}/score` works when tender has only `requirements_summary`
4. Android shows "Score with AI" button for unscored tenders
5. Android "Re-score" uses user's actual criteria (not generic profile)
6. On-device scores appear in backend database after scoring
7. Syncing does not overwrite local scores with null
8. All existing tests continue to pass
