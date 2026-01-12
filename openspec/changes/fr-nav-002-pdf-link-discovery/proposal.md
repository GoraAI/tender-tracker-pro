# FR-NAV-002: PDF Link Discovery

**Status:** 🟢 APPROVED
**Priority:** P0 (Critical for MVP1)
**Depends On:** FR-NAV-001 (Browser Navigation Engine) ✅
**Blocks:** FR-NAV-003 (Direct PDF Download)

---

## Summary

Implement pattern-based PDF link discovery on loaded web pages to identify tender documents for download and processing.

---

## Motivation

After navigating to tender listing pages using the BrowserEngine, we need to:
1. Identify all PDF links on the page
2. Extract metadata (link text, title, surrounding context)
3. Filter based on configurable patterns
4. Deduplicate against previously discovered PDFs
5. Track discovery history to avoid reprocessing

This enables the downstream pipeline to download and analyze only new, relevant tender documents.

---

## Scope

### In Scope

- Scan Playwright Page objects for PDF links
- Pattern-based filtering using regex
- Extract link metadata (URL, text, title, context)
- Context extraction from parent elements (table rows, divs)
- Deduplication against tracking file
- Exclude pattern support (skip archived/test PDFs)
- Support for relative and absolute URLs
- Structured logging for discovery metrics
- Integration with BrowserEngine pagination results

### Out of Scope

- PDF downloading (FR-NAV-003)
- PDF content analysis (FR-EXTRACT-001)
- HTML scraping fallback (FR-SCRAPE-001)
- Database storage (using JSON tracking file for MVP1)

---

## Technical Approach

### Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  PDF Discovery Flow                                          │
│                                                              │
│  BrowserEngine                                               │
│       │                                                      │
│       ├─ navigate() → Page                                   │
│       ├─ paginate() → List[Page]                             │
│       │                                                      │
│       ▼                                                      │
│  PDFDiscovery                                                │
│       │                                                      │
│       ├─ discover_pdfs(page) → List[PDFLink]                │
│       │   │                                                  │
│       │   ├─ Query all <a> tags                             │
│       │   ├─ Filter by href patterns                        │
│       │   ├─ Extract metadata                               │
│       │   └─ Build PDFLink objects                          │
│       │                                                      │
│       ├─ extract_context(element, config) → str             │
│       │                                                      │
│       ├─ deduplicate(pdf_links) → List[PDFLink]             │
│       │   │                                                  │
│       │   ├─ Load tracking file                             │
│       │   ├─ Filter seen URLs                               │
│       │   └─ Update tracking file                           │
│       │                                                      │
│       └─ save_discovery_metadata()                          │
└──────────────────────────────────────────────────────────────┘
```

### Key Components

**1. PDFLink Data Class**
```python
@dataclass
class PDFLink:
    url: str              # Full PDF URL
    link_text: str        # Anchor text
    title: Optional[str]  # Link title attribute
    context: str          # Surrounding text (e.g., table row)
    discovered_at: datetime
    source_url: str       # Page URL where found
    source_id: str        # e.g., "jhb-water"
```

**2. PDFDiscovery Class**
```python
class PDFDiscovery:
    def __init__(self, config: dict, tracking_file: Path):
        """
        Initialize PDF discovery engine.

        Args:
            config: pdf_discovery section from source config
            tracking_file: Path to JSON tracking file
        """

    async def discover_pdfs(
        self,
        page: Page,
        source_id: str
    ) -> List[PDFLink]:
        """
        Discover all PDF links on a page.

        Returns only NEW links (not in tracking file).
        """

    async def extract_context(
        self,
        element: ElementHandle,
        parent_selector: str,
        max_length: int
    ) -> str:
        """Extract surrounding context for a PDF link."""

    def deduplicate(self, pdf_links: List[PDFLink]) -> List[PDFLink]:
        """Filter out previously discovered PDFs."""

    def load_tracking(self) -> Dict[str, Any]:
        """Load tracking file (JSON)."""

    def save_tracking(self, pdf_links: List[PDFLink]) -> None:
        """Update tracking file with new discoveries."""
