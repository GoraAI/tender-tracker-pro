# Spec Delta: Fix Scoring Pipeline

**Change**: fix-scoring-pipeline
**Date**: 2026-02-19
**Affects**: api-endpoints.md, android/foundation.md

---

## Changes to: api-endpoints.md

### POST /tenders/{id}/score — Behavior Change

**Before**: Background task silently returns when `raw_text` is null (even if `requirements_summary` exists). Duplicate scoring raises unhandled IntegrityError.

**After**: Background task uses `raw_text` falling back to `requirements_summary` for keyword matching. Duplicate scoring performs upsert (updates existing score record).

### POST /pipeline/score — Bug Fix

**Before**: LLM scoring path always fails with AttributeError (`result.category` does not exist on `ScoredTender`). Falls back to keyword scoring. Keyword path also fails on re-score due to IntegrityError.

**After**: LLM scoring path uses correct attribute `result.relevance_category`. Both LLM and keyword paths use upsert logic for score persistence.

---

## Changes to: android/foundation.md

### TenderDetailScreen — UI Change

**Before**: Score card (including Re-score button) only renders when `tender.score != null`. Unscored tenders have no scoring UI.

**After**: Two states:
- `score != null`: Score card with ScoreBadge + "Re-score" button (unchanged visual)
- `score == null`: "Score with AI" button card (new, glassmorphism styled)

### TenderDetailViewModel — Dependency Change

**Before**: Constructor takes `TenderRepository` and `ScoringService`. `rescore()` passes empty `userProfile` to scoring. Scores saved locally only.

**After**: Constructor additionally takes `CriteriaRepository`. `rescore()` fetches default criteria and passes formatted profile string to scoring. After local save, submits score to backend via `TenderRepository.submitScoreToBackend()`.

### TenderRepositoryImpl — New Method + Sync Change

**New method**: `submitScoreToBackend(tenderId, score, explanation)` — calls `POST /tenders/{id}/score` endpoint.

**Sync change**: `syncFromApi()` preserves local non-null scores when backend returns null score for the same tender.

---

## Changes to: src/scoring/relevance_scorer.py — Interface Change

**Before**: `__init__` requires YAML file paths for criteria and config. Always reads from filesystem.

**After**: `__init__` accepts optional `criteria: dict` parameter. When provided, YAML loading is skipped. When not provided, YAML loading is attempted with graceful fallback.

---

## No Changes To

- Database schema (no migrations needed — Score model unchanged)
- Auth system
- Dashboard
- Analytics
- Chat system
- Navigation
- CI/CD pipeline
