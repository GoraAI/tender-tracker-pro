# Spec Delta: PDF Viewer & Scoring Fix

**Change ID**: `pdf-viewer-scoring-fix`

---

## API Changes

### TenderListItem Schema
```diff
class TenderListItem(BaseModel):
    id: UUID
    source_id: str
    title: Optional[str] = None
    reference: Optional[str] = None
    deadline: Optional[datetime] = None
    estimated_value: Optional[Decimal] = None
    discovered_at: datetime
    score: Optional[int] = None
    relevance_category: Optional[str] = None
+   pdf_url: Optional[str] = None
+   score_explanation: Optional[str] = None
```

### New Endpoints
```
POST /tenders/{id}/extract
  - Triggers LLM extraction for tender
  - Returns updated TenderResponse with requirements_summary

POST /tenders/{id}/score
  - Triggers scoring with user's criteria (or defaults)
  - Returns updated TenderResponse with score
```

---

## Android UI Changes

### TenderDetailScreen
```diff
+ Action buttons row:
+   - "View PDF" → opens pdf_url in browser
+   - "Download PDF" → saves to device (Phase 2)
+   - "Re-score" → triggers manual scoring

+ Enhanced summary section:
+   - Prominent requirements_summary card
+   - Urgency chip (days to deadline)
+   - Value range chip
```

### TenderDetailViewModel
```diff
+ sealed class TenderDetailEvent {
+     data class OpenUrl(val url: String) : TenderDetailEvent()
+     data class ShowError(val message: String) : TenderDetailEvent()
+ }
+
+ fun openPdf()
+ fun downloadPdf()  // Phase 2
+ fun rescore()      // Phase 3
```

---

## Data Flow Changes

### Discovery Pipeline
```diff
  Scrape → Download → Extract
+                        ↓
+                    Score (auto)
+                        ↓
                    Store in DB
```

### Android Sync
```diff
  API getTenders() → TenderDto (includes pdf_url, score, explanation)
      ↓
  TenderEntity → Room DB
      ↓
  Tender domain model → UI
```

---

## Status: PENDING APPROVAL
