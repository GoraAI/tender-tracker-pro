"""
Unit tests for PDF discovery.

Tests cover:
- PDFLink data model serialization
- Pattern matching (include and exclude)
- Context extraction
- Deduplication logic
- Tracking file operations
- URL normalization
"""

import json
from datetime import datetime
from unittest.mock import AsyncMock, patch

import pytest
from playwright.async_api import Page

from src.navigator import PDFDiscovery, PDFLink


@pytest.fixture
def pdf_discovery_config():
    """Standard PDF discovery configuration for tests."""
    return {
        "patterns": [
            {"regex": r"/wp-content/uploads/\d{4}/\d{2}/.*\.pdf$"},
            {"regex": r"RFQ.*\.pdf$"},
        ],
        "exclude_patterns": [
            ".*archive.*",
            ".*old.*",
        ],
        "extract_context": {
            "enabled": True,
            "parent_selector": "tr",
            "max_length": 500,
        },
        "dedup_key": "url",
    }


@pytest.fixture
def sample_pdf_link():
    """Sample PDFLink for testing."""
    return PDFLink(
        url="https://example.com/rfq123.pdf",
        link_text="RFQ123 - Water Supply",
        title="Request for Quotation 123",
        context="RFQ123 Water Supply Equipment Deadline: 2026-01-20",
        discovered_at=datetime(2026, 1, 12, 14, 30, 0),
        source_url="https://example.com/tenders",
        source_id="test-source",
    )


# Task 7.1: Test PDFLink creation and serialization
def test_pdf_link_creation(sample_pdf_link):
    """Test PDFLink creation with all fields."""
    assert sample_pdf_link.url == "https://example.com/rfq123.pdf"
    assert sample_pdf_link.link_text == "RFQ123 - Water Supply"
    assert sample_pdf_link.title == "Request for Quotation 123"
    assert (
        sample_pdf_link.context == "RFQ123 Water Supply Equipment Deadline: 2026-01-20"
    )
    assert sample_pdf_link.source_id == "test-source"


# Task 7.2: Test PDFLink to_dict() and from_dict()
def test_pdf_link_to_dict(sample_pdf_link):
    """Test PDFLink serialization to dict."""
    data = sample_pdf_link.to_dict()

    assert data["url"] == "https://example.com/rfq123.pdf"
    assert data["link_text"] == "RFQ123 - Water Supply"
    assert data["title"] == "Request for Quotation 123"
    assert data["discovered_at"] == "2026-01-12T14:30:00"
    assert data["source_id"] == "test-source"


def test_pdf_link_from_dict(sample_pdf_link):
    """Test PDFLink deserialization from dict."""
    data = sample_pdf_link.to_dict()
    restored = PDFLink.from_dict(data)

    assert restored.url == sample_pdf_link.url
    assert restored.link_text == sample_pdf_link.link_text
    assert restored.discovered_at == sample_pdf_link.discovered_at


# Task 7.3: Test pattern matching with single pattern
def test_pattern_matching_single_pattern(pdf_discovery_config, tmp_path):
    """Test URL matching with a single pattern."""
    config = {
        "patterns": [{"regex": r"RFQ.*\.pdf$"}],
        "exclude_patterns": [],
        "dedup_key": "url",
    }
    discovery = PDFDiscovery(config, tmp_path / "tracking.json")

    assert discovery._matches_patterns("https://example.com/RFQ123.pdf") is True
    assert discovery._matches_patterns("https://example.com/invoice.pdf") is False


# Task 7.4: Test pattern matching with multiple patterns (OR logic)
def test_pattern_matching_multiple_patterns(pdf_discovery_config, tmp_path):
    """Test URL matching with multiple patterns (OR logic)."""
    discovery = PDFDiscovery(pdf_discovery_config, tmp_path / "tracking.json")

    # Should match first pattern
    assert (
        discovery._matches_patterns(
            "https://example.com/wp-content/uploads/2026/01/rfq.pdf"
        )
        is True
    )

    # Should match second pattern
    assert discovery._matches_patterns("https://example.com/RFQ123.pdf") is True

    # Should not match any pattern
    assert discovery._matches_patterns("https://example.com/invoice.pdf") is False


