# OpenSpec Proposal: PDF Viewer & Real Scoring Implementation

**Change ID**: `pdf-viewer-scoring-fix`
**Status**: PENDING APPROVAL
**Created**: 2026-01-17

---

## 1. Problem Statement

Three critical issues are blocking user functionality:

### Issue 1: Cannot Open PDF Tenders
- **Current State**: PDF URLs are stored (`pdf_url` field) but no viewer/download UI exists in Android
- **User Impact**: Users cannot read actual tender documents to make bid decisions
- **Root Cause**: `TenderDetailScreen.kt` displays metadata but lacks PDF action buttons

### Issue 2: Missing Tender Summary on Click
- **Current State**: Detail screen shows limited info (title, reference, deadline, value)
- **User Impact**: Users need AI-generated summary to quickly assess tender relevance
- **Root Cause**: `requirements_summary` exists in data model but display could be enhanced

### Issue 3: Scoring/Calculations Not Working
- **Current State**: Backend has `RelevanceScorer` but it's not triggered automatically
- **User Impact**: All tenders show `null` scores, no relevance filtering possible
- **Root Cause**:
  - Scoring requires manual trigger (`score_tenders` endpoint or pipeline step)
  - Android shows mock radar values derived from score, not actual scoring dimensions
  - No automatic scoring on tender discovery

---

## 2. Proposed Solution

### 2.1 PDF Viewing/Download (Android)

**Option A: External Browser (Recommended - MVP)**
- Add "View PDF" button that opens `pdf_url` in device browser
- Pros: No download management, works immediately, browser handles PDF rendering
- Cons: Requires internet connection, leaves app

**Option B: In-App Download + External Viewer**
- Download PDF to app's cache directory
- Open with Android's file intent (uses device's PDF viewer app)
- Pros: Offline access, native PDF experience
- Cons: More complex, needs download progress UI, storage permissions

**Option C: In-App PDF Viewer**
- Integrate AndroidPdfViewer library or similar
- Render PDF directly in app
- Pros: Full control, best UX
- Cons: Significant implementation effort, library dependency

**Recommendation**: Start with Option A (external browser) for immediate value, then enhance to Option B.

### 2.2 Tender Summary Enhancement

- Prominently display `requirements_summary` in a collapsible card (already exists, just needs emphasis)
- Add "Generate Summary" button if `requirements_summary` is null
  - Calls backend endpoint to trigger LLM extraction
  - Shows loading state while processing
- Add extracted deadline, value, and key requirements as quick-glance chips

### 2.3 Automatic Scoring Implementation

**Backend Changes:**
1. Integrate scoring into the tender discovery pipeline:
   - After extraction → automatically run scoring
   - Store scores in `Score` table with default user profile
2. Add endpoint: `POST /tenders/{id}/score` to manually trigger scoring
3. Ensure `TenderListItem` includes `pdf_url` for Android to access

**Android Changes:**
1. Display actual score from API (already wired, just needs data)
2. Replace mock radar chart values with actual dimensions from score breakdown
3. Add "Re-score" button to refresh scoring with user's current criteria

---

## 3. Acceptance Criteria

### PDF Viewing
- [ ] User can tap "View PDF" button on tender detail screen
- [ ] PDF opens in device browser (or PDF viewer app)
- [ ] Button shows loading state while preparing
- [ ] Graceful error handling if PDF URL is invalid/404

### Summary Display
- [ ] Requirements summary prominently displayed when available
- [ ] "No summary available" state with option to generate
- [ ] Key tender details (deadline, value) shown as visual chips

### Scoring
- [ ] Tenders discovered have automatic scoring (score >= 0)
- [ ] Tender list shows actual scores, sorted by relevance
- [ ] Tender detail shows score explanation
- [ ] Radar chart reflects actual scoring dimensions (not mock data)

---

## 4. Technical Implementation

### 4.1 Android Changes

