"""
Unit tests for BrowserEngine.

Tests cover:
- Browser initialization
- Navigation with retries
- Cookie consent handling
- Pagination
- Screenshot capture
- Human-like delays
- Error handling
"""

import asyncio
from unittest.mock import AsyncMock, Mock, patch

import pytest
from playwright.async_api import Page

from src.navigator import (
    BrowserEngine,
    BrowserInitError,
    NavigationError,
    NavigationTimeout,
)


@pytest.fixture
def browser_config():
    """Standard browser configuration for tests."""
    return {
        "browser": {
            "type": "chromium",
            "headless": True,
            "viewport": {"width": 1920, "height": 1080},
        },
        "timeouts": {
            "page_load": 30,
            "navigation": 60,
        },
        "delays": {
            "min_ms": 100,  # Shorter for tests
            "max_ms": 200,
        },
        "max_retries": 3,
        "screenshot_on_error": True,
    }


# Task 7.1: Test browser initialization succeeds
@pytest.mark.asyncio
async def test_browser_initialization_succeeds(browser_config):
    """Test that browser initializes successfully."""
    engine = BrowserEngine(browser_config)

    with patch("src.navigator.browser_engine.async_playwright") as mock_playwright:
        # Mock the Playwright API
        mock_pw = AsyncMock()
        mock_browser = AsyncMock()
        mock_context = AsyncMock()

        mock_playwright.return_value.start = AsyncMock(return_value=mock_pw)
        mock_pw.chromium.launch = AsyncMock(return_value=mock_browser)
        mock_browser.new_context = AsyncMock(return_value=mock_context)

        # Initialize
        await engine.initialize()

        # Verify
        assert engine.playwright is not None
        assert engine.browser is not None
        assert engine.context is not None
        mock_pw.chromium.launch.assert_called_once_with(headless=True)


# Task 7.2: Test browser initialization fails gracefully
@pytest.mark.asyncio
async def test_browser_initialization_fails_gracefully(browser_config):
    """Test that browser initialization failure raises BrowserInitError."""
    engine = BrowserEngine(browser_config)

    with patch("src.navigator.browser_engine.async_playwright") as mock_playwright:
        mock_playwright.return_value.start = AsyncMock(
            side_effect=Exception("Playwright not installed")
        )

        # Should raise BrowserInitError
        with pytest.raises(BrowserInitError, match="Failed to initialize browser"):
            await engine.initialize()


# Task 7.3: Test navigate() to valid URL
@pytest.mark.asyncio
async def test_navigate_to_valid_url(browser_config):
    """Test navigation to a valid URL."""
    engine = BrowserEngine(browser_config)

    # Mock initialized browser
    mock_context = AsyncMock()
    mock_page = AsyncMock(spec=Page)
    mock_response = Mock()
    mock_response.ok = True
    mock_response.status = 200

    engine.context = mock_context
    mock_context.new_page = AsyncMock(return_value=mock_page)
    mock_page.goto = AsyncMock(return_value=mock_response)
    mock_page.url = "https://example.com"
    mock_page.set_default_timeout = Mock()

    # Navigate
    result_page = await engine.navigate("https://example.com")

    # Verify
    assert result_page == mock_page
    mock_page.goto.assert_called_once()
    mock_page.set_default_timeout.assert_called_once()


# Task 7.4: Test navigate() timeout handling
@pytest.mark.asyncio
async def test_navigate_timeout_handling(browser_config):
    """Test that navigation timeout is handled correctly."""
    engine = BrowserEngine(browser_config)

    # Mock initialized browser
    mock_context = AsyncMock()
    mock_page = AsyncMock(spec=Page)

    engine.context = mock_context
    mock_context.new_page = AsyncMock(return_value=mock_page)
    mock_page.goto = AsyncMock(side_effect=asyncio.TimeoutError("Timeout"))
    mock_page.set_default_timeout = Mock()
    mock_page.screenshot = AsyncMock()  # Mock screenshot

    # Should raise NavigationTimeout after retries
    with pytest.raises(NavigationTimeout, match="Navigation timeout"):
        await engine.navigate("https://example.com")

    # Should have retried 3 times
    assert mock_page.goto.call_count == 3


# Task 7.5: Test retry logic on network error
@pytest.mark.asyncio
async def test_retry_logic_on_network_error(browser_config):
    """Test that navigation retries on network errors."""
    engine = BrowserEngine(browser_config)

    # Mock initialized browser
    mock_context = AsyncMock()
    mock_page = AsyncMock(spec=Page)
    mock_response = Mock()
    mock_response.ok = True

    engine.context = mock_context
    mock_context.new_page = AsyncMock(return_value=mock_page)

    # Fail first 2 attempts, succeed on 3rd
    mock_page.goto = AsyncMock(
        side_effect=[
            Exception("Network error"),
            Exception("Network error"),
            mock_response,
        ]
    )
    mock_page.url = "https://example.com"
    mock_page.set_default_timeout = Mock()

    # Should succeed after retries
    result_page = await engine.navigate("https://example.com")

    # Verify
    assert result_page == mock_page
    assert mock_page.goto.call_count == 3


