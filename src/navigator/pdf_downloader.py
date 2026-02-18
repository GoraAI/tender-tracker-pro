"""
PDF downloader with validation and retry logic.

This module provides functionality to download PDFs from discovered links with:
- Direct HTTP download (fast)
- Browser-based download fallback (for auth-required sites)
- PDF validation (header check, size limits)
- Retry logic with exponential backoff
- Concurrent downloads with rate limiting
- Organized storage by source and date
"""

from pathlib import Path
from typing import Any, Dict, List, Optional
import asyncio
from urllib.parse import urlparse, unquote
import time
import json
from datetime import datetime

import aiohttp
import structlog

from .models import PDFLink, DownloadResult
from .browser_engine import BrowserEngine

logger = structlog.get_logger()


class PDFDownloader:
    """
    Downloads PDFs from discovered links with validation and retry logic.

    Features:
    - Direct HTTP download (fast)
    - Browser-based download fallback (for auth-required sites)
    - PDF validation (header check, size limits)
    - Retry logic with exponential backoff
    - Concurrent downloads with rate limiting
    - Organized storage by source and date
    - Tracking file integration
    """

    def __init__(self, config: dict, browser_engine: Optional[BrowserEngine] = None):
        """
        Initialize PDF downloader.

        Args:
            config: download section from source config with keys:
                - method: "auto", "direct", or "browser"
                - path_template: str (storage path template)
                - validate: dict with min_size_bytes, max_size_mb, check_pdf_header
                - retry_attempts: int (default 3)
                - retry_delay_ms: int (base delay)
                - max_concurrent: int (default 3)
                - skip_existing: bool (default True)
                - compare_by: "size_and_name", "hash", or "name_only"
            browser_engine: Optional BrowserEngine for browser downloads
        """
        self.config = config
        self.browser_engine = browser_engine
        self.download_method = config.get("method", "auto")
        self.max_retries = config.get("retry_attempts", 3)
        self.retry_delay_ms = config.get("retry_delay_ms", 5000)
        self.max_concurrent = config.get("max_concurrent", 3)
        self.skip_existing = config.get("skip_existing", True)

        # Validation settings
        validate_config = config.get("validate", {})
        self.min_size = validate_config.get("min_size_bytes", 1000)
        self.max_size = validate_config.get("max_size_mb", 50) * 1024 * 1024
        self.check_pdf_header = validate_config.get("check_pdf_header", True)

        # HTTP session
        self.session: Optional[aiohttp.ClientSession] = None

    async def __aenter__(self):
        """Context manager entry."""
        # Disable SSL verification: SA government sites use local CAs not in default bundle
        connector = aiohttp.TCPConnector(ssl=False)
        self.session = aiohttp.ClientSession(connector=connector)
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        if self.session:
            await self.session.close()

    async def download_batch(
        self, pdf_links: List[PDFLink], max_concurrent: Optional[int] = None
    ) -> List[DownloadResult]:
        """
        Download multiple PDFs concurrently.

        Args:
            pdf_links: List of PDFLinks to download
            max_concurrent: Override default concurrency limit

        Returns:
            List of DownloadResults (one per PDFLink)
        """
        concurrent_limit = max_concurrent or self.max_concurrent
        semaphore = asyncio.Semaphore(concurrent_limit)

        async def download_with_limit(pdf_link: PDFLink) -> DownloadResult:
            async with semaphore:
                return await self.download_single(pdf_link)

        logger.info(
            "download_batch_started", total=len(pdf_links), concurrent=concurrent_limit
        )

        # Create tasks
        tasks = [download_with_limit(link) for link in pdf_links]

        # Execute concurrently
        results = await asyncio.gather(*tasks, return_exceptions=True)

        # Convert exceptions to failed DownloadResults
        processed_results = []
        for i, result in enumerate(results):
            if isinstance(result, Exception):
                processed_results.append(
                    DownloadResult(
                        pdf_link=pdf_links[i],
                        success=False,
                        file_path=None,
                        file_size=0,
                        download_time_ms=0,
                        method="error",
                        error=str(result),
                        attempts=1,
                    )
                )
            else:
                processed_results.append(result)

        # Log summary
        success_count = sum(1 for r in processed_results if r.success)
        logger.info(
            "download_batch_complete",
            total=len(processed_results),
            success=success_count,
            failed=len(processed_results) - success_count,
        )

        return processed_results

    async def download_single(
        self, pdf_link: PDFLink, method: Optional[str] = None
    ) -> DownloadResult:
        """
        Download a single PDF with retry logic.

        Args:
            pdf_link: PDFLink to download
            method: Override download method ("auto", "direct", "browser")

        Returns:
            DownloadResult with success status and file path
        """
        start_time = time.time()
        download_method = method or self.download_method

        # Build output path
        output_path = self._build_output_path(pdf_link)

        # Check if should skip
        if self.skip_existing and self._should_skip(pdf_link, output_path):
            logger.info(
                "download_skipped_existing", url=pdf_link.url, path=str(output_path)
            )
            return DownloadResult(
                pdf_link=pdf_link,
                success=True,
                file_path=output_path,
                file_size=output_path.stat().st_size,
                download_time_ms=0,
                method="skip",
                error=None,
                attempts=0,
            )

        # Retry loop
        last_error = None
        for attempt in range(1, self.max_retries + 1):
            try:
                logger.info(
                    "download_attempt",
                    url=pdf_link.url,
                    attempt=attempt,
                    method=download_method,
                )

                # Try download
                if download_method == "direct" or download_method == "auto":
                    success = await self._download_direct(pdf_link.url, output_path)
                    method_used = "direct"

                    # Fallback to browser on auth error
                    if (
                        not success
                        and download_method == "auto"
                        and self.browser_engine
                    ):
                        logger.info("download_fallback_to_browser", url=pdf_link.url)
                        success = await self._download_browser(
                            pdf_link.url, output_path
                        )
                        method_used = "browser"

                elif download_method == "browser":
                    if not self.browser_engine:
                        raise ValueError(
                            "Browser download requested but no BrowserEngine provided"
                        )
                    success = await self._download_browser(pdf_link.url, output_path)
                    method_used = "browser"
                else:
                    raise ValueError(f"Unknown download method: {download_method}")

                # Validate
                if success and self._validate_pdf(output_path):
                    duration_ms = int((time.time() - start_time) * 1000)
                    file_size = output_path.stat().st_size

                    logger.info(
                        "download_success",
                        url=pdf_link.url,
                        file_size=file_size,
                        duration_ms=duration_ms,
                        method=method_used,
                        attempts=attempt,
                    )

                    return DownloadResult(
                        pdf_link=pdf_link,
                        success=True,
                        file_path=output_path,
                        file_size=file_size,
                        download_time_ms=duration_ms,
                        method=method_used,
                        error=None,
                        attempts=attempt,
                    )
                else:
                    # Invalid PDF or download failed
                    if output_path.exists():
                        output_path.unlink()  # Delete invalid file
                    raise ValueError("PDF validation failed")

            except Exception as e:
                last_error = str(e)
                logger.warning(
                    "download_attempt_failed",
                    url=pdf_link.url,
                    attempt=attempt,
                    error=last_error,
                )

                # Exponential backoff
                if attempt < self.max_retries:
                    backoff_ms = self.retry_delay_ms * (2 ** (attempt - 1))
                    await asyncio.sleep(backoff_ms / 1000)

        # All attempts failed
        duration_ms = int((time.time() - start_time) * 1000)
        logger.error(
            "download_failed",
            url=pdf_link.url,
            error=last_error,
            attempts=self.max_retries,
        )

        return DownloadResult(
            pdf_link=pdf_link,
            success=False,
            file_path=None,
            file_size=0,
            download_time_ms=duration_ms,
            method=download_method,
            error=last_error,
            attempts=self.max_retries,
        )

    async def _download_direct(self, url: str, output_path: Path) -> bool:
        """
        Direct HTTP download using aiohttp.

        Args:
            url: PDF URL
            output_path: Where to save the file

        Returns:
            True if download succeeded, False otherwise
        """
        try:
            # Ensure directory exists
            output_path.parent.mkdir(parents=True, exist_ok=True)

            async with self.session.get(
                url, timeout=aiohttp.ClientTimeout(total=30)
            ) as response:
                # Check status
                if response.status == 403 or response.status == 401:
                    logger.debug(
                        "download_direct_auth_required", status=response.status
                    )
                    return False

                if response.status != 200:
                    logger.warning("download_direct_http_error", status=response.status)
                    return False

                # Check content-type (optional)
                content_type = response.headers.get("Content-Type", "")
                if content_type and "pdf" not in content_type.lower():
                    logger.debug("download_direct_not_pdf", content_type=content_type)

                # Stream to file
                with open(output_path, "wb") as f:
                    async for chunk in response.content.iter_chunked(8192):
                        f.write(chunk)

                return True

        except asyncio.TimeoutError:
            logger.warning("download_direct_timeout", url=url)
            return False
        except Exception as e:
            logger.warning("download_direct_error", url=url, error=str(e))
            return False

    async def _download_browser(self, url: str, output_path: Path) -> bool:
        """
        Browser-based download using Playwright.

        Args:
            url: PDF URL
            output_path: Where to save the file

        Returns:
            True if download succeeded, False otherwise
        """
        if not self.browser_engine or not self.browser_engine.context:
            return False

        try:
            # Set download directory
            temp_download_dir = Path("/tmp/tender_downloads")
            temp_download_dir.mkdir(parents=True, exist_ok=True)

            # Navigate to PDF URL
            page = await self.browser_engine.context.new_page()

            # Wait for download
            async with page.expect_download(timeout=60000) as download_info:
                await page.goto(url, wait_until="commit")

            download = await download_info.value

            # Save to temp location
            temp_path = temp_download_dir / download.suggested_filename
            await download.save_as(temp_path)

            # Move to final location
            output_path.parent.mkdir(parents=True, exist_ok=True)
            temp_path.rename(output_path)

            await page.close()
            return True

        except Exception as e:
            logger.warning("download_browser_error", url=url, error=str(e))
            return False

    def _validate_pdf(self, file_path: Path) -> bool:
        """
        Validate PDF file.

        Args:
            file_path: Path to PDF file

        Returns:
            True if valid, False otherwise
        """
        if not file_path.exists():
            logger.warning("validation_file_not_found", path=str(file_path))
            return False

        # Check size
        file_size = file_path.stat().st_size
        if file_size < self.min_size:
            logger.warning("validation_too_small", size=file_size, min=self.min_size)
            return False

        if file_size > self.max_size:
            logger.warning("validation_too_large", size=file_size, max=self.max_size)
            return False

        # Check PDF header
        if self.check_pdf_header:
            try:
                with open(file_path, "rb") as f:
                    header = f.read(5)
                    if not header.startswith(b"%PDF-"):
                        logger.warning(
                            "validation_missing_pdf_header", path=str(file_path)
                        )
                        return False
            except Exception as e:
                logger.warning("validation_read_error", error=str(e))
                return False

        return True

    def _should_skip(self, pdf_link: PDFLink, target_path: Path) -> bool:
        """
        Check if file should be skipped (already downloaded).

        Args:
            pdf_link: PDFLink being checked
            target_path: Expected file path

        Returns:
            True if should skip, False otherwise
        """
        if not target_path.exists():
            return False

        compare_by = self.config.get("compare_by", "name_only")

        if compare_by == "name_only":
            return True

        if compare_by == "size_and_name":
            # Would need to fetch size from URL (not implemented)
            return True

        return False

    def _build_output_path(self, pdf_link: PDFLink) -> Path:
        """
        Build organized output path.

        Format: documents/downloads/{source_id}/{YYYY-MM}/filename.pdf

        Args:
            pdf_link: PDFLink with URL and metadata

        Returns:
            Path object for output location
        """
        # Extract filename from URL
        filename = self._extract_filename(pdf_link.url)

        # Get date from discovery timestamp
        date_str = pdf_link.discovered_at.strftime("%Y-%m")

        # Build path
        path = (
            Path("documents") / "downloads" / pdf_link.source_id / date_str / filename
        )

        return path

    def _extract_filename(self, url: str) -> str:
        """
        Extract filename from URL.

        Args:
            url: PDF URL

        Returns:
            Filename (e.g., "RFQ123.pdf")
        """
        # Parse URL
        parsed = urlparse(url)
        path = parsed.path

        # Get last component
        filename = path.split("/")[-1]

        # Decode URL encoding
        filename = unquote(filename)

        # Remove query parameters if accidentally included
        filename = filename.split("?")[0]

        # Ensure .pdf extension
        if not filename.lower().endswith(".pdf"):
            filename += ".pdf"

        return filename

    def update_tracking(
        self, tracking_file: Path, download_results: List[DownloadResult]
    ) -> None:
        """
        Update tracking file with download results.

        Args:
            tracking_file: Path to tracking JSON file
            download_results: List of DownloadResult objects

        Updates the "downloaded" and "file_path" fields for each URL.
        Uses atomic write (write to temp, then rename).
        """
        logger.info(
            "updating_tracking_file",
            tracking_file=str(tracking_file),
            results_count=len(download_results),
        )

        # Load existing tracking
        tracking = self._load_tracking_file(tracking_file)

        # Update download status
        updates = 0
        for result in download_results:
            url = result.pdf_link.url
            if url in tracking.get("discovered_pdfs", {}):
                tracking["discovered_pdfs"][url]["downloaded"] = result.success
                if result.success and result.file_path:
                    tracking["discovered_pdfs"][url]["file_path"] = str(result.file_path)
                    tracking["discovered_pdfs"][url]["downloaded_at"] = datetime.now().isoformat()
                updates += 1

        # Update metadata
        tracking["last_updated"] = datetime.now().isoformat()

        # Save atomically
        self._save_tracking_file(tracking_file, tracking)

        logger.info("tracking_file_updated", updates=updates)

    def _load_tracking_file(self, tracking_file: Path) -> Dict[str, Any]:
        """
        Load tracking file from disk.

        Args:
            tracking_file: Path to tracking JSON file

        Returns:
            Tracking dictionary
        """
        if not tracking_file.exists():
            logger.warning(
                "tracking_file_not_found", path=str(tracking_file), creating_new=True
            )
            return {
                "version": "1.0",
                "last_updated": datetime.now().isoformat(),
                "discovered_pdfs": {},
            }

        try:
            with open(tracking_file, "r") as f:
                tracking = json.load(f)
                return tracking
        except json.JSONDecodeError as e:
            logger.error(
                "tracking_file_corrupt",
                path=str(tracking_file),
                error=str(e),
                creating_new=True,
            )
            return {
                "version": "1.0",
                "last_updated": datetime.now().isoformat(),
                "discovered_pdfs": {},
            }

    def _save_tracking_file(self, tracking_file: Path, tracking: Dict[str, Any]) -> None:
        """
        Save tracking file to disk atomically.

        Args:
            tracking_file: Path to tracking JSON file
            tracking: Tracking dictionary to save

        Uses atomic write pattern (write to temp, then rename).
        """
        # Ensure directory exists
        tracking_file.parent.mkdir(parents=True, exist_ok=True)

        # Atomic write
        temp_file = tracking_file.with_suffix(".tmp")
        try:
            with open(temp_file, "w") as f:
                json.dump(tracking, f, indent=2)

            # Atomic rename
            temp_file.rename(tracking_file)

            logger.debug("tracking_file_saved", path=str(tracking_file))
        except Exception as e:
            logger.error("tracking_file_save_failed", error=str(e))
            if temp_file.exists():
                temp_file.unlink()
            raise
