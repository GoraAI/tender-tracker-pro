"""
Navigator module for browser automation and web scraping.

Main components:
- BrowserEngine: Playwright-based browser navigation
- PDFDiscovery: Pattern-based PDF link finder
- PDFLink: PDF link data model
- Exceptions: Navigation-related errors
"""

from .browser_engine import BrowserEngine
from .exceptions import (
    BrowserInitError,
    NavigationError,
    NavigationTimeout,
    ScreenshotError,
)
from .models import PDFLink
from .pdf_discovery import PDFDiscovery

__all__ = [
    "BrowserEngine",
    "PDFDiscovery",
    "PDFLink",
    "NavigationError",
    "BrowserInitError",
    "NavigationTimeout",
    "ScreenshotError",
]