```

**3. Pattern Matching**
- Use regex patterns from config
- Support multiple patterns (OR logic)
- Exclude patterns (blocklist)

**4. Tracking File Format**
```json
{
  "version": "1.0",
  "last_updated": "2026-01-12T14:30:00Z",
  "discovered_pdfs": {
    "https://example.com/rfq123.pdf": {
      "first_seen": "2026-01-12T14:30:00Z",
      "link_text": "RFQ123 - Water Supply",
      "source_id": "jhb-water",
      "downloaded": true
    }
  }
}
```

---

## Acceptance Criteria

### Functional

- [ ] **AC1:** Discover all PDF links on a single page
  - Given a loaded Playwright Page
  - When `discover_pdfs()` is called
  - Then return all PDFLink objects matching patterns

- [ ] **AC2:** Support regex pattern filtering
  - Given patterns: `["/wp-content/.*\\.pdf$", "RFQ.*\\.pdf$"]`
  - When page has 10 PDF links (5 matching, 5 not)
  - Then return only the 5 matching links

- [ ] **AC3:** Extract link metadata
  - For each PDF link, extract:
    - Full URL (absolute)
    - Link text (anchor innerText)
    - Title attribute (if present)
    - Discovery timestamp

- [ ] **AC4:** Extract surrounding context
  - Given parent_selector: "tr" (table row)
  - When PDF link is in table
  - Then extract all text from parent row (max 500 chars)

- [ ] **AC5:** Handle relative URLs
  - Given href="/wp-content/uploads/2026/01/rfq.pdf"
  - When page URL is "https://example.com/tenders"
  - Then return absolute URL: "https://example.com/wp-content/uploads/2026/01/rfq.pdf"

- [ ] **AC6:** Deduplicate against tracking file
  - Given tracking file has 3 URLs
  - When page has 5 PDF links (2 are duplicates)
  - Then return only 3 new links

- [ ] **AC7:** Update tracking file
  - Given 3 new PDFLinks discovered
  - When `save_tracking()` is called
  - Then tracking file updated with new entries

- [ ] **AC8:** Support exclude patterns
  - Given exclude_patterns: `[".*archive.*", ".*old.*"]`
  - When page has "rfq123_old.pdf" and "rfq124.pdf"
  - Then return only "rfq124.pdf"

- [ ] **AC9:** Discover PDFs across multiple pages
  - Given paginated results (3 pages)
  - When each page has 5 PDFs
  - Then discover all 15 PDFs (deduplicated)

### Non-Functional

- [ ] **AC10:** Discovery performance
  - Discover and filter 50 PDF links in <2 seconds

- [ ] **AC11:** Memory efficiency
  - Handle pages with 500+ links without memory issues

- [ ] **AC12:** Logging
  - Log discovery metrics (count, patterns matched, duplicates filtered)

- [ ] **AC13:** Error handling
  - Gracefully handle pages with no PDF links (return empty list)
  - Handle invalid URLs (log warning, skip)
  - Handle missing tracking file (create new)

---

## Implementation Tasks

See `tasks.md` for detailed implementation checklist.

---

## Performance Targets

| Metric | Target | Alert | Don't Ship |
|--------|--------|-------|------------|
| Discovery time (50 links) | <2s | >5s | >10s |
| Memory usage | <100MB | >200MB | >500MB |
| Pattern matching | <50ms/link | >100ms | >200ms |
| Context extraction | <100ms/link | >200ms | >500ms |

---

## Dependencies

**Required (Must Complete First):**
- ✅ FR-NAV-001: Browser Navigation Engine

**Blocks (Depends on This):**
- FR-NAV-003: Direct PDF Download
- FR-EXTRACT-001: PDF Text Extraction

---

## Risks & Mitigations

### Risk 1: Dynamic Content Loading
**Impact:** PDFs loaded via JavaScript may not be in initial DOM
**Mitigation:** BrowserEngine already waits for "networkidle", which handles most dynamic content
**Fallback:** Add explicit wait for PDF links before discovery

### Risk 2: Inconsistent URL Formats
**Impact:** Some PDFs may use query parameters or redirects
**Mitigation:** Normalize URLs before tracking (strip query params configurable)
**Test:** Include edge cases in unit tests

### Risk 3: Tracking File Corruption
**Impact:** Could lose deduplication history
**Mitigation:** Use atomic writes, backup before update
**Recovery:** Tracking file can be regenerated (worst case: reprocess some PDFs)

### Risk 4: Large Tracking Files
**Impact:** 10,000+ PDFs could slow down dedup checks
**Mitigation:** Use dict lookups (O(1)), archive old entries after 90 days
**Future:** Migrate to SQLite for production scale

---

## Testing Strategy

### Unit Tests (15 tests)
- Test pattern matching (3 tests)
- Test metadata extraction (3 tests)
- Test context extraction (2 tests)
- Test deduplication logic (3 tests)
- Test tracking file operations (2 tests)
- Test URL normalization (2 tests)

### Integration Tests (5 tests)
- Test discovery on JHB Water live site
- Test pagination + discovery (multiple pages)
- Test deduplication across runs
- Test exclude patterns with real data
- Test tracking file persistence

### Test Coverage Target
- Unit test coverage: >85%
- Integration tests: All critical paths

---

## Documentation

- [ ] Update `docs/API.md` with PDFDiscovery API
- [ ] Update `docs/NAVIGATION.md` with PDF discovery examples
- [ ] Create discovery pattern guide in docs
- [ ] Document tracking file format

---

## Success Criteria

**Definition of Done:**
- [ ] All 13 acceptance criteria passing
- [ ] Unit tests >85% coverage
- [ ] Integration tests passing on JHB Water
- [ ] Documentation updated
- [ ] Code formatted and linted
- [ ] Pattern matching benchmarks meet targets
- [ ] Tracking file tested with 100+ entries

---

## Approval

**Proposed By:** Claude (OpenSpec Apply)
**Date:** 2026-01-12
**Reviewer:** [Tech Lead]
**Status:** 🟡 AWAITING APPROVAL

---

## References

- PRD: `docs/PRD.md`
- Architecture: `docs/ARCHITECTURE.md`
- Config: `config/sources/jhb_water.yaml` (lines 76-111)
- FR-NAV-001: `openspec/changes/fr-nav-001-browser-navigation-engine/`
