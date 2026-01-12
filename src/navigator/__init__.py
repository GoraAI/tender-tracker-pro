"""
Navigator module for browser automation and web scraping.

Main components:
- BrowserEngine: Playwright-based browser navigation
- Exceptions: Navigation-related errors
"""

from .browser_engine import BrowserEngine
from .exceptions import (
    BrowserInitError,
    NavigationError,
    NavigationTimeout,
    ScreenshotError,
)

__all__ = [
    "BrowserEngine",
    "NavigationError",
    "BrowserInitError",
    "NavigationTimeout",
    "ScreenshotError",
]
