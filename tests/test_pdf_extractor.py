"""
Unit tests for PDFExtractor.

Tests cover:
- ExtractionResult and PDFMetadata models
- Path building
- Skip logic
- Metadata extraction
- Text extraction (pdfplumber)
- Scanned detection
- OCR extraction (mocked)
- Single and batch extraction
"""

import json
from datetime import datetime
from pathlib import Path
from unittest.mock import MagicMock, patch
import pytest

from src.extractor import PDFExtractor, ExtractionResult, PDFMetadata


# ============================================================================
# Fixtures
# ============================================================================


@pytest.fixture
def extraction_config():
    """Create a sample extraction configuration."""
    return {
        "method": "auto",
        "ocr_language": "eng",
        "ocr_confidence_threshold": 0.7,
        "min_text_length": 100,
        "max_concurrent": 3,
        "skip_existing": True,
        "output_base_path": "documents/extracted",
    }


@pytest.fixture
def sample_pdf_path(tmp_path):
    """Create a sample PDF file path."""
    pdf_dir = tmp_path / "documents" / "downloads" / "jhb-water" / "2026-01"
    pdf_dir.mkdir(parents=True, exist_ok=True)
    pdf_file = pdf_dir / "RFQ123.pdf"
    pdf_file.write_bytes(b"%PDF-1.4\ntest content")
    return pdf_file


# ============================================================================
# Model Tests
# ============================================================================


def test_extraction_result_creation():
    """Test ExtractionResult dataclass creation."""
    result = ExtractionResult(
        pdf_path=Path("/tmp/test.pdf"),
        success=True,
        text_path=Path("/tmp/test.txt"),
        metadata_path=Path("/tmp/test_meta.json"),
        page_count=10,
        text_length=5000,
        extraction_method="pdfplumber",
        ocr_confidence=None,
        extraction_time_ms=1500,
        error=None,
    )

    assert result.success is True
    assert result.page_count == 10
    assert result.text_length == 5000
    assert result.extraction_method == "pdfplumber"


def test_extraction_result_to_dict():
    """Test ExtractionResult serialization."""
    result = ExtractionResult(
        pdf_path=Path("/tmp/test.pdf"),
        success=True,
        text_path=Path("/tmp/test.txt"),
        metadata_path=Path("/tmp/test_meta.json"),
        page_count=10,
        text_length=5000,
        extraction_method="pdfplumber",
        ocr_confidence=None,
        extraction_time_ms=1500,
        error=None,
    )

    result_dict = result.to_dict()

    assert result_dict["pdf_path"] == "/tmp/test.pdf"
    assert result_dict["success"] is True
    assert result_dict["page_count"] == 10
    assert result_dict["extraction_method"] == "pdfplumber"


def test_pdf_metadata_creation():
    """Test PDFMetadata dataclass creation."""
    metadata = PDFMetadata(
        title="Test Document",
        author="Test Author",
        subject="Test Subject",
        keywords="test, keywords",
        creation_date=datetime(2026, 1, 15, 10, 0, 0),
        modification_date=datetime(2026, 1, 16, 14, 30, 0),
        page_count=10,
        file_size=524288,
        pdf_version="1.4",
        is_encrypted=False,
        is_scanned=False,
    )

    assert metadata.title == "Test Document"
    assert metadata.page_count == 10
    assert metadata.is_scanned is False


def test_pdf_metadata_serialization():
    """Test PDFMetadata to_dict and from_dict."""
    metadata = PDFMetadata(
        title="Test Document",
        author="Test Author",
        subject=None,
        keywords=None,
        creation_date=datetime(2026, 1, 15, 10, 0, 0),
        modification_date=None,
        page_count=10,
        file_size=524288,
        pdf_version="1.4",
        is_encrypted=False,
        is_scanned=False,
    )

    # Serialize
    metadata_dict = metadata.to_dict()

    assert metadata_dict["title"] == "Test Document"
    assert metadata_dict["creation_date"] == "2026-01-15T10:00:00"
    assert metadata_dict["modification_date"] is None

    # Deserialize
    restored_metadata = PDFMetadata.from_dict(metadata_dict)

    assert restored_metadata.title == "Test Document"
    assert restored_metadata.page_count == 10
    assert restored_metadata.creation_date == datetime(2026, 1, 15, 10, 0, 0)


# ============================================================================
# Path Building Tests
# ============================================================================


