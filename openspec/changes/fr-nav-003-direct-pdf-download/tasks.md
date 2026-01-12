# FR-NAV-003 Implementation Tasks

**Feature:** Direct PDF Download
**Status:** 🟡 NOT STARTED
**Progress:** 0/45 tasks complete (0%)

---

## Phase 1: Data Models & Setup (5 tasks)

- [ ] **Task 1.1:** Create `src/navigator/pdf_downloader.py` module
- [ ] **Task 1.2:** Create `src/navigator/models.py` DownloadResult dataclass
- [ ] **Task 1.3:** Update `src/navigator/__init__.py` to export PDFDownloader
- [ ] **Task 1.4:** Create `tests/test_pdf_downloader.py` test file
- [ ] **Task 1.5:** Create `tests/integration/test_jhb_water_downloads.py`

---

## Phase 2: DownloadResult Data Model (3 tasks)

- [ ] **Task 2.1:** Implement DownloadResult dataclass with all fields
- [ ] **Task 2.2:** Add `to_dict()` method for logging/tracking
- [ ] **Task 2.3:** Add summary statistics method (`success_rate()`, `total_size()`)

---

## Phase 3: Core Downloader Setup (5 tasks)

- [ ] **Task 3.1:** Implement `PDFDownloader.__init__()` with config
- [ ] **Task 3.2:** Add requests session with retry adapter
- [ ] **Task 3.3:** Implement `_build_output_path()` with date-based organization
- [ ] **Task 3.4:** Implement `_should_skip()` for existing file check
- [ ] **Task 3.5:** Implement `_extract_filename()` from URL

---

## Phase 4: Direct HTTP Download (6 tasks)

- [ ] **Task 4.1:** Implement `_download_direct()` method
- [ ] **Task 4.2:** Use requests.get() with stream=True
- [ ] **Task 4.3:** Validate content-type header (application/pdf)
- [ ] **Task 4.4:** Stream write to file in chunks (8KB)
- [ ] **Task 4.5:** Add timeout (30s default)
- [ ] **Task 4.6:** Handle HTTP errors (403, 404, 500)

---

## Phase 5: Browser-Based Download (7 tasks)

- [ ] **Task 5.1:** Implement `_download_browser()` method
- [ ] **Task 5.2:** Navigate to PDF URL with BrowserEngine
- [ ] **Task 5.3:** Wait for download event with Playwright
- [ ] **Task 5.4:** Set download directory for browser
- [ ] **Task 5.5:** Wait for download completion (timeout 60s)
- [ ] **Task 5.6:** Move downloaded file to target path
- [ ] **Task 5.7:** Clean up browser download directory

---

## Phase 6: PDF Validation (5 tasks)

- [ ] **Task 6.1:** Implement `_validate_pdf()` method
- [ ] **Task 6.2:** Check file exists
- [ ] **Task 6.3:** Check file size (min 1KB, max 50MB configurable)
- [ ] **Task 6.4:** Read first 5 bytes and verify `%PDF-` header
- [ ] **Task 6.5:** Return validation result with reason on failure

---

## Phase 7: Single Download with Retry (6 tasks)

- [ ] **Task 7.1:** Implement `download_single()` method
- [ ] **Task 7.2:** Check if should skip (already downloaded)
- [ ] **Task 7.3:** Try direct download first
- [ ] **Task 7.4:** Fallback to browser download on 403/401/timeout
- [ ] **Task 7.5:** Implement retry logic (3 attempts, exponential backoff)
- [ ] **Task 7.6:** Validate PDF after download, retry if invalid

---

## Phase 8: Batch Download with Concurrency (5 tasks)

- [ ] **Task 8.1:** Implement `download_batch()` method
- [ ] **Task 8.2:** Use asyncio.Semaphore for concurrency limit
- [ ] **Task 8.3:** Create tasks for all PDFLinks
- [ ] **Task 8.4:** Gather results with asyncio.gather()
- [ ] **Task 8.5:** Return List[DownloadResult] with summary

---

## Phase 9: Tracking File Integration (4 tasks)

- [ ] **Task 9.1:** Implement `update_tracking()` method
- [ ] **Task 9.2:** Load tracking file from PDFDiscovery
- [ ] **Task 9.3:** Update downloaded status and file_path for each PDFLink
- [ ] **Task 9.4:** Save updated tracking file atomically

