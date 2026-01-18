# PDF Text Extraction Specification

## Overview

PDF text extraction module for Tender Tracker Pro that extracts text from both digital and scanned PDFs using pdfplumber and pytesseract OCR.

**Module:** `src/extractor/`
**Dependencies:** pdfplumber, pytesseract, structlog
**Status:** ✅ Implemented (FR-EXTRACT-001)

---

## Components

### Data Models (src/extractor/models.py)

#### PDFMetadata

Metadata extracted from a PDF file.

**Fields:**
- `title`: Optional[str] - Document title
- `author`: Optional[str] - Document author
- `subject`: Optional[str] - Document subject
- `keywords`: Optional[str] - Document keywords
- `creation_date`: Optional[datetime] - Creation timestamp
- `modification_date`: Optional[datetime] - Modification timestamp
- `page_count`: int - Number of pages
- `file_size`: int - File size in bytes
- `pdf_version`: str - PDF version (e.g., "1.4")
- `is_encrypted`: bool - Whether PDF is password-protected
- `is_scanned`: bool - Whether PDF is image-based (needs OCR)

**Methods:**
- `to_dict() -> dict` - Serialize to dictionary with ISO date format
- `from_dict(data: dict) -> PDFMetadata` - Deserialize from dictionary

#### ExtractionResult

Result of a PDF text extraction attempt.

**Fields:**
- `pdf_path`: Path - Original PDF file path
- `success`: bool - Whether extraction succeeded
- `text_path`: Optional[Path] - Path to extracted text file
- `metadata_path`: Optional[Path] - Path to metadata JSON file
- `page_count`: int - Number of pages processed
- `text_length`: int - Character count of extracted text
- `extraction_method`: str - Method used: "pdfplumber", "ocr", "skip", or "error"
- `ocr_confidence`: Optional[float] - OCR confidence (0.0-1.0) if OCR was used
- `extraction_time_ms`: int - Extraction duration in milliseconds
- `error`: Optional[str] - Error message if failed

**Methods:**
- `to_dict() -> dict` - Serialize to dictionary for logging

---

### PDFExtractor (src/extractor/pdf_extractor.py)

Main class for PDF text extraction with OCR fallback.

#### Configuration

```python
config = {
    "method": "auto",                    # "auto", "pdfplumber", or "ocr"
    "ocr_language": "eng",               # Tesseract language
    "ocr_confidence_threshold": 0.7,     # Minimum OCR confidence
    "min_text_length": 100,              # Minimum extracted text length
    "max_concurrent": 3,                 # Concurrent extraction limit
    "skip_existing": True,               # Skip already extracted files
    "output_base_path": "documents/extracted"
}
```

#### Public Methods

##### `async extract_single(pdf_path: Path) -> ExtractionResult`

Extract text from a single PDF.

**Process:**
1. Build output paths (text + metadata)
2. Check if already extracted (skip if configured)
3. Extract metadata
4. Detect if scanned (>80% pages with no text)
5. Choose extraction method (pdfplumber for digital, OCR for scanned)
6. Validate extracted text (minimum length)
7. Save text and metadata to organized paths
8. Return ExtractionResult

**Returns:** ExtractionResult with extraction details

##### `async extract_batch(pdf_paths: List[Path], max_concurrent: Optional[int] = None) -> List[ExtractionResult]`

Extract text from multiple PDFs concurrently.

**Parameters:**
- `pdf_paths`: List of PDF file paths
- `max_concurrent`: Override default concurrency limit

**Process:**
1. Create semaphore with concurrency limit
2. Create async tasks for all PDFs
3. Execute concurrently with semaphore
4. Handle exceptions (convert to failed ExtractionResult)
5. Log batch summary (success count, OCR count, total characters)

**Returns:** List of ExtractionResults (one per PDF)

##### `extract_text_pdfplumber(pdf_path: Path) -> Tuple[str, PDFMetadata]`

Extract text using pdfplumber (digital PDFs).

**Process:**
1. Open PDF with pdfplumber
2. Iterate through pages
3. Extract text per page
4. Add page markers: `=== Page N ===`
5. Handle empty pages: `[No text on this page]`
6. Extract metadata

**Returns:** (extracted_text, metadata)

##### `extract_text_ocr(pdf_path: Path) -> Tuple[str, float]`

Extract text using pytesseract OCR (scanned PDFs).

**Process:**
1. Open PDF with pdfplumber
2. Convert each page to image (300 DPI)
3. Apply pytesseract OCR with language setting
4. Calculate confidence score per page
5. Add page markers with confidence: `=== Page N (OCR confidence: 0.92) ===`
6. Calculate overall confidence (average across pages)

**Returns:** (extracted_text, overall_confidence)

##### `detect_scanned(pdf_path: Path) -> bool`

Detect if PDF is image-based (needs OCR).

**Algorithm:**
1. Open PDF with pdfplumber
2. Check each page for text content
3. Count pages with <50 characters as "no text"
4. Calculate scanned ratio: pages_with_no_text / total_pages
5. Return True if ratio > 80%

**Returns:** True if scanned, False if digital

##### `extract_metadata(pdf_path: Path) -> PDFMetadata`

Extract PDF metadata.

**Process:**
1. Open PDF with pdfplumber
2. Extract metadata dictionary
3. Parse PDF dates (format: `D:YYYYMMDDHHmmSS`)
4. Get page count from `len(pdf.pages)`
5. Get file size from `pdf_path.stat().st_size`
6. Check encryption status

**Returns:** PDFMetadata object

---

## Storage Organization