# Task 7.5: Test exclude patterns
def test_exclude_patterns(pdf_discovery_config, tmp_path):
    """Test exclude patterns filter out matching URLs."""
    discovery = PDFDiscovery(pdf_discovery_config, tmp_path / "tracking.json")

    # Should be excluded (contains "archive")
    assert (
        discovery._matches_exclude_patterns("https://example.com/archive/rfq.pdf")
        is True
    )

    # Should be excluded (contains "old")
    assert (
        discovery._matches_exclude_patterns("https://example.com/rfq_old.pdf") is True
    )

    # Should not be excluded
    assert discovery._matches_exclude_patterns("https://example.com/rfq.pdf") is False


# Task 7.6: Test relative URL to absolute URL conversion
def test_relative_url_conversion(pdf_discovery_config, tmp_path):
    """Test conversion of relative URLs to absolute."""
    discovery = PDFDiscovery(pdf_discovery_config, tmp_path / "tracking.json")

    # Relative URL
    absolute = discovery._make_absolute_url(
        "/wp-content/uploads/2026/01/rfq.pdf", "https://example.com/tenders"
    )
    assert absolute == "https://example.com/wp-content/uploads/2026/01/rfq.pdf"

    # Already absolute
    absolute = discovery._make_absolute_url(
        "https://other.com/rfq.pdf", "https://example.com/tenders"
    )
    assert absolute == "https://other.com/rfq.pdf"


# Task 7.10: Test deduplication with empty tracking file
def test_deduplication_empty_tracking(pdf_discovery_config, sample_pdf_link, tmp_path):
    """Test deduplication with no existing tracking."""
    discovery = PDFDiscovery(pdf_discovery_config, tmp_path / "tracking.json")

    pdf_links = [sample_pdf_link]
    new_links = discovery.deduplicate(pdf_links)

    # All should be new
    assert len(new_links) == 1
    assert new_links[0].url == sample_pdf_link.url


# Task 7.11: Test deduplication with existing URLs
def test_deduplication_with_existing_urls(
    pdf_discovery_config, sample_pdf_link, tmp_path
):
    """Test deduplication filters out existing URLs."""
    tracking_file = tmp_path / "tracking.json"

    # Create existing tracking file
    tracking_data = {
        "version": "1.0",
        "last_updated": "2026-01-12T10:00:00Z",
        "discovered_pdfs": {
            "https://example.com/rfq123.pdf": {
                "first_seen": "2026-01-12T10:00:00Z",
                "link_text": "RFQ123",
                "source_id": "test-source",
                "downloaded": False,
            }
        },
    }
    with open(tracking_file, "w") as f:
        json.dump(tracking_data, f)

    discovery = PDFDiscovery(pdf_discovery_config, tracking_file)

    # Sample PDF link has same URL as tracking file
    pdf_links = [sample_pdf_link]
    new_links = discovery.deduplicate(pdf_links)

    # Should be filtered out
    assert len(new_links) == 0


# Task 7.12: Test tracking file load (valid JSON)
def test_tracking_file_load_valid(pdf_discovery_config, tmp_path):
    """Test loading valid tracking file."""
    tracking_file = tmp_path / "tracking.json"

    tracking_data = {
        "version": "1.0",
        "last_updated": "2026-01-12T10:00:00Z",
        "discovered_pdfs": {
            "https://example.com/rfq1.pdf": {
                "first_seen": "2026-01-12T10:00:00Z",
                "link_text": "RFQ1",
                "source_id": "test",
                "downloaded": False,
            }
        },
    }
    with open(tracking_file, "w") as f:
        json.dump(tracking_data, f)

    discovery = PDFDiscovery(pdf_discovery_config, tracking_file)
    tracking = discovery.load_tracking()

    assert tracking["version"] == "1.0"
    assert len(tracking["discovered_pdfs"]) == 1


