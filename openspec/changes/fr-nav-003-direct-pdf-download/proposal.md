# FR-NAV-003: Direct PDF Download

**Status:** 🟢 APPROVED
**Priority:** P0 (Critical for MVP1)
**Depends On:**
- FR-NAV-001 (Browser Navigation Engine) ✅
- FR-NAV-002 (PDF Link Discovery) ✅
**Blocks:** FR-EXTRACT-001 (PDF Text Extraction)

---

## Summary

Implement reliable PDF downloading from discovered links with validation, retry logic, and organized storage.

---

## Motivation

After discovering PDF links (FR-NAV-002), we need to:
1. Download PDFs reliably (handle network errors, timeouts)
2. Validate file integrity (verify PDF header, file size)
3. Organize downloads by source and date
4. Skip already-downloaded files (efficiency)
5. Support concurrent downloads (performance)
6. Update tracking file with download status
7. Handle browser-based downloads (some sites require session/cookies)

This enables the downstream extraction pipeline to process tender documents.

---

## Scope

### In Scope

- Browser-based PDF download (click link, wait for download)
- Direct HTTP download (faster, for public PDFs)
- Automatic method selection (try direct, fallback to browser)
- PDF validation (check `%PDF-` header, min/max file size)
- Retry logic (3 attempts with exponential backoff)
- Organized storage (`documents/downloads/{source_id}/{YYYY-MM}/filename.pdf`)
- Concurrent downloads (configurable, default 3)
- Skip existing files (by filename or size comparison)
- Progress tracking and logging
- Update tracking file with download status
- Error handling with screenshots on failure

### Out of Scope

- PDF content analysis (FR-EXTRACT-001)
- Compression/decompression
- Password-protected PDFs (mark as failed)
- Download resume (partial downloads)
- Cloud storage integration
- Database storage (using filesystem for MVP1)

---

## Technical Approach

### Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  PDF Download Flow                                           │
│                                                              │
│  PDFDiscovery                                                │
│       │                                                      │
│       ├─ discover_pdfs() → List[PDFLink]                    │
│       │                                                      │
│       ▼                                                      │
│  PDFDownloader                                               │
│       │                                                      │
│       ├─ download_batch(pdf_links) → List[DownloadResult]   │
│       │   │                                                  │
│       │   ├─ For each PDFLink (with concurrency limit):     │
│       │   │   │                                              │
│       │   │   ├─ Check if already downloaded (skip if yes)  │
│       │   │   │                                              │
│       │   │   ├─ Try direct HTTP download first             │
│       │   │   │   ├─ requests.get() with stream=True        │
│       │   │   │   ├─ Validate content-type                  │
│       │   │   │   └─ If 403/401 → fallback to browser      │
│       │   │   │                                              │
│       │   │   ├─ Fallback: Browser download                 │
│       │   │   │   ├─ Navigate to PDF URL                    │
│       │   │   │   ├─ Wait for download event                │
│       │   │   │   └─ Move to target location                │
│       │   │   │                                              │
│       │   │   ├─ Validate PDF                               │
│       │   │   │   ├─ Check file size (min/max)              │
│       │   │   │   ├─ Check PDF header (%PDF-)               │
│       │   │   │   └─ If invalid → retry                     │
│       │   │   │                                              │
│       │   │   ├─ Move to organized location                 │
│       │   │   │   documents/downloads/                      │
│       │   │   │   └─ {source_id}/{YYYY-MM}/filename.pdf    │
│       │   │   │                                              │
│       │   │   ├─ Retry on failure (3 attempts, backoff)     │
│       │   │   │                                              │
│       │   │   └─ Return DownloadResult                      │
│       │   │                                                  │
│       │   └─ Update tracking file (mark as downloaded)      │
│       │                                                      │
│       └─ Return summary (success/failed counts)             │
└──────────────────────────────────────────────────────────────┘
```

### Key Components

**1. DownloadResult Data Class**
```python
@dataclass
class DownloadResult:
    pdf_link: PDFLink
    success: bool
    file_path: Optional[Path]
    file_size: int
    download_time_ms: int
    method: str  # "direct" or "browser"
    error: Optional[str]
    attempts: int
