# Implementation Tasks: PDF Viewer & Scoring Fix

**Change ID**: `pdf-viewer-scoring-fix`
**Status**: IMPLEMENTATION COMPLETE - Ready for Verification

---

## Phase 1: PDF View Button (MVP) ✅ COMPLETE

### 1.1 Backend - Add pdf_url to list response
- [x] Add `pdf_url: Optional[str] = None` to `TenderListItem` schema
- [x] Update `get_tenders()` in routers/tenders.py to include pdf_url
- [x] Test: Verify API response includes pdf_url

### 1.2 Android - PDF View Button
- [x] Add "View PDF" button to TenderDetailScreen
- [x] Add "Download PDF" button placeholder (disabled initially)
- [x] Implement `openPdf()` in TenderDetailViewModel
- [x] Add `TenderDetailEvent` sealed class for navigation events
- [x] Handle event in TenderDetailScreen to launch browser intent
- [x] Add error handling for missing/invalid PDF URLs
- [x] Test: Tap button opens PDF in browser

---

## Phase 2: Summary Enhancement ✅ COMPLETE

### 2.1 Android - Summary Display
- [x] Move requirements summary to prominent position
- [x] Add collapsible/expandable behavior for long summaries
- [x] Add quick-glance chips (deadline urgency, value range)
- [x] Style chips with semantic colors (red=urgent, green=good value)
- [x] Test: Summary clearly visible on detail screen

### 2.2 Backend - Generate Summary Endpoint
- [x] Add `POST /tenders/{id}/extract` endpoint to trigger extraction
- [x] Return updated tender with requirements_summary
- [x] Test: Endpoint populates summary for tender without one

---

## Phase 3: Automatic Scoring ✅ COMPLETE

### 3.1 Backend - Pipeline Integration
- [x] Add `POST /tenders/{id}/score` for manual scoring trigger
- [x] Handle scoring failures gracefully (mark as "unscored")
- [ ] Add scoring step to discovery pipeline after extraction (deferred - needs pipeline service integration)
- [ ] Create default scoring criteria for unregistered users (deferred - needs criteria service)
- [ ] Test: New tenders have scores after pipeline runs (deferred)

### 3.2 Android - Real Score Display
- [x] Verify score flows from API → DTO → Entity → Domain → UI
- [x] Add score_explanation to TenderListItem (backend + Android)
- [x] Add "Re-score" button on detail screen
- [ ] Replace mock radar values with actual score breakdown (deferred - needs score dimension data)
- [ ] Test: Tenders display real scores, radar reflects actual dimensions (deferred)

---

## Dependencies

| Task | Depends On |
|------|------------|
| 1.2 Android PDF Button | 1.1 Backend pdf_url |
| 2.1 Summary Display | None (enhance existing) |
| 2.2 Generate Summary | Extraction pipeline functional |
| 3.1 Pipeline Scoring | Scoring module + LLM model loaded |
| 3.2 Real Score Display | 3.1 Pipeline Scoring |

---

**Status**: PENDING APPROVAL

Tasks will be marked complete as implementation proceeds after approval.