# Task 7.13: Test tracking file load (missing file)
def test_tracking_file_load_missing(pdf_discovery_config, tmp_path):
    """Test loading creates new tracking when file missing."""
    tracking_file = tmp_path / "tracking.json"

    discovery = PDFDiscovery(pdf_discovery_config, tracking_file)
    tracking = discovery.load_tracking()

    assert tracking["version"] == "1.0"
    assert tracking["discovered_pdfs"] == {}


# Task 7.14: Test tracking file save (atomic write)
def test_tracking_file_save(pdf_discovery_config, sample_pdf_link, tmp_path):
    """Test saving tracking file uses atomic write."""
    tracking_file = tmp_path / "tracking.json"

    discovery = PDFDiscovery(pdf_discovery_config, tracking_file)
    discovery.save_tracking([sample_pdf_link])

    # Verify file exists
    assert tracking_file.exists()

    # Verify temp file was cleaned up
    assert not tracking_file.with_suffix(".tmp").exists()

    # Verify content
    with open(tracking_file, "r") as f:
        tracking = json.load(f)

    assert sample_pdf_link.url in tracking["discovered_pdfs"]
    assert (
        tracking["discovered_pdfs"][sample_pdf_link.url]["link_text"]
        == "RFQ123 - Water Supply"
    )


# Task 7.15: Test discovery on page with no PDF links
@pytest.mark.asyncio
async def test_discovery_no_pdf_links(pdf_discovery_config, tmp_path):
    """Test discovery returns empty list when no PDFs found."""
    discovery = PDFDiscovery(pdf_discovery_config, tmp_path / "tracking.json")

    # Mock page with no anchors
    mock_page = AsyncMock(spec=Page)
    mock_page.url = "https://example.com/tenders"
    mock_page.query_selector_all = AsyncMock(return_value=[])

    pdf_links = await discovery.discover_pdfs(mock_page, "test-source")

    assert len(pdf_links) == 0


# Task 7.7: Test metadata extraction (href, text, title)
@pytest.mark.asyncio
async def test_metadata_extraction(pdf_discovery_config, tmp_path):
    """Test extraction of href, text, and title attributes."""
    discovery = PDFDiscovery(pdf_discovery_config, tmp_path / "tracking.json")

    # Mock page and anchor
    mock_page = AsyncMock(spec=Page)
    mock_page.url = "https://example.com/tenders"

    mock_anchor = AsyncMock()
    mock_anchor.get_attribute = AsyncMock(
        side_effect=lambda attr: {
            "href": "https://example.com/RFQ123.pdf",
            "title": "Request for Quotation 123",
        }.get(attr)
    )
    mock_anchor.inner_text = AsyncMock(return_value="RFQ123 - Water Supply")

    mock_page.query_selector_all = AsyncMock(return_value=[mock_anchor])

    # Mock context extraction to return empty
    with patch.object(discovery, "extract_context", return_value=""):
        pdf_links = await discovery.discover_pdfs(mock_page, "test-source")

    assert len(pdf_links) == 1
    link = pdf_links[0]
    assert link.url == "https://example.com/RFQ123.pdf"
    assert link.link_text == "RFQ123 - Water Supply"
    assert link.title == "Request for Quotation 123"


# Task 7.9: Test context truncation (max_length)
@pytest.mark.asyncio
async def test_context_truncation(pdf_discovery_config, tmp_path):
    """Test context is truncated to max_length."""
    config = pdf_discovery_config.copy()
    config["extract_context"]["max_length"] = 50

    discovery = PDFDiscovery(config, tmp_path / "tracking.json")

    # Mock element with long context
    mock_element = AsyncMock()
    long_text = "A" * 100
    mock_parent = AsyncMock()
    mock_parent.inner_text = AsyncMock(return_value=long_text)

    mock_element.evaluate_handle = AsyncMock(return_value=mock_parent)

    context = await discovery.extract_context(mock_element, config["extract_context"])

    assert len(context) == 53  # 50 + "..."
    assert context.endswith("...")
