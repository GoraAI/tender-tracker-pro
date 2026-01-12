# Tender Tracker Pro - Internal API Documentation

**Version:** 1.0.0
**Status:** Design Phase
**Purpose:** Internal API documentation for system components

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Navigator API](#navigator-api)
3. [Extractor API](#extractor-api)
4. [LLM API](#llm-api)
5. [Reporting API](#reporting-api)
6. [Notification API](#notification-api)
7. [Data Models](#data-models)

---

## Architecture Overview

Tender Tracker Pro is organized into modular layers:

```
src/
├── navigator/      # Browser automation & PDF discovery
├── extractors/     # PDF text extraction
├── llm/            # AI model management & analysis
├── reporting/      # CSV & dashboard generation
└── notifications/  # Email & Discord alerts
```

Each layer exposes a clean API for the next layer to consume.

---

## Navigator API

**Module:** `src.navigator`

### Classes

#### `BrowserEngine`

Manages Playwright browser instances and navigation with human-like behavior.

```python
from src.navigator import BrowserEngine

class BrowserEngine:
    """Playwright-based browser automation engine with retry logic and human-like behavior."""

    def __init__(self, config: dict):
        """
        Initialize browser engine.

        Args:
            config: Browser configuration with keys:
                - browser: {type: "chromium"|"firefox"|"webkit", headless: bool, viewport: {width, height}}
                - timeouts: {page_load: int, navigation: int}
                - delays: {min_ms: int, max_ms: int}
                - max_retries: int
                - screenshot_on_error: bool
        """

    async def initialize(self) -> None:
        """
        Launch browser instance.

        Raises:
            BrowserInitError: If browser fails to initialize
        """

    async def navigate(
        self,
        url: str,
        wait_for: str = "networkidle",
        timeout_seconds: Optional[int] = None
    ) -> Page:
        """
        Navigate to URL with retry logic and human-like delays.

        Args:
            url: Target URL
            wait_for: Wait condition ("load", "networkidle", "domcontentloaded")
            timeout_seconds: Page load timeout (default: from config)

        Returns:
            Playwright Page object

        Raises:
            NavigationError: If browser not initialized
            NavigationTimeout: If navigation exceeds timeout after retries
        """

    async def handle_cookie_consent(self, page: Page) -> bool:
        """
        Detect and accept cookie consent dialogs.

        Args:
            page: Playwright Page object

        Returns:
            True if consent dialog handled, False if not found
        """

    async def paginate(
        self,
        page: Page,
        next_button_selectors: list[str],
        max_pages: int = 5
    ) -> list[Page]:
        """
        Paginate through results by clicking Next buttons.

        Args:
            page: Initial page
            next_button_selectors: CSS selectors for Next button
            max_pages: Maximum pages to load

        Returns:
            List of Page objects (one per page loaded)
        """

    async def screenshot(
        self,
        page: Page,
        name: str,
        path: str = "logs/screenshots"
    ) -> str:
        """
        Capture full-page screenshot.

        Args:
            page: Playwright Page object
            name: Screenshot name prefix
            path: Save directory

        Returns:
            Screenshot file path

        Raises:
            ScreenshotError: If capture fails
        """

    async def close(self) -> None:
        """Close browser and clean up resources."""
```

**Usage:**
```python
from src.navigator import BrowserEngine

config = {
    "browser": {"type": "chromium", "headless": True, "viewport": {"width": 1920, "height": 1080}},
    "timeouts": {"page_load": 30, "navigation": 60},
    "delays": {"min_ms": 1000, "max_ms": 3000},
    "max_retries": 3,
    "screenshot_on_error": True
}

engine = BrowserEngine(config)
await engine.initialize()

# Navigate with automatic retry
page = await engine.navigate("https://example.com")

# Handle cookie consent
await engine.handle_cookie_consent(page)

# Paginate through results
pages = await engine.paginate(
    page,
    next_button_selectors=["a:has-text('Next')", ".pagination .next"],
    max_pages=5
)

# Take screenshot
screenshot_path = await engine.screenshot(page, "example_page")

# Cleanup
await engine.close()
```

**Features:**
- Automatic retry with exponential backoff (3 attempts by default)
- Human-like random delays (1-3 seconds) between actions
- Cookie consent detection and handling
- Pagination support with configurable selectors
- Screenshot capture on navigation failures
- Structured logging for debugging

---

#### `PDFDiscovery`

Discovers PDF links on loaded pages.

```python
from src.navigator.pdf_discovery import PDFDiscovery

class PDFDiscovery:
    """Discovers PDF links on web pages."""

    def __init__(self, patterns: list[str]):
        """
        Initialize PDF discovery.

        Args:
            patterns: List of regex patterns for PDF URLs
        """

    async def find_pdfs(self, page: Page) -> list[PDFLink]:
        """
        Find all PDF links on page.

        Args:
            page: Playwright Page object

        Returns:
            List of PDFLink objects with URL, text, context
        """

    def deduplicate(self, pdf_links: list[PDFLink]) -> list[PDFLink]:
        """
        Remove previously downloaded PDFs.

        Args:
            pdf_links: Discovered PDF links

        Returns:
            New PDF links only
        """
```

---

#### `PDFDownloader`

Downloads PDFs via browser.

```python
from src.navigator.pdf_downloader import PDFDownloader

class PDFDownloader:
    """Downloads PDFs using browser automation."""

    def __init__(self, download_dir: str, config: dict):
        """
        Initialize downloader.

        Args:
            download_dir: Base directory for downloads
            config: Download configuration
        """

    async def download(self, page: Page, pdf_url: str) -> DownloadResult:
        """
        Download PDF from URL.

        Args:
            page: Playwright Page object
            pdf_url: URL of PDF to download

        Returns:
            DownloadResult with path, status, size, error (if any)
        """

    def verify_pdf(self, file_path: str) -> bool:
        """
        Verify PDF integrity.

        Args:
            file_path: Path to downloaded file

        Returns:
            True if valid PDF, False otherwise
        """
```

---

## Extractor API

**Module:** `src.extractors`

### Classes

#### `PDFExtractor`

Extracts text and tables from PDFs.

```python
from src.extractors.pdf_extractor import PDFExtractor

class PDFExtractor:
    """Extracts text content from PDF documents."""

    def __init__(self, config: dict):
        """
        Initialize extractor.

        Args:
            config: Extraction configuration
        """

    def extract_text(self, pdf_path: str) -> ExtractionResult:
        """
        Extract text from PDF.

        Args:
            pdf_path: Path to PDF file

        Returns:
            ExtractionResult with text, tables, metadata

        Raises:
            ExtractionError: If extraction fails
        """

    def extract_tables(self, pdf_path: str) -> list[dict]:
        """
        Extract tables as structured data.

        Args:
            pdf_path: Path to PDF file

        Returns:
            List of tables as JSON dictionaries
        """
```

**Usage:**
```python
extractor = PDFExtractor(config)
result = extractor.extract_text("path/to/tender.pdf")

print(result.text)           # Full text content
print(result.tables)         # Extracted tables
print(result.page_count)     # Number of pages
print(result.ocr_used)       # Whether OCR was used
```

---

#### `OCRFallback`

Handles scanned PDFs via Tesseract OCR.

```python
from src.extractors.ocr_fallback import OCRFallback

class OCRFallback:
    """OCR extraction for scanned PDFs."""

    def __init__(self, language: str = "eng"):
        """
        Initialize OCR engine.

        Args:
            language: Tesseract language code
        """

    def extract_with_ocr(self, pdf_path: str) -> OCRResult:
        """
        Extract text using OCR.

        Args:
            pdf_path: Path to scanned PDF

        Returns:
            OCRResult with text and confidence scores
        """
```

---

## LLM API

**Module:** `src.llm`

### Classes

#### `ModelManager`

Manages Gemma 3n model loading and lifecycle.

```python
from src.llm.model_manager import ModelManager

class ModelManager:
    """Manages Gemma 3n model instances."""

    def __init__(self, config: dict):
        """
        Initialize model manager.

        Args:
            config: LLM configuration from app_config.yaml
        """

    def load_model(self, model_name: str) -> Llama:
        """
        Load model into memory.

        Args:
            model_name: "e2b" or "e4b"

        Returns:
            llama.cpp Llama instance

        Raises:
            ModelLoadError: If model loading fails
        """

    def unload_model(self, model_name: str):
        """Unload model from memory."""

    def get_model(self, model_name: str) -> Llama:
        """
        Get loaded model (loads if not already loaded).

        Args:
            model_name: "e2b" or "e4b"

        Returns:
            Llama instance
        """
```

---

#### `ModelRouter`

Routes inference requests to appropriate model (E2B vs E4B).

```python
from src.llm.router import ModelRouter

class ModelRouter:
    """Routes LLM requests to appropriate model."""

    def __init__(self, model_manager: ModelManager, config: dict):
        """
        Initialize router.

        Args:
            model_manager: ModelManager instance
            config: Routing configuration
        """

    def route(self, task: Task) -> str:
        """
        Determine which model to use.

        Args:
            task: Task with context and type

        Returns:
            Model name ("e2b" or "e4b")
        """
```

---

#### `ExtractionAgent`

Extracts structured information from PDF text.

```python
from src.llm.extraction_agent import ExtractionAgent

class ExtractionAgent:
    """Extracts structured tender information using LLM."""

    def __init__(self, model_manager: ModelManager, config: dict):
        """
        Initialize extraction agent.

        Args:
            model_manager: ModelManager instance
            config: Extraction prompt configuration
        """

    def extract_tender_info(self, pdf_text: str) -> TenderInfo:
        """
        Extract tender information from PDF text.

        Args:
            pdf_text: Extracted text from PDF

        Returns:
            TenderInfo object with all extracted fields

        Raises:
            ExtractionError: If extraction fails or JSON invalid
        """
```

**Usage:**
```python
agent = ExtractionAgent(model_manager, config)
tender_info = agent.extract_tender_info(pdf_text)

print(tender_info.tender_id)      # "RFQJW087TN25"
print(tender_info.title)          # "Hygiene Services"
print(tender_info.closing_date)   # datetime object
print(tender_info.industry)       # "HYGIENE"
```

---

#### `ScoringAgent`

Scores tender relevance based on company profile.

```python
from src.llm.scoring_agent import ScoringAgent

class ScoringAgent:
    """Scores tender relevance using LLM."""

    def __init__(self, model_manager: ModelManager, company_profile: dict, config: dict):
        """
        Initialize scoring agent.

        Args:
            model_manager: ModelManager instance
            company_profile: Company profile from config
            config: Scoring configuration
        """

    def score_tender(self, tender_info: TenderInfo) -> ScoringResult:
        """
        Score tender relevance.

        Args:
            tender_info: Extracted tender information

        Returns:
            ScoringResult with score 0-100, breakdown, rationale
        """
```

**Usage:**
```python
agent = ScoringAgent(model_manager, company_profile, config)
result = agent.score_tender(tender_info)

print(result.total_score)         # 85
print(result.priority)            # "High Priority"
print(result.industry_match)      # 22/25
print(result.location_match)      # 18/20
print(result.rationale)           # "Strong industry and location match..."
```

---

## Reporting API

**Module:** `src.reporting`

### Classes

#### `CSVGenerator`

Generates CSV reports of analyzed tenders.

```python
from src.reporting.csv_generator import CSVGenerator

class CSVGenerator:
    """Generates CSV reports."""

    def __init__(self, output_dir: str, config: dict):
        """
        Initialize CSV generator.

        Args:
            output_dir: Directory for CSV files
            config: CSV configuration
        """

    def generate_report(self, tenders: list[ProcessedTender], date: date) -> str:
        """
        Generate daily CSV report.

        Args:
            tenders: List of processed tenders
            date: Report date

        Returns:
            Path to generated CSV file
        """
```

---

#### `DashboardGenerator`

Generates HTML dashboard with visualizations.

```python
from src.reporting.dashboard import DashboardGenerator

class DashboardGenerator:
    """Generates HTML dashboard."""

    def __init__(self, output_dir: str, config: dict):
        """
        Initialize dashboard generator.

        Args:
            output_dir: Dashboard directory
            config: Dashboard configuration
        """

    def generate(self, tenders: list[ProcessedTender]):
        """
        Generate dashboard HTML and data files.

        Args:
            tenders: All tenders to display
        """
```

---

## Notification API

**Module:** `src.notifications`

### Classes

#### `EmailSender`

Sends email notifications via SMTP.

```python
from src.notifications.email_sender import EmailSender

class EmailSender:
    """Sends email notifications."""

    def __init__(self, config: dict):
        """
        Initialize email sender.

        Args:
            config: Email configuration from email_config.yaml
        """

    def send_tender_alert(self, tenders: list[ProcessedTender]):
        """
        Send email with high-priority tenders.

        Args:
            tenders: List of tenders with score ≥70

        Raises:
            EmailError: If sending fails after retries
        """
```

---

## Data Models

### PDFLink

```python
@dataclass
class PDFLink:
    """Discovered PDF link."""
    url: str
    link_text: str
    context: str
    discovered_at: datetime
```

### DownloadResult

```python
@dataclass
class DownloadResult:
    """PDF download result."""
    status: str  # "SUCCESS", "FAILED", "SKIPPED", "INVALID"
    file_path: Optional[str]
    file_size_bytes: int
    error: Optional[str]
    download_time_seconds: float
```

### ExtractionResult

```python
@dataclass
class ExtractionResult:
    """PDF extraction result."""
    text: str
    tables: list[dict]
    page_count: int
    ocr_used: bool
    extraction_time_seconds: float
    confidence_score: float  # 0-1 (lower if OCR used)
```

### TenderInfo

```python
@dataclass
class TenderInfo:
    """Extracted tender information."""
    tender_id: str
    title: str
    organization: str
    description: str
    closing_date: datetime
    closing_time: str
    contact_name: Optional[str]
    contact_email: Optional[str]
    contact_phone: Optional[str]
    budget_min: Optional[float]
    budget_max: Optional[float]
    industry_category: str  # ELECTRICAL, PLUMBING, etc.
    location: Optional[str]
    requirements: list[str]
    mandatory_documents: list[str]
    submission_method: Optional[str]
    submission_address: Optional[str]
    confidence_scores: dict[str, float]  # Field-level confidence
```

### ScoringResult

```python
@dataclass
class ScoringResult:
    """Tender relevance scoring result."""
    total_score: int  # 0-100
    priority: str  # "High Priority", "Medium Priority", "Low Priority"
    industry_match: int  # 0-25
    location_match: int  # 0-20
    size_fit: int  # 0-20
    capability_match: int  # 0-20
    deadline_feasibility: int  # 0-15
    rationale: str  # 1-2 sentence explanation
```

### ProcessedTender

```python
@dataclass
class ProcessedTender:
    """Fully processed tender with all analysis."""
    source_id: str
    pdf_url: str
    pdf_path: str
    tender_info: TenderInfo
    scoring_result: ScoringResult
    processed_at: datetime
```

---

## Error Handling

All APIs raise specific exception types:

```python
class TenderTrackerError(Exception):
    """Base exception."""

class NavigationError(TenderTrackerError):
    """Navigation/download failures."""

class ExtractionError(TenderTrackerError):
    """PDF extraction failures."""

class ModelError(TenderTrackerError):
    """LLM model loading/inference failures."""

class NotificationError(TenderTrackerError):
    """Email/notification failures."""
```

Usage:
```python
try:
    result = extractor.extract_text(pdf_path)
except ExtractionError as e:
    logger.error(f"Extraction failed: {e}")
    # Handle gracefully
```

---

## Logging

All components use structured logging:

```python
import structlog

logger = structlog.get_logger(__name__)

logger.info(
    "pdf_downloaded",
    source="jhb-water",
    tender_id="RFQJW087TN25",
    file_size_kb=245,
    duration_ms=1234
)
```

---

## Configuration

All components accept configuration dictionaries loaded from YAML:

```python
import yaml

with open("config/app_config.yaml") as f:
    config = yaml.safe_load(f)

extractor = PDFExtractor(config["extraction"])
model_manager = ModelManager(config["llm"])
```

---

## Testing

All APIs are unit tested with pytest:

```python
# tests/test_navigator.py
def test_pdf_discovery():
    """Test PDF link discovery."""
    discovery = PDFDiscovery(patterns=["*.pdf"])
    # Mock page with PDF links
    links = await discovery.find_pdfs(mock_page)
    assert len(links) > 0
    assert all(link.url.endswith(".pdf") for link in links)
```

Run tests:
```bash
pytest tests/ -v
```

---

## Future API Extensions

### Planned (MVP2)

- `MultiSourceOrchestrator`: Coordinate multiple source scrapers
- `DeduplicationEngine`: Cross-source duplicate detection
- `DiscordNotifier`: Discord webhook notifications

### Planned (MVP3)

- `ResponseGenerator`: AI-assisted tender response drafting
- `SearchDiscovery`: Google Search integration
- `TenderDatabase`: SQLite/PostgreSQL backend

---

## References

- Architecture: `docs/ARCHITECTURE.md`
- PRD: `docs/PRD.md`
- Code: `src/`

---

**Last Updated:** 2026-01-12
**Status:** Design Phase - APIs will be implemented during P0 feature development
