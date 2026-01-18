# FR-NAV-002 Specification Delta

This document defines the exact specification changes for PDF Link Discovery.

---

## ADDED: PDFLink Data Model

```python
from dataclasses import dataclass
from datetime import datetime
from typing import Optional

@dataclass
class PDFLink:
    """Represents a discovered PDF link with metadata."""

    url: str  # Full absolute URL to PDF
    link_text: str  # Anchor text (innerText)
    title: Optional[str]  # Link title attribute
    context: str  # Surrounding text (e.g., from parent <tr>)
    discovered_at: datetime  # Discovery timestamp
    source_url: str  # Page URL where found
    source_id: str  # Source identifier (e.g., "jhb-water")

    def to_dict(self) -> dict:
        """Convert to dictionary for JSON serialization."""
        return {
            "url": self.url,
            "link_text": self.link_text,
            "title": self.title,
            "context": self.context,
            "discovered_at": self.discovered_at.isoformat(),
            "source_url": self.source_url,
            "source_id": self.source_id
        }

    @classmethod
    def from_dict(cls, data: dict) -> "PDFLink":
        """Create PDFLink from dictionary."""
        return cls(
            url=data["url"],
            link_text=data["link_text"],
            title=data.get("title"),
            context=data["context"],
            discovered_at=datetime.fromisoformat(data["discovered_at"]),
            source_url=data["source_url"],
            source_id=data["source_id"]
        )
```

---

## ADDED: PDFDiscovery Class

