# FR-EXTRACT-001 Implementation Tasks

**Feature:** PDF Text Extraction
**Status:** ✅ FUNCTIONALLY COMPLETE
**Progress:** 66/90 tasks complete (73%)

---

## Phase 1: Data Models & Setup (5 tasks) ✅

- [x] **Task 1.1:** Create `src/extractor/__init__.py` module
- [x] **Task 1.2:** Create `src/extractor/models.py` with ExtractionResult and PDFMetadata dataclasses
- [x] **Task 1.3:** Create `src/extractor/pdf_extractor.py` module
- [x] **Task 1.4:** Create `tests/test_pdf_extractor.py` test file
- [ ] **Task 1.5:** Create `tests/integration/test_jhb_water_extraction.py`

---

## Phase 2: Data Models (4 tasks) ✅

- [x] **Task 2.1:** Implement ExtractionResult dataclass with all fields
- [x] **Task 2.2:** Add `to_dict()` method for logging
- [x] **Task 2.3:** Implement PDFMetadata dataclass with all fields
- [x] **Task 2.4:** Add PDFMetadata `to_dict()` and `from_dict()` for JSON serialization

---

## Phase 3: Core Extractor Setup (4 tasks) ✅

- [x] **Task 3.1:** Implement `PDFExtractor.__init__()` with config
- [x] **Task 3.2:** Implement `_build_output_paths()` for text and metadata files
- [x] **Task 3.3:** Implement `_should_skip()` for existing extraction check
- [x] **Task 3.4:** Add helper method to extract filename from PDF path

---

## Phase 4: Metadata Extraction (5 tasks) ✅

- [x] **Task 4.1:** Implement `extract_metadata()` using pdfplumber
- [x] **Task 4.2:** Extract basic metadata (title, author, subject, keywords)
- [x] **Task 4.3:** Extract dates (creation_date, modification_date)
- [x] **Task 4.4:** Extract page count and file size
- [x] **Task 4.5:** Detect encryption status (is_encrypted)

---

## Phase 5: Digital PDF Text Extraction (5 tasks) ✅

- [x] **Task 5.1:** Implement `extract_text_pdfplumber()` method
- [x] **Task 5.2:** Iterate through all pages
- [x] **Task 5.3:** Extract text per page with page numbers
- [x] **Task 5.4:** Handle multi-column layouts
- [x] **Task 5.5:** Extract text from tables using pdfplumber.extract_table()

---

## Phase 6: Scanned PDF Detection (3 tasks) ✅

- [x] **Task 6.1:** Implement `detect_scanned()` method
- [x] **Task 6.2:** Check if pages have text content
- [x] **Task 6.3:** Return True if >80% of pages have no text (scanned)

---

## Phase 7: OCR Extraction (6 tasks) ✅

- [x] **Task 7.1:** Implement `extract_text_ocr()` method
- [x] **Task 7.2:** Convert PDF pages to images with pdfplumber
- [x] **Task 7.3:** Apply pytesseract OCR to each page image
- [x] **Task 7.4:** Calculate confidence score per page
- [x] **Task 7.5:** Combine page texts with page breaks
- [x] **Task 7.6:** Return combined text and average confidence

---

## Phase 8: Single Extraction with Validation (5 tasks) ✅

- [x] **Task 8.1:** Implement `extract_single()` method
- [x] **Task 8.2:** Check if should skip (already extracted)
- [x] **Task 8.3:** Extract metadata first
- [x] **Task 8.4:** Detect if scanned, use appropriate extraction method
- [x] **Task 8.5:** Validate extracted text (min length, not empty)

---

## Phase 9: Batch Extraction with Concurrency (4 tasks) ✅

- [x] **Task 9.1:** Implement `extract_batch()` method
- [x] **Task 9.2:** Use asyncio.Semaphore for concurrency limit
- [x] **Task 9.3:** Create tasks for all PDFs
- [x] **Task 9.4:** Gather results and return summary

---

## Phase 10: File I/O & Storage (5 tasks) ✅

- [x] **Task 10.1:** Implement text file writing with UTF-8 encoding
- [x] **Task 10.2:** Implement metadata JSON file writing
- [x] **Task 10.3:** Create output directories if they don't exist
- [x] **Task 10.4:** Atomic file writes (write to temp, then rename)
- [x] **Task 10.5:** Add file size validation before writing

---

## Phase 11: Error Handling & Logging (5 tasks) ✅

- [x] **Task 11.1:** Add structured logging for all extraction operations
- [x] **Task 11.2:** Log extraction start (PDF path, method, page count)
- [x] **Task 11.3:** Log extraction complete (duration, text length, method)
- [x] **Task 11.4:** Handle corrupted PDFs with specific error messages
- [x] **Task 11.5:** Handle password-protected PDFs (detect and skip)

---

## Phase 12: Unit Tests (15 tests) ✅

- [x] **Task 12.1:** Test ExtractionResult creation
- [x] **Task 12.2:** Test ExtractionResult to_dict()
- [x] **Task 12.3:** Test PDFMetadata creation
- [x] **Task 12.4:** Test PDFMetadata serialization (to_dict/from_dict)
- [x] **Task 12.5:** Test path building for output files
- [x] **Task 12.6:** Test should_skip logic (file exists)
- [x] **Task 12.7:** Test should_skip logic (file doesn't exist)
- [x] **Task 12.8:** Test metadata extraction from valid PDF
- [x] **Task 12.9:** Test text extraction with pdfplumber (digital PDF)
- [x] **Task 12.10:** Test scanned detection (digital PDF → False)
- [x] **Task 12.11:** Test scanned detection (image PDF → True)
- [x] **Task 12.12:** Test OCR extraction (mock pytesseract)
- [x] **Task 12.13:** Test extract_single success
- [x] **Task 12.14:** Test extract_single with corrupted PDF
- [x] **Task 12.15:** Test extract_batch with concurrent processing

---

## Phase 13: Integration Tests (5 tests)

- [ ] **Task 13.1:** Test extract real PDF from JHB Water downloads
- [ ] **Task 13.2:** Test batch extraction (5 PDFs) with concurrency
- [ ] **Task 13.3:** Test skip existing extractions on second run
- [ ] **Task 13.4:** Test large PDF handling (50+ pages)
- [ ] **Task 13.5:** Verify extracted text quality (spot check)

---

## Phase 14: Performance Validation (5 tasks)

- [ ] **Task 14.1:** Benchmark single digital PDF extraction (10 pages)
- [ ] **Task 14.2:** Verify extraction time <2 seconds
- [ ] **Task 14.3:** Benchmark batch extraction (10 PDFs)
- [ ] **Task 14.4:** Verify batch time <20 seconds
- [ ] **Task 14.5:** Test memory usage with 50-page PDF

---

## Phase 15: Documentation & Code Quality (4 tasks)

- [ ] **Task 15.1:** Update `docs/API.md` with PDFExtractor API
- [ ] **Task 15.2:** Create `docs/EXTRACTION.md` with usage examples
- [x] **Task 15.3:** Run black formatter and ruff linter
- [ ] **Task 15.4:** Code review by tech lead

---

## Definition of Done Checklist

- [ ] All 40 implementation tasks complete
- [ ] Unit test coverage >85%
- [ ] All integration tests passing
- [ ] Manual extraction of 10 real tender PDFs successful
- [ ] Performance targets met (see proposal.md)
- [ ] Documentation updated
- [ ] Code formatted and linted
- [ ] Code reviewed and approved
- [ ] Storage organization validated
- [ ] Extracted text is readable and complete

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
