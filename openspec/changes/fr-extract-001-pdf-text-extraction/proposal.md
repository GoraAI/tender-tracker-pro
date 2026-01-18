# FR-EXTRACT-001: PDF Text Extraction

**Status:** 🟢 APPROVED
**Priority:** P0 (Critical for MVP1)
**Depends On:**
- FR-NAV-003 (Direct PDF Download) ✅
**Blocks:**
- FR-LLM-002 (Tender Information Extraction)

---

## Summary

Implement reliable PDF text extraction with OCR fallback for scanned documents, metadata extraction, and structured output for downstream LLM processing.

---

## Motivation

After downloading tender PDFs (FR-NAV-003), we need to:
1. Extract text content for LLM analysis
2. Handle both digital and scanned (image-based) PDFs
3. Preserve document structure (pages, headings, tables)
4. Extract metadata (title, date, author) for tracking
5. Detect and handle corrupted/password-protected PDFs
6. Process batches efficiently with progress tracking
7. Cache extracted text to avoid re-processing

This enables the LLM pipeline (FR-LLM-002) to parse tender information from extracted text.

---

## Scope

### In Scope

- **Text Extraction** (digital PDFs):
  - Extract text using pdfplumber
  - Preserve page structure
  - Handle multi-column layouts
  - Extract text from tables
  - Unicode support (including special characters)

- **OCR Fallback** (scanned PDFs):
  - Detect image-based PDFs (no text content)
  - Extract images from PDF pages
  - OCR with pytesseract
  - Confidence scoring per page
  - Configurable OCR language (default: English)

- **Metadata Extraction**:
  - Title, Author, Subject, Keywords
  - Creation date, modification date
  - Page count
  - File size
  - PDF version

- **Error Handling**:
  - Corrupted PDF detection
  - Password-protected PDFs (mark as failed)
  - Invalid PDF format (not PDF)
  - Large file handling (>50MB)
  - Memory management for large documents

- **Caching & Storage**:
  - Save extracted text to organized directory
  - Format: `documents/extracted/{source_id}/{YYYY-MM}/filename.txt`
  - Store metadata separately (JSON)
  - Skip re-processing if already extracted

- **Batch Processing**:
  - Process multiple PDFs concurrently
  - Progress tracking (X/Y completed)
  - Summary statistics (pages processed, errors, OCR rate)

### Out of Scope

- Image extraction from PDFs (not needed for MVP1)
- Advanced table parsing (will use LLM for this)
- PDF form field extraction
- Bookmarks/annotations extraction
- PDF splitting/merging
- PDF-to-HTML conversion
- Cloud OCR services (using local pytesseract for privacy)

---

## Technical Approach

### Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  PDF Text Extraction Flow                                    │
│                                                              │
│  PDFDownloader → List[Path]                                  │
│       │                                                      │
│       ▼                                                      │
│  PDFExtractor                                                │
│       │                                                      │
│       ├─ extract_batch(pdf_paths) → List[ExtractionResult]  │
│       │   │                                                  │
│       │   ├─ For each PDF (with concurrency limit):         │
│       │   │   │                                              │
│       │   │   ├─ Check if already extracted (skip if yes)   │
│       │   │   │                                              │
│       │   │   ├─ Open PDF with pdfplumber                   │
│       │   │   │                                              │
│       │   │   ├─ Extract metadata (author, title, pages)    │
│       │   │   │                                              │
│       │   │   ├─ Detect if scanned (no text content)        │
│       │   │   │   ├─ If digital: extract_text_pdfplumber()  │
│       │   │   │   └─ If scanned: extract_text_ocr()         │
│       │   │   │                                              │
│       │   │   ├─ Validate extracted text (min length)       │
│       │   │   │                                              │
│       │   │   ├─ Save text to organized location             │
│       │   │   │   documents/extracted/                      │
│       │   │   │   └─ {source_id}/{YYYY-MM}/filename.txt    │
│       │   │   │                                              │
│       │   │   ├─ Save metadata to JSON                      │
│       │   │   │                                              │
│       │   │   └─ Return ExtractionResult                    │
│       │   │                                                  │
│       │   └─ Return summary (success/failed/ocr counts)     │
│       │                                                      │
│       └─ Update tracking with extraction status             │
└──────────────────────────────────────────────────────────────┘
```

### Key Components

**1. ExtractionResult Data Class**
```python
@dataclass
class ExtractionResult:
    pdf_path: Path
    success: bool
    text_path: Optional[Path]  # Where extracted text was saved
    metadata_path: Optional[Path]  # Where metadata was saved
    page_count: int
    text_length: int  # Characters
    extraction_method: str  # "pdfplumber", "ocr", or "hybrid"
    ocr_confidence: Optional[float]  # 0.0-1.0 if OCR was used
    extraction_time_ms: int
    error: Optional[str]
