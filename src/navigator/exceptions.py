"""
Navigation exceptions for Browser Engine.

All navigation-related errors inherit from NavigationError.
"""


class NavigationError(Exception):
    """Base error for navigation failures."""

    pass


class BrowserInitError(NavigationError):
    """Browser failed to initialize."""

    pass


class NavigationTimeout(NavigationError):
    """Page load exceeded timeout."""

    pass


class ScreenshotError(NavigationError):
    """Screenshot capture failed."""

    pass