# Task 7.6: Test cookie consent detection and click
@pytest.mark.asyncio
async def test_cookie_consent_detection_and_click(browser_config):
    """Test that cookie consent dialogs are detected and clicked."""
    engine = BrowserEngine(browser_config)

    # Mock page
    mock_page = AsyncMock(spec=Page)
    mock_element = AsyncMock()
    mock_element.is_visible = AsyncMock(return_value=True)
    mock_element.click = AsyncMock()

    mock_page.query_selector = AsyncMock(return_value=mock_element)

    # Handle cookie consent
    result = await engine.handle_cookie_consent(mock_page)

    # Verify
    assert result is True
    mock_element.click.assert_called_once()


# Task 7.7: Test pagination with Next button
@pytest.mark.asyncio
async def test_pagination_with_next_button(browser_config):
    """Test pagination by clicking Next button."""
    engine = BrowserEngine(browser_config)

    # Mock page
    mock_page = AsyncMock(spec=Page)
    mock_next_button = AsyncMock()
    mock_next_button.is_visible = AsyncMock(return_value=True)
    mock_next_button.click = AsyncMock()

    # First 2 calls find Next button, 3rd call doesn't (end of pagination)
    mock_page.query_selector = AsyncMock(
        side_effect=[mock_next_button, mock_next_button, None]
    )
    mock_page.wait_for_load_state = AsyncMock()

    # Paginate
    pages = await engine.paginate(
        mock_page,
        next_button_selectors=["a:has-text('Next')"],
        max_pages=5,
    )

    # Verify
    assert len(pages) == 3  # Initial + 2 next pages
    assert mock_next_button.click.call_count == 2


# Task 7.8: Test pagination stops at max_pages
@pytest.mark.asyncio
async def test_pagination_stops_at_max_pages(browser_config):
    """Test that pagination stops at max_pages limit."""
    engine = BrowserEngine(browser_config)

    # Mock page with Next button always available
    mock_page = AsyncMock(spec=Page)
    mock_next_button = AsyncMock()
    mock_next_button.is_visible = AsyncMock(return_value=True)
    mock_next_button.click = AsyncMock()

    mock_page.query_selector = AsyncMock(return_value=mock_next_button)
    mock_page.wait_for_load_state = AsyncMock()

    # Paginate with max_pages=3
    pages = await engine.paginate(
        mock_page,
        next_button_selectors=["a:has-text('Next')"],
        max_pages=3,
    )

    # Verify - should stop at max_pages
    assert len(pages) == 3
    assert mock_next_button.click.call_count == 2  # max_pages - 1


# Task 7.9: Test screenshot capture on error
@pytest.mark.asyncio
async def test_screenshot_capture_on_error(browser_config, tmp_path):
    """Test that screenshots are captured on navigation errors."""
    engine = BrowserEngine(browser_config)

    # Mock page
    mock_page = AsyncMock(spec=Page)
    mock_page.screenshot = AsyncMock()

    # Capture screenshot
    screenshot_path = await engine.screenshot(
        mock_page,
        name="test_error",
        path=str(tmp_path),
    )

    # Verify
    assert "test_error" in screenshot_path
    assert screenshot_path.endswith(".png")
    mock_page.screenshot.assert_called_once()


# Task 7.10: Test human-like delay range
@pytest.mark.asyncio
async def test_human_like_delay_range(browser_config):
    """Test that delays are within configured range."""
    engine = BrowserEngine(browser_config)

    import time

    # Test delay is within range
    start = time.time()
    await engine._random_delay()
    elapsed_ms = (time.time() - start) * 1000

    # Should be between min and max (with some tolerance)
    assert elapsed_ms >= (browser_config["delays"]["min_ms"] - 10)
    assert elapsed_ms <= (browser_config["delays"]["max_ms"] + 50)


# Additional tests for coverage


@pytest.mark.asyncio
async def test_close_browser(browser_config):
    """Test browser cleanup."""
    engine = BrowserEngine(browser_config)

    # Mock browser components
    engine.context = AsyncMock()
    engine.browser = AsyncMock()
    engine.playwright = AsyncMock()

    # Close
    await engine.close()

    # Verify all cleaned up
    assert engine.context is None
    assert engine.browser is None
    assert engine.playwright is None


@pytest.mark.asyncio
async def test_navigate_without_initialization(browser_config):
    """Test that navigation fails if browser not initialized."""
    engine = BrowserEngine(browser_config)

    # Should raise NavigationError
    with pytest.raises(NavigationError, match="Browser not initialized"):
        await engine.navigate("https://example.com")


@pytest.mark.asyncio
async def test_unknown_browser_type(browser_config):
    """Test that unknown browser type raises error."""
    browser_config["browser"]["type"] = "unknown"
    engine = BrowserEngine(browser_config)

    with patch("src.navigator.browser_engine.async_playwright") as mock_playwright:
        mock_pw = AsyncMock()
        mock_playwright.return_value.start = AsyncMock(return_value=mock_pw)

        with pytest.raises(BrowserInitError, match="Unknown browser type"):
            await engine.initialize()