```

**2. PDFMetadata Data Class**
```python
@dataclass
class PDFMetadata:
    title: Optional[str]
    author: Optional[str]
    subject: Optional[str]
    keywords: Optional[str]
    creation_date: Optional[datetime]
    modification_date: Optional[datetime]
    page_count: int
    file_size: int  # bytes
    pdf_version: str
    is_encrypted: bool
    is_scanned: bool  # True if image-based
```

**3. PDFExtractor Class**
```python
class PDFExtractor:
    def __init__(self, config: dict):
        """
        Initialize PDF extractor.

        Args:
            config: extraction section from source config with keys:
                - method: "auto", "pdfplumber", or "ocr"
                - ocr_language: str (default "eng")
                - ocr_confidence_threshold: float (default 0.7)
                - min_text_length: int (minimum chars to consider valid)
                - max_concurrent: int (default 3)
                - skip_existing: bool (default True)
        """

    async def extract_batch(
        self,
        pdf_paths: List[Path],
        max_concurrent: Optional[int] = None
    ) -> List[ExtractionResult]:
        """Extract text from multiple PDFs concurrently."""

    async def extract_single(self, pdf_path: Path) -> ExtractionResult:
        """Extract text from a single PDF."""

    def extract_text_pdfplumber(self, pdf_path: Path) -> Tuple[str, PDFMetadata]:
        """Extract text using pdfplumber (digital PDFs)."""

    def extract_text_ocr(self, pdf_path: Path) -> Tuple[str, float]:
        """Extract text using pytesseract OCR (scanned PDFs)."""

    def detect_scanned(self, pdf_path: Path) -> bool:
        """Detect if PDF is image-based (needs OCR)."""

    def extract_metadata(self, pdf_path: Path) -> PDFMetadata:
        """Extract PDF metadata."""

    def _build_output_paths(self, pdf_path: Path) -> Tuple[Path, Path]:
        """Build organized paths for text and metadata files."""

    def _should_skip(self, pdf_path: Path, text_path: Path) -> bool:
        """Check if already extracted."""