def test_build_output_paths(extraction_config, sample_pdf_path):
    """Test output path building."""
    extractor = PDFExtractor(extraction_config)

    text_path, metadata_path = extractor._build_output_paths(sample_pdf_path)

    assert text_path == Path("documents/extracted/jhb-water/2026-01/RFQ123.txt")
    assert metadata_path == Path(
        "documents/extracted/jhb-water/2026-01/RFQ123_meta.json"
    )


def test_build_output_paths_fallback(extraction_config, tmp_path):
    """Test output path building with non-standard PDF path."""
    extractor = PDFExtractor(extraction_config)

    # PDF path that doesn't match expected format
    pdf_path = tmp_path / "random" / "location" / "test.pdf"

    text_path, metadata_path = extractor._build_output_paths(pdf_path)

    # Should fall back to "unknown" source
    assert "unknown" in str(text_path)
    assert text_path.name == "test.txt"
    assert metadata_path.name == "test_meta.json"


# ============================================================================
# Skip Logic Tests
# ============================================================================


def test_should_skip_file_exists(extraction_config, sample_pdf_path, tmp_path):
    """Test skip logic when text file exists."""
    extractor = PDFExtractor(extraction_config)

    # Create existing text file
    text_path = tmp_path / "existing.txt"
    text_path.write_text("existing content")

    should_skip = extractor._should_skip(sample_pdf_path, text_path)

    assert should_skip is True


def test_should_skip_file_not_exists(extraction_config, sample_pdf_path, tmp_path):
    """Test skip logic when text file doesn't exist."""
    extractor = PDFExtractor(extraction_config)

    text_path = tmp_path / "nonexistent.txt"

    should_skip = extractor._should_skip(sample_pdf_path, text_path)

    assert should_skip is False


# ============================================================================
# Metadata Extraction Tests (Mocked)
# ============================================================================


def test_extract_metadata_basic(extraction_config, sample_pdf_path):
    """Test metadata extraction from PDF."""
    extractor = PDFExtractor(extraction_config)

    # Mock pdfplumber
    mock_pdf = MagicMock()
    mock_pdf.metadata = {
        "Title": "Test RFQ Document",
        "Author": "Johannesburg Water",
        "PDFVersion": "1.4",
    }
    mock_pdf.pages = [MagicMock(), MagicMock(), MagicMock()]  # 3 pages
    mock_pdf.is_encrypted = False

    with patch("pdfplumber.open", return_value=mock_pdf):
        mock_pdf.__enter__ = MagicMock(return_value=mock_pdf)
        mock_pdf.__exit__ = MagicMock(return_value=None)

        metadata = extractor.extract_metadata(sample_pdf_path)

        assert metadata.title == "Test RFQ Document"
        assert metadata.author == "Johannesburg Water"
        assert metadata.page_count == 3
        assert metadata.pdf_version == "1.4"
        assert metadata.is_encrypted is False


def test_extract_metadata_with_dates(extraction_config, sample_pdf_path):
    """Test metadata extraction with dates."""
    extractor = PDFExtractor(extraction_config)

    mock_pdf = MagicMock()
    mock_pdf.metadata = {
        "CreationDate": "D:20260115100000",
        "ModDate": "D:20260116143000",
    }
    mock_pdf.pages = [MagicMock()]
    mock_pdf.is_encrypted = False

    with patch("pdfplumber.open", return_value=mock_pdf):
        mock_pdf.__enter__ = MagicMock(return_value=mock_pdf)
        mock_pdf.__exit__ = MagicMock(return_value=None)

        metadata = extractor.extract_metadata(sample_pdf_path)

        assert metadata.creation_date == datetime(2026, 1, 15, 10, 0, 0)
        assert metadata.modification_date == datetime(2026, 1, 16, 14, 30, 0)


# ============================================================================
# Text Extraction Tests (Mocked)
# ============================================================================


def test_extract_text_pdfplumber(extraction_config, sample_pdf_path):
    """Test text extraction with pdfplumber."""
    extractor = PDFExtractor(extraction_config)

    # Mock PDF with text content
    mock_page1 = MagicMock()
    mock_page1.extract_text.return_value = "Page 1 content with tender details."
    mock_page2 = MagicMock()
    mock_page2.extract_text.return_value = "Page 2 content with pricing info."

    mock_pdf = MagicMock()
    mock_pdf.pages = [mock_page1, mock_page2]
    mock_pdf.metadata = {"Title": "Test"}
    mock_pdf.is_encrypted = False

    with patch("pdfplumber.open", return_value=mock_pdf):
        mock_pdf.__enter__ = MagicMock(return_value=mock_pdf)
        mock_pdf.__exit__ = MagicMock(return_value=None)

        extracted_text, metadata = extractor.extract_text_pdfplumber(sample_pdf_path)

        assert "Page 1" in extracted_text
        assert "Page 2" in extracted_text
        assert "tender details" in extracted_text
        assert "pricing info" in extracted_text