```

**2. PDFDownloader Class**
```python
class PDFDownloader:
    def __init__(self, config: dict, browser_engine: Optional[BrowserEngine] = None):
        """
        Initialize PDF downloader.

        Args:
            config: download section from source config
            browser_engine: Optional BrowserEngine for browser-based downloads
        """

    async def download_batch(
        self,
        pdf_links: List[PDFLink],
        max_concurrent: int = 3
    ) -> List[DownloadResult]:
        """Download multiple PDFs concurrently."""

    async def download_single(
        self,
        pdf_link: PDFLink,
        method: str = "auto"
    ) -> DownloadResult:
        """
        Download a single PDF.

        Args:
            pdf_link: PDFLink to download
            method: "auto", "direct", or "browser"

        Returns:
            DownloadResult with success status and file path
        """

    async def _download_direct(self, url: str, output_path: Path) -> bool:
        """Direct HTTP download using requests."""

    async def _download_browser(
        self,
        url: str,
        output_path: Path
    ) -> bool:
        """Browser-based download (for sites requiring auth/cookies)."""

    def _validate_pdf(self, file_path: Path) -> bool:
        """
        Validate PDF file.

        Checks:
        - File exists
        - Size within min/max bounds
        - Has PDF header (%PDF-)
        """

    def _should_skip(self, pdf_link: PDFLink, target_path: Path) -> bool:
        """Check if file should be skipped (already downloaded)."""

    def _build_output_path(self, pdf_link: PDFLink) -> Path:
        """
        Build organized output path.

        Format: documents/downloads/{source_id}/{YYYY-MM}/filename.pdf
        """