### Directory Structure

```
documents/
├── downloads/                   # Downloaded PDFs
│   └── {source_id}/
│       └── {YYYY-MM}/
│           └── filename.pdf
│
└── extracted/                   # Extracted text
    └── {source_id}/
        └── {YYYY-MM}/
            ├── filename.txt      # Extracted text
            └── filename_meta.json # Metadata
```

### Path Building

Given PDF path: `documents/downloads/jhb_water/2024-01/RFQ123.pdf`

Outputs:
- Text: `documents/extracted/jhb_water/2024-01/RFQ123.txt`
- Metadata: `documents/extracted/jhb_water/2024-01/RFQ123_meta.json`

**Fallback:** If PDF path doesn't match expected format, use source_id="unknown" and current month.

---

## Text Format

### Digital PDF (pdfplumber)

```
=== Page 1 ===
Request for Quotation
Project: Water Infrastructure

=== Page 2 ===
Scope of Work
1. Installation of pipes
2. Testing and commissioning
```

### Scanned PDF (OCR)

```
=== Page 1 (OCR confidence: 0.92) ===
Request for Quotation
Project: Water Infrastructure

=== Page 2 (OCR confidence: 0.88) ===
Scope of Work
1. Installation of pipes
2. Testing and commissioning
```

---

## Error Handling

### Corrupted PDF
- Return `ExtractionResult(success=False, error="PDF is corrupted")`
- Log error with structlog

### Password-Protected PDF
- Return `ExtractionResult(success=False, error="password_protected")`
- Detected via pdfplumber `is_encrypted` check

### Text Too Short
- Raise `ValueError` if extracted text < `min_text_length`
- Default minimum: 100 characters

### Batch Processing Errors
- Individual failures don't stop batch
- Failed PDFs return `ExtractionResult(success=False, error=...)`
- Batch continues processing remaining PDFs

---

## Performance

### Expected Performance
- **Digital PDF (10 pages):** <2 seconds
- **Scanned PDF (10 pages with OCR):** <30 seconds
- **Batch (10 PDFs, concurrency=3):** ~20 seconds

### Optimization
- Async/await with semaphore-based concurrency
- Thread pool for blocking I/O operations
- Skip already extracted files (configurable)
- Page-by-page streaming for large PDFs

---

## Logging

### Extraction Events

```python
# Batch start
logger.info("extraction_batch_started", total=10, concurrent=3)

# Single extraction success
logger.info("extraction_success",
    pdf_path="/path/to/file.pdf",
    page_count=10,
    text_length=5000,
    method="pdfplumber",
    duration_ms=1500
)

# Extraction failure
logger.error("extraction_failed",
    pdf_path="/path/to/file.pdf",
    error="PDF is corrupted"
)

# Batch complete
logger.info("extraction_batch_complete",
    total=10,
    success=9,
    failed=1,
    ocr_count=2,
    total_chars=50000
)
```

---

## Usage Example

```python
from src.extractor import PDFExtractor
from pathlib import Path

# Configure extractor
config = {
    "method": "auto",
    "ocr_language": "eng",
    "max_concurrent": 3,
    "output_base_path": "documents/extracted"
}

extractor = PDFExtractor(config)

# Extract single PDF
pdf_path = Path("documents/downloads/jhb_water/2024-01/RFQ123.pdf")
result = await extractor.extract_single(pdf_path)

if result.success:
    print(f"Extracted {result.text_length} chars")
    print(f"Method: {result.extraction_method}")
    print(f"Text saved to: {result.text_path}")
else:
    print(f"Failed: {result.error}")

# Extract batch
pdf_paths = list(Path("documents/downloads/jhb_water/2024-01").glob("*.pdf"))
results = await extractor.extract_batch(pdf_paths, max_concurrent=5)

success_count = sum(1 for r in results if r.success)
print(f"Extracted {success_count}/{len(results)} PDFs")
```

---

## Dependencies

### Python Packages
- **pdfplumber** (0.11.9) - Digital PDF text extraction
- **pytesseract** (0.3.10) - OCR for scanned PDFs
- **Pillow** (10.2.0) - Image processing for OCR
- **structlog** - Structured logging

### System Requirements
- **Tesseract OCR** - Must be installed on system (`apt-get install tesseract-ocr`)
- **Language data** - English by default (`tesseract-ocr-eng`)

---

## Testing

### Unit Tests (tests/test_pdf_extractor.py)

18 tests covering:
- Data model creation and serialization
- Path building logic
- Skip logic
- Metadata extraction (mocked)
- Text extraction with pdfplumber (mocked)
- Scanned detection
- OCR extraction (mocked)
- Single extraction with error handling
- Batch extraction with concurrency

**Coverage:** All core functionality covered with mocks

### Integration Tests (Manual)

Test with real PDFs from JHB Water downloads:
- Digital PDF extraction
- Scanned PDF with OCR
- Batch processing
- Skip existing files
- Large PDF (50+ pages)

---

## Future Enhancements

### Performance Optimizations
- Parallel page processing within single PDF
- Memory-mapped file reading for large PDFs
- Result caching with content hash

### Quality Improvements
- Table extraction preservation
- Multi-column layout handling
- Image caption extraction
- OCR preprocessing (deskew, denoise)

### Additional Features
- PDF/A validation
- Redaction detection
- Signature verification
- Form field extraction

---

**Implemented:** 2026-01-18
**Version:** 1.0
**Related Specs:** None (first extraction spec)
**Archived From:** openspec/changes/fr-extract-001-pdf-text-extraction
