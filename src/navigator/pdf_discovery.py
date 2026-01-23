"""
PDF link discovery on web pages.

Discovers PDF links using pattern matching, extracts metadata and context,
and tracks discovered PDFs to avoid reprocessing.
"""

import json
import re
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List
from urllib.parse import urljoin

import structlog
from playwright.async_api import ElementHandle, Page

from .models import PDFLink

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

        logger.info(
            "pdf_discovery_initialized",
            pattern_count=len(self.patterns),
            exclude_pattern_count=len(self.exclude_patterns),
            tracking_file=str(tracking_file),
        )

    def _compile_patterns(self, pattern_configs: List[dict]) -> List[re.Pattern]:
        """Compile regex patterns from config."""
        patterns = []
        for p in pattern_configs:
            try:
                patterns.append(re.compile(p["regex"]))
            except re.error as e:
                logger.error("invalid_regex_pattern", pattern=p["regex"], error=str(e))
        return patterns

    async def discover_pdfs(self, page: Page, source_id: str) -> List[PDFLink]:
        """
        Discover all PDF links on a page.

        Args:
            page: Playwright Page object
            source_id: Source identifier (e.g., "joburg")

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
                link_text = (await anchor.inner_text()) or ""
                title = await anchor.get_attribute("title")

                # Extract context
                context = await self.extract_context(
                    anchor, self.config.get("extract_context", {})
                )

                # Build PDFLink
                pdf_link = PDFLink(
                    url=absolute_url,
                    link_text=link_text.strip(),
                    title=title,
                    context=context,
                    discovered_at=datetime.now(),
                    source_url=page.url,
                    source_id=source_id,
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
        self, element: ElementHandle, context_config: dict
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

        logger.info(
            "deduplication_complete",
            total=len(pdf_links),
            new=len(new_links),
            duplicates=len(pdf_links) - len(new_links),
        )

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
                "discovered_pdfs": {},
            }

        try:
            with open(self.tracking_file, "r") as f:
                tracking = json.load(f)
                logger.info(
                    "tracking_file_loaded",
                    pdf_count=len(tracking.get("discovered_pdfs", {})),
                )
                return tracking
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
                "discovered_pdfs": {},
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
                "downloaded": False,
            }

        tracking["last_updated"] = datetime.now().isoformat()

        # Ensure directory exists
        self.tracking_file.parent.mkdir(parents=True, exist_ok=True)

        # Atomic write
        temp_file = self.tracking_file.with_suffix(".tmp")
        with open(temp_file, "w") as f:
            json.dump(tracking, f, indent=2)

        temp_file.rename(self.tracking_file)

        logger.info(
            "tracking_file_updated",
            path=str(self.tracking_file),
            total_pdfs=len(tracking["discovered_pdfs"]),
            new_pdfs=len(pdf_links),
        )
