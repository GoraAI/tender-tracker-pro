"""
Navigator module for browser automation and web scraping.

Main components:
- BrowserEngine: Playwright-based browser navigation
- PDFDiscovery: Pattern-based PDF link finder
- PDFDownloader: PDF download with validation
- PDFLink: PDF link data model
- DownloadResult: Download result data model
- Exceptions: Navigation-related errors
"""

from .browser_engine import BrowserEngine
from .exceptions import (
    BrowserInitError,
    NavigationError,
    NavigationTimeout,
    ScreenshotError,
)
from .models import PDFLink, DownloadResult
from .pdf_discovery import PDFDiscovery
from .pdf_downloader import PDFDownloader

__all__ = [
    "BrowserEngine",
    "PDFDiscovery",
    "PDFDownloader",
    "PDFLink",
    "DownloadResult",
    "NavigationError",
    "BrowserInitError",
    "NavigationTimeout",
    "ScreenshotError",
]