**TenderDetailScreen.kt:**
```kotlin
// Add action buttons row
Row {
    // View PDF button
    OutlinedButton(onClick = { viewModel.openPdf() }) {
        Icon(Icons.Default.PictureAsPdf, "PDF")
        Text("View PDF")
    }

    // Download PDF button (Phase 2)
    OutlinedButton(onClick = { viewModel.downloadPdf() }) {
        Icon(Icons.Default.Download, "Download")
        Text("Download")
    }
}
```

**TenderDetailViewModel.kt:**
```kotlin
fun openPdf() {
    state.value.tender?.pdfUrl?.let { url ->
        // Use Android Intent to open URL in browser
        _events.emit(TenderDetailEvent.OpenUrl(url))
    }
}
```

**Domain Model - Add missing field mapping:**
- Ensure `pdfUrl` flows from API → DTO → Entity → Domain model
- Currently `TenderListItem` schema missing `pdf_url` - need to add

### 4.2 Backend Changes

**TenderListItem Schema** (`src/api/schemas/tender.py`):
```python
class TenderListItem(BaseModel):
    # ... existing fields ...
    pdf_url: Optional[str] = None  # ADD THIS
```

**Auto-Scoring Integration** (`src/api/routers/pipeline.py`):
- After extraction step completes, trigger scoring
- Use default criteria if user criteria not set

### 4.3 Data Flow

```
Discovery Pipeline:
  1. Scrape → PDF URLs discovered
  2. Download → PDF files saved
  3. Extract → Text extracted, TenderInfo parsed
  4. Score → [NEW] Auto-score with default/user criteria
  5. Sync → Android fetches scored tenders

Android Detail View:
  User taps tender → Load from Room → Display score + summary + PDF button
  User taps "View PDF" → Intent.ACTION_VIEW with pdf_url → Opens browser
```

---

## 5. Implementation Phases

### Phase 1: PDF View Button (MVP)
1. Add `pdf_url` to `TenderListItem` schema (backend)
2. Add "View PDF" button to `TenderDetailScreen` (Android)
3. Implement `openPdf()` with browser intent (Android)
4. Handle missing/invalid URLs gracefully

### Phase 2: Summary Enhancement
1. Emphasize requirements summary in detail screen
2. Add deadline/value chips for quick glance
3. Add "Generate Summary" for null summaries (calls extraction)

### Phase 3: Automatic Scoring
1. Integrate scoring into discovery pipeline (backend)
2. Add `POST /tenders/{id}/score` endpoint for manual trigger
3. Replace mock radar values with actual score dimensions (Android)
4. Add "Re-score" capability

---

## 6. Files to Modify

### Backend
| File | Changes |
|------|---------|
| `src/api/schemas/tender.py` | Add `pdf_url` to `TenderListItem` |
| `src/api/routers/tenders.py` | Include `pdf_url` in list response |
| `src/api/routers/pipeline.py` | Add scoring step to pipeline |

### Android
| File | Changes |
|------|---------|
| `app/.../TenderDetailScreen.kt` | Add PDF action buttons, enhance summary |
| `app/.../TenderDetailViewModel.kt` | Add `openPdf()`, `downloadPdf()` functions |
| `data/.../dto/TenderDto.kt` | Already has `pdfUrl`, verify mapping |
| `data/.../entity/TenderEntity.kt` | Verify `pdfUrl` field |
| `domain/.../model/Tender.kt` | Verify `pdfUrl` field |

---

## 7. Risk Assessment

| Risk | Mitigation |
|------|------------|
| PDF URLs may be invalid/expired | Check URL validity, show user-friendly error |
| Large PDFs slow to load | Show loading indicator, consider streaming |
| Scoring LLM model not loaded | Add health check, fallback to "unscored" |
| User criteria not configured | Use sensible defaults for scoring |

---

## 8. Questions for User

1. **PDF Handling Priority**: Start with browser-based viewing (faster to ship) or jump to download+offline (better UX)?
2. **Scoring Behavior**: Score all tenders automatically, or only on user request?
3. **Radar Chart Dimensions**: What dimensions matter most for bid strength? (Price, B-BBEE, Track Record, Functionality are current)

---

**Status**: PENDING APPROVAL

Awaiting user approval before implementation begins.