---

## Phase 10: Error Handling & Logging (6 tasks)

- [ ] **Task 10.1:** Add structured logging for all download operations
- [ ] **Task 10.2:** Log download start (URL, method, attempt)
- [ ] **Task 10.3:** Log download complete (duration, file size, method)
- [ ] **Task 10.4:** Handle network timeouts with specific error messages
- [ ] **Task 10.5:** Handle disk space errors
- [ ] **Task 10.6:** Take screenshot on browser download failure

---

## Phase 11: Unit Tests (20 tests)

- [ ] **Task 11.1:** Test DownloadResult creation
- [ ] **Task 11.2:** Test path building with date organization
- [ ] **Task 11.3:** Test filename extraction from URL
- [ ] **Task 11.4:** Test filename extraction with query parameters
- [ ] **Task 11.5:** Test should_skip logic (file exists)
- [ ] **Task 11.6:** Test should_skip logic (file doesn't exist)
- [ ] **Task 11.7:** Test direct download success
- [ ] **Task 11.8:** Test direct download with HTTP error
- [ ] **Task 11.9:** Test direct download with timeout
- [ ] **Task 11.10:** Test PDF validation (valid PDF)
- [ ] **Task 11.11:** Test PDF validation (too small)
- [ ] **Task 11.12:** Test PDF validation (too large)
- [ ] **Task 11.13:** Test PDF validation (missing header)
- [ ] **Task 11.14:** Test retry logic (success on 2nd attempt)
- [ ] **Task 11.15:** Test retry logic (all attempts fail)
- [ ] **Task 11.16:** Test fallback from direct to browser
- [ ] **Task 11.17:** Test concurrent download with semaphore
- [ ] **Task 11.18:** Test batch download result aggregation
- [ ] **Task 11.19:** Test tracking file update
- [ ] **Task 11.20:** Test disk space error handling

---

## Phase 12: Integration Tests (5 tests)

- [ ] **Task 12.1:** Test download real PDF from JHB Water
- [ ] **Task 12.2:** Test batch download (5 PDFs) with concurrency
- [ ] **Task 12.3:** Test skip existing files on second run
- [ ] **Task 12.4:** Test fallback to browser (mock 403 response)
- [ ] **Task 12.5:** Test large file download (>10MB)

---

## Phase 13: Performance Validation (5 tasks)

- [ ] **Task 13.1:** Benchmark single download (500KB PDF)
- [ ] **Task 13.2:** Verify download time <5 seconds
- [ ] **Task 13.3:** Benchmark batch download (10 PDFs, concurrent)
- [ ] **Task 13.4:** Verify batch time <30 seconds
- [ ] **Task 13.5:** Test memory usage with 50MB PDF

---

## Phase 14: Configuration & Documentation (5 tasks)

- [ ] **Task 14.1:** Validate `config/sources/jhb_water.yaml` download section
- [ ] **Task 14.2:** Update `docs/API.md` with PDFDownloader API
- [ ] **Task 14.3:** Update `docs/NAVIGATION.md` with download examples
- [ ] **Task 14.4:** Create download troubleshooting guide
- [ ] **Task 14.5:** Document storage organization format

---

## Phase 15: Code Quality & Review (3 tasks)

- [ ] **Task 15.1:** Run black formatter on all files
- [ ] **Task 15.2:** Run ruff linter and fix issues
- [ ] **Task 15.3:** Code review by tech lead

---

## Definition of Done Checklist

- [ ] All 45 implementation tasks complete
- [ ] Unit test coverage >85%
- [ ] All integration tests passing
- [ ] Manual download of 10 real PDFs successful
- [ ] Performance targets met (see proposal.md)
- [ ] Documentation updated
- [ ] Code formatted and linted
- [ ] Code reviewed and approved
- [ ] Storage organization validated
- [ ] Tracking file integration tested

---

## Task Ownership

**Assigned To:** [Implementation Developer]
**Reviewer:** [Tech Lead]
**Approver:** [Product Owner]

---

## Progress Tracking

**Started:** [YYYY-MM-DD]
**Target Completion:** [YYYY-MM-DD]
**Actual Completion:** [YYYY-MM-DD]

Update this file as tasks are completed. Use `/openspec-apply` to execute implementation.

---

**Task Status:** 🟡 NOT STARTED
**Last Updated:** 2026-01-12