```python
from pathlib import Path
import re
import json
from typing import List, Dict, Any, Optional
from playwright.async_api import Page, ElementHandle
import structlog

logger = structlog.get_logger()

class PDFDiscovery:
    """
    Discovers PDF links on web pages using pattern matching.

    Features:
    - Regex pattern matching
    - Exclude pattern support
    - Context extraction from parent elements
    - Deduplication against tracking file
    - Atomic tracking file updates
    """

    def __init__(self, config: dict, tracking_file: Path):
        """
        Initialize PDF discovery engine.

        Args:
            config: pdf_discovery section from source config
                Required keys:
                - patterns: List[dict] with "regex" key
                - exclude_patterns: List[str] (optional)
                - extract_context: dict with enabled, parent_selector, max_length
                - dedup_key: str (default "url")
            tracking_file: Path to JSON tracking file
        """
        self.config = config
        self.tracking_file = tracking_file
        self.patterns = self._compile_patterns(config.get("patterns", []))
        self.exclude_patterns = self._compile_patterns(
            [{"regex": p} for p in config.get("exclude_patterns", [])]
        )

    def _compile_patterns(self, pattern_configs: List[dict]) -> List[re.Pattern]:
        """Compile regex patterns from config."""
        return [re.compile(p["regex"]) for p in pattern_configs]

    async def discover_pdfs(
        self,
        page: Page,
        source_id: str
    ) -> List[PDFLink]:
        """
        Discover all PDF links on a page.

        Args:
            page: Playwright Page object
            source_id: Source identifier (e.g., "jhb-water")

        Returns:
            List of PDFLink objects (deduplicated, filtered)

        Process:
        1. Query all <a> tags
        2. Filter by patterns
        3. Extract metadata and context
        4. Deduplicate against tracking file
        5. Return new links only
        """
        logger.info("discovering_pdfs", source_id=source_id, url=page.url)

        # Query all anchor tags
        anchors = await page.query_selector_all("a")
        logger.debug("found_anchors", count=len(anchors))

        pdf_links = []
        for anchor in anchors:
            try:
                # Extract href
                href = await anchor.get_attribute("href")
                if not href:
                    continue

                # Convert relative to absolute
                absolute_url = self._make_absolute_url(href, page.url)

                # Check patterns
                if not self._matches_patterns(absolute_url):
                    continue

                # Check exclude patterns
                if self._matches_exclude_patterns(absolute_url):
                    logger.debug("excluded_by_pattern", url=absolute_url)
                    continue

                # Extract metadata
                link_text = await anchor.inner_text() or ""
                title = await anchor.get_attribute("title")

                # Extract context
                context = await self.extract_context(
                    anchor,
                    self.config.get("extract_context", {})
                )

                # Build PDFLink
                pdf_link = PDFLink(
                    url=absolute_url,
                    link_text=link_text.strip(),
                    title=title,
                    context=context,
                    discovered_at=datetime.now(),
                    source_url=page.url,
                    source_id=source_id
                )
                pdf_links.append(pdf_link)

            except Exception as e:
                logger.warning("failed_to_process_anchor", error=str(e))
                continue

        logger.info("discovered_raw_pdfs", count=len(pdf_links))

        # Deduplicate
        new_links = self.deduplicate(pdf_links)
        logger.info("discovered_new_pdfs", count=len(new_links))

        return new_links

    def _make_absolute_url(self, href: str, page_url: str) -> str:
        """Convert relative URL to absolute."""
        from urllib.parse import urljoin
        return urljoin(page_url, href)

    def _matches_patterns(self, url: str) -> bool:
        """Check if URL matches any include pattern."""
        if not self.patterns:
            return True  # No patterns = match all
        return any(pattern.search(url) for pattern in self.patterns)

    def _matches_exclude_patterns(self, url: str) -> bool:
        """Check if URL matches any exclude pattern."""
        return any(pattern.search(url) for pattern in self.exclude_patterns)

    async def extract_context(
        self,
        element: ElementHandle,
        context_config: dict
    ) -> str:
        """
        Extract surrounding context for a PDF link.

        Args:
            element: The anchor element
            context_config: Configuration with:
                - enabled: bool
                - parent_selector: str (e.g., "tr")
                - max_length: int

        Returns:
            Context string (truncated to max_length)
        """
        if not context_config.get("enabled", False):
            return ""

        parent_selector = context_config.get("parent_selector")
        max_length = context_config.get("max_length", 500)

        if not parent_selector:
            return ""

        try:
            # Find parent element
            parent = await element.evaluate_handle(
                f'(el) => el.closest("{parent_selector}")'
            )
            if not parent:
                return ""

            # Extract text
            text = await parent.inner_text()

            # Truncate
            if len(text) > max_length:
                text = text[:max_length] + "..."

            return text.strip()

        except Exception as e:
            logger.warning("context_extraction_failed", error=str(e))
            return ""

    def deduplicate(self, pdf_links: List[PDFLink]) -> List[PDFLink]:
        """
        Filter out previously discovered PDFs.

        Args:
            pdf_links: List of discovered PDFLinks

        Returns:
            List of new PDFLinks (not in tracking file)
        """
        tracking = self.load_tracking()
        dedup_key = self.config.get("dedup_key", "url")

        # Build set of seen keys
        seen_keys = set(tracking.get("discovered_pdfs", {}).keys())

        # Filter
        new_links = []
        for link in pdf_links:
            key = getattr(link, dedup_key)
            if key not in seen_keys:
                new_links.append(link)

        return new_links

    def load_tracking(self) -> Dict[str, Any]:
        """
        Load tracking file.

        Returns:
            Tracking dict with structure:
            {
                "version": "1.0",
                "last_updated": "ISO timestamp",
                "discovered_pdfs": {
                    "url": {
                        "first_seen": "ISO timestamp",
                        "link_text": "...",
                        "source_id": "...",
                        "downloaded": bool
                    }
                }
            }
        """
        if not self.tracking_file.exists():
            logger.info("tracking_file_not_found", creating_new=True)
            return {
                "version": "1.0",
                "last_updated": datetime.now().isoformat(),
                "discovered_pdfs": {}
            }

        try:
            with open(self.tracking_file, "r") as f:
                return json.load(f)
        except json.JSONDecodeError as e:
            logger.error("tracking_file_corrupted", error=str(e))
            # Backup corrupted file
            backup_path = self.tracking_file.with_suffix(".backup.json")
            self.tracking_file.rename(backup_path)
            logger.info("tracking_file_backed_up", path=str(backup_path))
            # Return empty tracking
            return {
                "version": "1.0",
                "last_updated": datetime.now().isoformat(),
                "discovered_pdfs": {}
            }

    def save_tracking(self, pdf_links: List[PDFLink]) -> None:
        """
        Update tracking file with new PDFLinks.

        Uses atomic write (write to temp, then rename).

        Args:
            pdf_links: List of PDFLinks to add to tracking
        """
        tracking = self.load_tracking()

        # Update
        for link in pdf_links:
            tracking["discovered_pdfs"][link.url] = {
                "first_seen": link.discovered_at.isoformat(),
                "link_text": link.link_text,
                "source_id": link.source_id,
                "source_url": link.source_url,
                "downloaded": False
            }

        tracking["last_updated"] = datetime.now().isoformat()

        # Atomic write
        temp_file = self.tracking_file.with_suffix(".tmp")
        with open(temp_file, "w") as f:
            json.dump(tracking, f, indent=2)

        temp_file.rename(self.tracking_file)

        logger.info(
            "tracking_file_updated",
            path=str(self.tracking_file),
            total_pdfs=len(tracking["discovered_pdfs"])
        )
```

---

## MODIFIED: src/navigator/__init__.py