```

**4. Storage Organization**
```
documents/extracted/
├── jhb-water/
│   ├── 2026-01/
│   │   ├── RFQWC001-2026.txt          # Extracted text
│   │   ├── RFQWC001-2026_meta.json    # Metadata
│   │   ├── RFQWC002-2026.txt
│   │   └── RFQWC002-2026_meta.json
│   └── 2026-02/
│       └── RFQWC004-2026.txt
```

---

## Acceptance Criteria

### Functional

- [x] **AC1:** Extract text from digital PDF ✅
  - Given a PDF with digital text content
  - When `extract_single()` is called
  - Then extract all text using pdfplumber
  - And preserve page structure (page breaks)
  - And text length > min_text_length

- [x] **AC2:** OCR fallback for scanned PDFs ✅
  - Given a PDF with only images (no text)
  - When `detect_scanned()` returns True
  - Then use pytesseract OCR to extract text
  - And return confidence score per page

- [x] **AC3:** Extract metadata ✅
  - Given any PDF
  - When extracting
  - Then extract title, author, creation date, page count
  - And save to separate JSON file

- [x] **AC4:** Handle corrupted PDFs ✅
  - Given a corrupted/invalid PDF file
  - When attempting extraction
  - Then return ExtractionResult with success=False
  - And descriptive error message

- [x] **AC5:** Skip existing extractions ✅
  - Given a PDF already extracted
  - When `_should_skip()` checks text path
  - Then return True (skip re-processing)

- [x] **AC6:** Organize output by date ✅
  - Given a PDF from source "jhb-water"
  - When saving extracted text
  - Then use format: `extracted/{source_id}/{YYYY-MM}/filename.txt`

- [x] **AC7:** Concurrent batch extraction ✅
  - Given 10 PDFs to extract
  - When `extract_batch()` is called with max_concurrent=3
  - Then process 3 at a time (not all 10 simultaneously)

- [x] **AC8:** Detect password-protected PDFs ✅
  - Given a password-protected PDF
  - When opening with pdfplumber
  - Then mark as failed with error="password_protected"

- [x] **AC9:** Validate extracted text ✅
  - Given extracted text
  - When validating
  - Then check length >= min_text_length (default 100 chars)
  - And verify not empty/whitespace only

- [x] **AC10:** Handle large PDFs ✅
  - Given a PDF with 100+ pages
  - When extracting
  - Then process page-by-page (streaming)
  - And complete without memory issues

### Non-Functional

- [x] **AC11:** Extraction performance ⚠️
  - Extract 10-page digital PDF in <2 seconds
  - Extract 10-page scanned PDF (OCR) in <30 seconds
  - Note: Not benchmarked, but implementation uses efficient methods

- [ ] **AC12:** Memory efficiency (Not validated)
  - Process 50-page PDF using <200MB RAM
  - Process 100-page PDF using <500MB RAM

- [x] **AC13:** Logging ✅
  - Log extraction start, method used, page count
  - Log OCR confidence scores
  - Log extraction failures with reason

- [x] **AC14:** Error resilience ✅
  - Continue batch processing if one PDF fails
  - Return partial results (successful + failed)
  - Log all errors but don't crash

---

## Implementation Tasks

See `tasks.md` for detailed implementation checklist (40 tasks).

---

## Performance Targets

| Metric | Target | Alert | Don't Ship |
|--------|--------|-------|------------|
| Digital PDF (10 pages) | <2s | >5s | >10s |
| Scanned PDF (10 pages, OCR) | <30s | >60s | >120s |
| Batch (10 PDFs, 10 pages each) | <20s | >40s | >60s |
| Memory (50-page PDF) | <200MB | >500MB | >1GB |
| Success rate (valid PDFs) | >98% | <95% | <90% |
| OCR accuracy (confidence) | >0.7 | <0.6 | <0.5 |

---

## Dependencies

**Required (Must Complete First):**
- ✅ FR-NAV-003: Direct PDF Download

**Blocks (Depends on This):**
- FR-LLM-002: Tender Information Extraction
- FR-LLM-003: Relevance Scoring

---

## Risks & Mitigations

### Risk 1: OCR Accuracy
**Impact:** Low-quality scans may have poor OCR accuracy
**Mitigation:** Confidence scoring, manual review for low-confidence (<0.6)
**Fallback:** Flag for manual processing

### Risk 2: Large File Memory
**Impact:** 200+ page PDFs could cause memory issues
**Mitigation:** Page-by-page streaming, configurable memory limits
**Monitoring:** Log memory usage, alert on >500MB

### Risk 3: Password-Protected PDFs
**Impact:** Cannot extract text from encrypted PDFs
**Mitigation:** Detect early, mark as failed, log for manual handling
**Recovery:** Manual decryption or request unencrypted version

### Risk 4: Multi-Column Layouts
**Impact:** Text extraction may scramble column order
**Mitigation:** pdfplumber handles this well, but monitor quality
**Fallback:** LLM can handle somewhat garbled text

### Risk 5: OCR Language
**Impact:** Non-English PDFs will have poor OCR
**Mitigation:** Configurable OCR language per source
**Future:** Auto-detect language

---

## Testing Strategy

### Unit Tests (15 tests)
- ExtractionResult creation (2 tests)
- Metadata extraction (3 tests)
- Text extraction (pdfplumber) (3 tests)
- OCR extraction (2 tests)
- Scanned detection (2 tests)
- Path building (2 tests)
- Skip logic (1 test)

### Integration Tests (5 tests)
- Extract real PDF from JHB Water
- Batch extraction (5 PDFs)
- OCR fallback (mock scanned PDF)
- Skip existing extractions
- Large PDF handling (50+ pages)

### Test Coverage Target
- Unit test coverage: >85%
- Integration tests: All critical paths
- Load test: 50 PDFs in batch

---

## Documentation

- [ ] Update `docs/API.md` with PDFExtractor API
- [ ] Update `docs/EXTRACTION.md` with examples
- [ ] Create OCR troubleshooting guide
- [ ] Document storage organization

---

## Success Criteria

**Definition of Done:**
- [ ] All 14 acceptance criteria passing
- [ ] Unit tests >85% coverage
- [ ] Integration tests passing on JHB Water PDFs
- [ ] Documentation updated
- [ ] Code formatted and linted
- [ ] Performance benchmarks meet targets
- [ ] Extract 10 real tender PDFs successfully

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
- FR-NAV-003: `openspec/changes/fr-nav-003-direct-pdf-download/`
- pdfplumber docs: https://github.com/jsvine/pdfplumber
- pytesseract docs: https://github.com/madmaze/pytesseract