```

**3. Storage Organization**
```
documents/downloads/
├── jhb-water/
│   ├── 2026-01/
│   │   ├── RFQWC001-2026.pdf
│   │   ├── RFQWC002-2026.pdf
│   │   └── RFQWC003-2026.pdf
│   └── 2026-02/
│       └── RFQWC004-2026.pdf
```

---

## Acceptance Criteria

### Functional

- [ ] **AC1:** Download PDF via direct HTTP
  - Given a public PDF URL
  - When `download_single()` is called with method="direct"
  - Then download using requests.get() and save to target path

- [ ] **AC2:** Download PDF via browser
  - Given a PDF URL requiring authentication/cookies
  - When direct download fails with 403/401
  - Then fallback to browser-based download automatically

- [ ] **AC3:** Validate PDF files
  - Given a downloaded file
  - When validating, check file size (min 1KB, max 50MB)
  - And verify PDF header starts with `%PDF-`
  - Then return validation result

- [ ] **AC4:** Retry on failure
  - Given a download that fails
  - When attempts < max_retries (3)
  - Then retry with exponential backoff (2s, 4s, 8s)

- [ ] **AC5:** Skip existing files
  - Given a PDF already downloaded
  - When `_should_skip()` checks target path
  - Then return True (skip download)

- [ ] **AC6:** Organize downloads by date
  - Given a PDFLink with discovered_at timestamp
  - When building output path
  - Then use format: `{source_id}/{YYYY-MM}/filename.pdf`

- [ ] **AC7:** Concurrent downloads
  - Given 10 PDFLinks
  - When `download_batch()` is called with max_concurrent=3
  - Then download 3 at a time (not all 10 simultaneously)

- [ ] **AC8:** Update tracking file
  - Given successful downloads
  - When batch completes
  - Then update tracking file with downloaded=True and file_path

- [ ] **AC9:** Handle invalid PDFs
  - Given a file that's not a valid PDF (HTML error page)
  - When validating
  - Then mark as failed and retry

- [ ] **AC10:** Extract filename from URL
  - Given URL: "https://example.com/path/RFQ123.pdf?query=1"
  - When building output path
  - Then extract "RFQ123.pdf" as filename

### Non-Functional

- [ ] **AC11:** Download performance
  - Download 10 PDFs (avg 500KB each) in <30 seconds (concurrent)

- [ ] **AC12:** Memory efficiency
  - Stream downloads (don't load entire file in memory)
  - Handle 50MB PDFs without memory issues

- [ ] **AC13:** Logging
  - Log download start, method used, file size, duration
  - Log validation failures with reason
  - Log retry attempts

- [ ] **AC14:** Error handling
  - Handle network timeouts gracefully
  - Handle disk space errors
  - Handle permission errors
  - Take screenshot on browser download failures

---

## Implementation Tasks

See `tasks.md` for detailed implementation checklist (45 tasks).

---

## Performance Targets

| Metric | Target | Alert | Don't Ship |
|--------|--------|-------|------------|
| Download speed (500KB PDF) | <5s | >10s | >20s |
| Batch download (10 PDFs, 500KB each) | <30s | >60s | >120s |
| Validation time | <100ms/file | >500ms | >1s |
| Memory usage (50MB PDF) | <200MB | >500MB | >1GB |
| Success rate (good URLs) | >95% | <90% | <80% |

---

## Dependencies

**Required (Must Complete First):**
- ✅ FR-NAV-001: Browser Navigation Engine
- ✅ FR-NAV-002: PDF Link Discovery

**Blocks (Depends on This):**
- FR-EXTRACT-001: PDF Text Extraction
- FR-LLM-002: Tender Information Extraction

---

## Risks & Mitigations

### Risk 1: Rate Limiting
**Impact:** Server may block too many concurrent requests
**Mitigation:** Configurable concurrency limit (default 3), human-like delays between downloads
**Fallback:** Reduce concurrency, add delays

### Risk 2: Authentication Required
**Impact:** Some PDFs may require login/session
**Mitigation:** Browser-based download fallback with BrowserEngine
**Test:** JHB Water site in integration tests

### Risk 3: Large Files
**Impact:** 50MB+ PDFs could cause memory issues or timeouts
**Mitigation:** Stream downloads, configurable max file size, separate timeout for large files
**Monitoring:** Log file sizes, alert on >50MB

### Risk 4: Disk Space
**Impact:** Could run out of disk space
**Mitigation:** Check available space before download, configurable storage limit
**Recovery:** Archive old downloads, clean up failed downloads

### Risk 5: Network Failures
**Impact:** Downloads interrupted mid-stream
**Mitigation:** Retry logic (3 attempts), validate after download
**Recovery:** Delete partial files, retry

---

## Testing Strategy

### Unit Tests (20 tests)
- Direct download (3 tests)
- Browser download (3 tests)
- Validation (4 tests)
- Path building (3 tests)
- Skip logic (2 tests)
- Retry logic (3 tests)
- Concurrent downloads (2 tests)

### Integration Tests (5 tests)
- Download from JHB Water (real PDFs)
- Batch download with concurrency
- Fallback from direct to browser
- Skip existing files across runs
- Large file handling (>10MB)

### Test Coverage Target
- Unit test coverage: >85%
- Integration tests: All critical paths
- Load test: 50 concurrent downloads

---

## Documentation

- [ ] Update `docs/API.md` with PDFDownloader API
- [ ] Update `docs/NAVIGATION.md` with download examples
- [ ] Create download troubleshooting guide
- [ ] Document storage organization

---

## Success Criteria

**Definition of Done:**
- [ ] All 14 acceptance criteria passing
- [ ] Unit tests >85% coverage
- [ ] Integration tests passing on JHB Water
- [ ] Documentation updated
- [ ] Code formatted and linted
- [ ] Performance benchmarks meet targets
- [ ] Download 10 real PDFs successfully

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
- Config: `config/sources/jhb_water.yaml` (lines 112-140)
- FR-NAV-001: `openspec/changes/fr-nav-001-browser-navigation-engine/`
- FR-NAV-002: `openspec/changes/fr-nav-002-pdf-link-discovery/`