def test_extract_text_pdfplumber_empty_page(extraction_config, sample_pdf_path):
    """Test text extraction with empty page."""
    extractor = PDFExtractor(extraction_config)

    mock_page1 = MagicMock()
    mock_page1.extract_text.return_value = "Page 1 content"
    mock_page2 = MagicMock()
    mock_page2.extract_text.return_value = None  # Empty page

    mock_pdf = MagicMock()
    mock_pdf.pages = [mock_page1, mock_page2]
    mock_pdf.metadata = {}
    mock_pdf.is_encrypted = False

    with patch("pdfplumber.open", return_value=mock_pdf):
        mock_pdf.__enter__ = MagicMock(return_value=mock_pdf)
        mock_pdf.__exit__ = MagicMock(return_value=None)

        extracted_text, metadata = extractor.extract_text_pdfplumber(sample_pdf_path)

        assert "Page 1" in extracted_text
        assert "Page 2" in extracted_text
        assert "[No text on this page]" in extracted_text


# ============================================================================
# Scanned Detection Tests
# ============================================================================


def test_detect_scanned_digital_pdf(extraction_config, sample_pdf_path):
    """Test scanned detection for digital PDF (has text)."""
    extractor = PDFExtractor(extraction_config)

    # Mock pages with text (digital PDF) - need >50 chars per page
    mock_pages = []
    for i in range(5):
        mock_page = MagicMock()
        # Long text to ensure it's detected as digital (>50 chars)
        mock_page.extract_text.return_value = (
            f"Page {i} with lots of text content. " * 5  # ~200 chars
        )
        mock_pages.append(mock_page)

    mock_pdf = MagicMock()
    mock_pdf.pages = mock_pages

    with patch("pdfplumber.open", return_value=mock_pdf):
        mock_pdf.__enter__ = MagicMock(return_value=mock_pdf)
        mock_pdf.__exit__ = MagicMock(return_value=None)

        is_scanned = extractor.detect_scanned(sample_pdf_path)

        assert is_scanned is False  # Digital PDF, not scanned


def test_detect_scanned_scanned_pdf(extraction_config, sample_pdf_path):
    """Test scanned detection for scanned PDF (no text)."""
    extractor = PDFExtractor(extraction_config)

    # Mock pages with no text (scanned PDF)
    mock_pages = []
    for i in range(5):
        mock_page = MagicMock()
        mock_page.extract_text.return_value = ""  # No text
        mock_pages.append(mock_page)

    mock_pdf = MagicMock()
    mock_pdf.pages = mock_pages

    with patch("pdfplumber.open", return_value=mock_pdf):
        mock_pdf.__enter__ = MagicMock(return_value=mock_pdf)
        mock_pdf.__exit__ = MagicMock(return_value=None)

        is_scanned = extractor.detect_scanned(sample_pdf_path)

        assert is_scanned is True  # Scanned PDF


# ============================================================================
# OCR Extraction Tests (Mocked)
# ============================================================================


def test_extract_text_ocr_mocked(extraction_config, sample_pdf_path):
    """Test OCR text extraction (mocked pytesseract)."""
    extractor = PDFExtractor(extraction_config)

    # Mock pdfplumber page to image
    mock_image = MagicMock()
    mock_image.original = MagicMock()  # PIL Image

    mock_page = MagicMock()
    mock_page.to_image.return_value = mock_image

    mock_pdf = MagicMock()
    mock_pdf.pages = [mock_page]

    # Mock pytesseract OCR output
    mock_ocr_data = {
        "text": ["Request", "for", "Quotation", "RFQ123"],
        "conf": [85, 90, 88, 92],  # Confidence scores
    }

    with patch("pdfplumber.open", return_value=mock_pdf):
        with patch("pytesseract.image_to_data", return_value=mock_ocr_data):
            mock_pdf.__enter__ = MagicMock(return_value=mock_pdf)
            mock_pdf.__exit__ = MagicMock(return_value=None)

            extracted_text, confidence = extractor.extract_text_ocr(sample_pdf_path)

            assert "Request" in extracted_text
            assert "Quotation" in extracted_text
            assert "RFQ123" in extracted_text
            assert confidence > 0.8  # High confidence