```python
"""
Navigator module for browser automation and web scraping.

Main components:
- BrowserEngine: Playwright-based browser navigation
- PDFDiscovery: Pattern-based PDF link finder
- PDFLink: PDF link data model
- Exceptions: Navigation-related errors
"""

from .browser_engine import BrowserEngine
from .pdf_discovery import PDFDiscovery
from .models import PDFLink
from .exceptions import (
    BrowserInitError,
    NavigationError,
    NavigationTimeout,
    ScreenshotError,
)

__all__ = [
    "BrowserEngine",
    "PDFDiscovery",
    "PDFLink",
    "NavigationError",
    "BrowserInitError",
    "NavigationTimeout",
    "ScreenshotError",
]
```

---

## ADDED: Tracking File Format

**Location:** `data/tracking/{source_id}_pdfs.json`

**Schema:**
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "version": {
      "type": "string",
      "const": "1.0"
    },
    "last_updated": {
      "type": "string",
      "format": "date-time"
    },
    "discovered_pdfs": {
      "type": "object",
      "patternProperties": {
        "^https?://.*\\.pdf$": {
          "type": "object",
          "properties": {
            "first_seen": {
              "type": "string",
              "format": "date-time"
            },
            "link_text": {
              "type": "string"
            },
            "source_id": {
              "type": "string"
            },
            "source_url": {
              "type": "string",
              "format": "uri"
            },
            "downloaded": {
              "type": "boolean"
            }
          },
          "required": ["first_seen", "link_text", "source_id", "downloaded"]
        }
      }
    }
  },
  "required": ["version", "last_updated", "discovered_pdfs"]
}
```

**Example:**
```json
{
  "version": "1.0",
  "last_updated": "2026-01-12T14:30:00Z",
  "discovered_pdfs": {
    "https://scm.johannesburgwater.co.za/wp-content/uploads/2026/01/RFQWC001-2026.pdf": {
      "first_seen": "2026-01-12T14:30:00Z",
      "link_text": "RFQWC001-2026 - Water Chlorination Equipment",
      "source_id": "jhb-water",
      "source_url": "https://scm.johannesburgwater.co.za/supply-chain/request-for-quotations/open-rfq-2/",
      "downloaded": true
    },
    "https://scm.johannesburgwater.co.za/wp-content/uploads/2026/01/RFQWC002-2026.pdf": {
      "first_seen": "2026-01-12T14:31:00Z",
      "link_text": "RFQWC002-2026 - Pump Maintenance Services",
      "source_id": "jhb-water",
      "source_url": "https://scm.johannesburgwater.co.za/supply-chain/request-for-quotations/open-rfq-2/",
      "downloaded": false
    }
  }
}
```

---

## MODIFIED: Configuration Schema

**File:** `config/sources/{source_id}.yaml`

**Added Section:** `pdf_discovery` (already exists in jhb_water.yaml)

No schema changes needed - configuration already defined.

---

## Testing Requirements

### Unit Test Coverage

**Minimum:** 85%

**Required Tests:**
- PDFLink serialization/deserialization (2 tests)
- Pattern matching (4 tests)
- Context extraction (2 tests)
- Deduplication (3 tests)
- Tracking file operations (4 tests)
- URL normalization (2 tests)

**Total:** 15+ unit tests

### Integration Test Coverage

**Required Tests:**
- Live site discovery (JHB Water)
- Multi-page discovery
- Deduplication across runs
- Exclude pattern validation
- Tracking persistence

**Total:** 5 integration tests

---

## Performance Specifications

| Operation | Target | Maximum |
|-----------|--------|---------|
| discover_pdfs() with 50 links | <2s | 5s |
| Pattern matching per link | <50ms | 100ms |
| Context extraction per link | <100ms | 200ms |
| Deduplication with 1000 entries | <500ms | 1s |
| Tracking file load | <100ms | 500ms |
| Tracking file save | <200ms | 1s |

---

## Error Handling Requirements

**Must Handle:**
- Missing tracking file → Create new
- Corrupted tracking file → Backup and recreate
- Invalid URLs → Log warning, skip
- Missing href attribute → Skip
- Context extraction failure → Return empty string
- Page with no PDF links → Return empty list

**Must Log:**
- Discovery start (source_id, url)
- Anchor count found
- Excluded URLs (by pattern)
- Raw PDF count
- New PDF count (after dedup)
- Tracking file updates
- All errors/warnings

---

## Backward Compatibility

**Breaking Changes:** None (new feature)

**Migrations Required:** None

---

## Future Enhancements (Out of Scope)

- Database storage (currently using JSON)
- Distributed tracking (multi-instance)
- Advanced pattern matching (CSS selectors, XPath)
- PDF metadata extraction (file size, date from URL)
- Fuzzy deduplication (similar URLs)

---

**Specification Version:** 1.0
**Last Updated:** 2026-01-12