# ============================================================================
# Single Extraction Tests
# ============================================================================


@pytest.mark.asyncio
async def test_extract_single_skip_existing(
    extraction_config, sample_pdf_path, tmp_path
):
    """Test extract_single skips if already extracted."""
    extractor = PDFExtractor(extraction_config)

    # Create existing text file
    text_dir = tmp_path / "documents" / "extracted" / "jhb-water" / "2026-01"
    text_dir.mkdir(parents=True, exist_ok=True)
    text_path = text_dir / "RFQ123.txt"
    text_path.write_text("Existing extracted text content")

    metadata_path = text_dir / "RFQ123_meta.json"
    metadata = {
        "page_count": 5,
        "file_size": 100000,
        "is_scanned": False,
    }
    with open(metadata_path, "w") as f:
        json.dump(metadata, f)

    # Override output base path
    extractor.output_base_path = tmp_path / "documents" / "extracted"

    result = await extractor.extract_single(sample_pdf_path)

    assert result.success is True
    assert result.extraction_method == "skip"
    assert result.extraction_time_ms == 0  # Skip should be instant
    assert result.page_count == 5


# ============================================================================
# Batch Extraction Tests
# ============================================================================


@pytest.mark.asyncio
async def test_extract_batch_concurrent(extraction_config, tmp_path):
    """Test batch extraction with concurrency."""
    extractor = PDFExtractor(extraction_config)

    # Create multiple PDF paths
    pdf_paths = []
    for i in range(5):
        pdf_dir = tmp_path / "pdfs"
        pdf_dir.mkdir(parents=True, exist_ok=True)
        pdf_file = pdf_dir / f"doc{i}.pdf"
        pdf_file.write_bytes(b"%PDF-1.4\ntest")
        pdf_paths.append(pdf_file)

    # Mock extract_single to return success
    async def mock_extract_single(pdf_path):
        return ExtractionResult(
            pdf_path=pdf_path,
            success=True,
            text_path=Path(f"/tmp/{pdf_path.name}.txt"),
            metadata_path=Path(f"/tmp/{pdf_path.name}_meta.json"),
            page_count=10,
            text_length=5000,
            extraction_method="pdfplumber",
            ocr_confidence=None,
            extraction_time_ms=1000,
            error=None,
        )

    with patch.object(extractor, "extract_single", side_effect=mock_extract_single):
        results = await extractor.extract_batch(pdf_paths, max_concurrent=2)

        assert len(results) == 5
        assert all(r.success for r in results)


@pytest.mark.asyncio
async def test_extract_batch_with_failures(extraction_config, tmp_path):
    """Test batch extraction with some failures."""
    extractor = PDFExtractor(extraction_config)

    pdf_paths = []
    for i in range(3):
        pdf_file = tmp_path / f"doc{i}.pdf"
        pdf_file.write_bytes(b"%PDF-1.4\ntest")
        pdf_paths.append(pdf_file)

    # Mock: first succeeds, second fails, third succeeds
    results_to_return = [
        ExtractionResult(
            pdf_path=pdf_paths[0],
            success=True,
            text_path=Path("/tmp/doc0.txt"),
            metadata_path=Path("/tmp/doc0_meta.json"),
            page_count=10,
            text_length=5000,
            extraction_method="pdfplumber",
            ocr_confidence=None,
            extraction_time_ms=1000,
            error=None,
        ),
        ExtractionResult(
            pdf_path=pdf_paths[1],
            success=False,
            text_path=None,
            metadata_path=None,
            page_count=0,
            text_length=0,
            extraction_method="error",
            ocr_confidence=None,
            extraction_time_ms=500,
            error="Corrupted PDF",
        ),
        ExtractionResult(
            pdf_path=pdf_paths[2],
            success=True,
            text_path=Path("/tmp/doc2.txt"),
            metadata_path=Path("/tmp/doc2_meta.json"),
            page_count=15,
            text_length=8000,
            extraction_method="pdfplumber",
            ocr_confidence=None,
            extraction_time_ms=1500,
            error=None,
        ),
    ]

    call_count = [0]

    async def mock_extract_single(pdf_path):
        result = results_to_return[call_count[0]]
        call_count[0] += 1
        return result

    with patch.object(extractor, "extract_single", side_effect=mock_extract_single):
        results = await extractor.extract_batch(pdf_paths)

        assert len(results) == 3
        assert results[0].success is True
        assert results[1].success is False
        assert results[2].success is True
