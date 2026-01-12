"""
Browser Navigation Engine using Playwright.

Provides human-like website navigation with automatic cookie handling,
pagination support, and error recovery.
"""

import asyncio
import random
from datetime import datetime
from pathlib import Path
from typing import Optional

import structlog
from playwright.async_api import Browser, BrowserContext, Page, Playwright, async_playwright

from .exceptions import BrowserInitError, NavigationError, NavigationTimeout, ScreenshotError

logger = structlog.get_logger(__name__)


class BrowserEngine:
    """
    Playwright-based browser navigation engine.

    Provides human-like website navigation with automatic cookie handling,
    pagination support, and error recovery.
    """

    def __init__(self, config: dict):
        """
        Initialize browser engine with configuration.

        Args:
            config: Browser configuration from app_config.yaml
                Required keys:
                - browser.type: str ("chromium", "firefox", "webkit")
                - browser.headless: bool
                - browser.viewport: dict {width, height}
                - browser.user_agent: str (optional)
                - timeouts.page_load: int (seconds)
                - timeouts.navigation: int (seconds)
                - delays.min_ms: int
                - delays.max_ms: int
        """
        self.config = config
        self.playwright: Optional[Playwright] = None
        self.browser: Optional[Browser] = None
        self.context: Optional[BrowserContext] = None

        # Extract config values
        browser_config = config.get("browser", {})
        self.browser_type = browser_config.get("type", "chromium")
        self.headless = browser_config.get("headless", True)
        self.viewport = browser_config.get("viewport", {"width": 1920, "height": 1080})
        self.user_agent = browser_config.get("user_agent")

        timeouts = config.get("timeouts", {})
        self.page_load_timeout = timeouts.get("page_load", 30) * 1000  # Convert to ms
        self.navigation_timeout = timeouts.get("navigation", 60) * 1000

        delays = config.get("delays", {})
        self.min_delay_ms = delays.get("min_ms", 1000)
        self.max_delay_ms = delays.get("max_ms", 3000)

        self.max_retries = config.get("max_retries", 3)
        self.screenshot_on_error = config.get("screenshot_on_error", True)

        logger.info(
            "browser_engine_initialized",
            browser_type=self.browser_type,
            headless=self.headless,
            viewport=self.viewport,
        )

    async def initialize(self) -> None:
        """
        Launch Playwright browser instance.

        Raises:
            BrowserInitError: If browser fails to launch
        """
        try:
            logger.info("initializing_playwright")
            self.playwright = await async_playwright().start()

            # Get browser launcher
            if self.browser_type == "chromium":
                browser_launcher = self.playwright.chromium
            elif self.browser_type == "firefox":
                browser_launcher = self.playwright.firefox
            elif self.browser_type == "webkit":
                browser_launcher = self.playwright.webkit
            else:
                raise BrowserInitError(f"Unknown browser type: {self.browser_type}")

            # Launch browser
            logger.info("launching_browser", browser_type=self.browser_type)
            self.browser = await browser_launcher.launch(headless=self.headless)

            # Create context with viewport and user agent
            context_options = {"viewport": self.viewport}
            if self.user_agent:
                context_options["user_agent"] = self.user_agent

            self.context = await self.browser.new_context(**context_options)

            logger.info(
                "browser_initialized",
                browser_type=self.browser_type,
                headless=self.headless,
            )

        except Exception as e:
            logger.error("browser_initialization_failed", error=str(e))
            raise BrowserInitError(f"Failed to initialize browser: {e}") from e

    async def navigate(
        self,
        url: str,
        wait_for: str = "networkidle",
        timeout_seconds: Optional[int] = None,
    ) -> Page:
        """
        Navigate to URL and return page object.

        Args:
            url: Target URL (must be valid http/https)
            wait_for: Load state to wait for
                - "load": Page load event
                - "domcontentloaded": DOM ready
                - "networkidle": No network activity for 500ms
            timeout_seconds: Maximum wait time (default from config)

        Returns:
            Playwright Page object ready for interaction

        Raises:
            NavigationError: If navigation fails after retries
            NavigationTimeout: If page load exceeds timeout
        """
        if not self.context:
            raise NavigationError("Browser not initialized. Call initialize() first.")

        timeout_ms = (
            timeout_seconds * 1000 if timeout_seconds else self.navigation_timeout
        )

        for attempt in range(1, self.max_retries + 1):
            try:
                logger.info(
                    "navigation_started",
                    url=url,
                    wait_for=wait_for,
                    attempt=attempt,
                    max_retries=self.max_retries,
                )

                # Create new page
                page = await self.context.new_page()

                # Set timeout
                page.set_default_timeout(timeout_ms)

                # Navigate
                start_time = datetime.now()
                response = await page.goto(url, wait_until=wait_for)
                load_time_ms = int((datetime.now() - start_time).total_seconds() * 1000)

                # Check response status
                if response and not response.ok:
                    logger.warning(
                        "navigation_non_ok_status",
                        url=url,
                        status=response.status,
                        attempt=attempt,
                    )
                    if attempt == self.max_retries:
                        raise NavigationError(
                            f"Navigation returned status {response.status}: {url}"
                        )
                    continue

                logger.info(
                    "navigation_succeeded",
                    url=url,
                    final_url=page.url,
                    load_time_ms=load_time_ms,
                    attempt=attempt,
                )

                # Add human-like delay
                await self._random_delay()

                return page

            except asyncio.TimeoutError as e:
                logger.error(
                    "navigation_timeout",
                    url=url,
                    timeout_ms=timeout_ms,
                    attempt=attempt,
                    error=str(e),
                )

                if attempt == self.max_retries:
                    # Take screenshot on final failure
                    if self.screenshot_on_error and "page" in locals():
                        await self._capture_error_screenshot(page, url)
                    raise NavigationTimeout(
                        f"Navigation timeout after {attempt} attempts: {url}"
                    ) from e

                # Exponential backoff
                backoff_ms = 2**attempt * 1000
                logger.info("retrying_navigation", backoff_ms=backoff_ms)
                await asyncio.sleep(backoff_ms / 1000)

            except Exception as e:
                logger.error(
                    "navigation_failed",
                    url=url,
                    attempt=attempt,
                    error=str(e),
                    error_type=type(e).__name__,
                )

                if attempt == self.max_retries:
                    if self.screenshot_on_error and "page" in locals():
                        await self._capture_error_screenshot(page, url)
                    raise NavigationError(
                        f"Navigation failed after {attempt} attempts: {url}"
                    ) from e

                # Exponential backoff
                backoff_ms = 2**attempt * 1000
                await asyncio.sleep(backoff_ms / 1000)

        raise NavigationError(f"Navigation failed after {self.max_retries} attempts: {url}")

    async def handle_cookie_consent(self, page: Page) -> bool:
        """
        Detect and accept cookie consent dialogs.

        Tries multiple selector patterns:
        1. button:has-text('Accept')
        2. button:has-text('Agree')
        3. .cookie-accept
        4. #cookie-accept

        Args:
            page: Playwright Page object

        Returns:
            True if consent dialog was handled, False if not found
        """
        selectors = [
            "button:has-text('Accept')",
            "button:has-text('Agree')",
            ".cookie-accept",
            "#cookie-accept",
            "[id*='cookie'][id*='accept']",
            "[class*='cookie'][class*='accept']",
        ]

        for selector in selectors:
            try:
                # Check if element exists (with short timeout)
                element = await page.query_selector(selector)
                if element and await element.is_visible():
                    logger.info("cookie_consent_found", selector=selector)

                    # Click with human-like delay
                    await self._random_delay(min_ms=500, max_ms=1500)
                    await element.click()

                    # Wait a bit after click
                    await asyncio.sleep(1)

                    logger.info("cookie_consent_handled", selector=selector)
                    return True

            except Exception as e:
                logger.debug(
                    "cookie_selector_failed",
                    selector=selector,
                    error=str(e),
                )
                continue

        logger.debug("no_cookie_consent_found")
        return False

    async def paginate(
        self,
        page: Page,
        next_button_selectors: list[str],
        max_pages: int = 5,
    ) -> list[Page]:
        """
        Handle pagination by clicking Next buttons.

        Args:
            page: Starting page with pagination
            next_button_selectors: List of selectors to try for Next button
            max_pages: Maximum pages to load (prevents infinite loops)

        Returns:
            List of Page objects (one per page loaded, including initial)
        """
        pages = [page]
        current_page = page

        logger.info("pagination_started", max_pages=max_pages)

        for page_num in range(2, max_pages + 1):
            # Try to find Next button with any of the selectors
            next_button = None
            used_selector = None

            for selector in next_button_selectors:
                try:
                    element = await current_page.query_selector(selector)
                    if element and await element.is_visible():
                        next_button = element
                        used_selector = selector
                        break
                except Exception:
                    continue

            if not next_button:
                logger.info(
                    "pagination_complete",
                    reason="no_next_button",
                    total_pages=len(pages),
                )
                break

            try:
                logger.info(
                    "pagination_clicking_next",
                    page_number=page_num,
                    selector=used_selector,
                )

                # Human-like delay before click
                await self._random_delay()

                # Click Next button
                await next_button.click()

                # Wait for navigation
                await current_page.wait_for_load_state("networkidle", timeout=30000)

                # Add to pages list
                pages.append(current_page)

                logger.info(
                    "pagination_page_loaded",
                    page_number=page_num,
                    total_pages=len(pages),
                )

                # Human-like delay after page load
                await self._random_delay()

            except Exception as e:
                logger.warning(
                    "pagination_failed",
                    page_number=page_num,
                    error=str(e),
                )
                break

        logger.info("pagination_finished", total_pages=len(pages))
        return pages

    async def screenshot(
        self,
        page: Page,
        name: str,
        path: str = "logs/screenshots",
    ) -> str:
        """
        Capture screenshot of current page.

        Args:
            page: Page to screenshot
            name: Base filename (without extension)
            path: Directory to save screenshot

        Returns:
            Full path to saved screenshot

        Raises:
            ScreenshotError: If capture fails
        """
        try:
            # Ensure directory exists
            screenshot_dir = Path(path)
            screenshot_dir.mkdir(parents=True, exist_ok=True)

            # Generate filename with timestamp
            timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
            filename = f"{name}_{timestamp}.png"
            filepath = screenshot_dir / filename

            # Capture screenshot
            logger.info("capturing_screenshot", filename=filename, path=path)
            await page.screenshot(path=str(filepath), full_page=True)

            logger.info("screenshot_captured", filepath=str(filepath))
            return str(filepath)

        except Exception as e:
            logger.error("screenshot_failed", name=name, error=str(e))
            raise ScreenshotError(f"Failed to capture screenshot: {e}") from e

    async def close(self) -> None:
        """Close browser and cleanup resources."""
        logger.info("closing_browser")

        try:
            if self.context:
                await self.context.close()
                self.context = None

            if self.browser:
                await self.browser.close()
                self.browser = None

            if self.playwright:
                await self.playwright.stop()
                self.playwright = None

            logger.info("browser_closed")

        except Exception as e:
            logger.error("browser_close_error", error=str(e))
            raise

    async def _random_delay(
        self, min_ms: Optional[int] = None, max_ms: Optional[int] = None
    ) -> None:
        """
        Add random human-like delay.

        Args:
            min_ms: Minimum delay in milliseconds (default from config)
            max_ms: Maximum delay in milliseconds (default from config)
        """
        min_delay = min_ms if min_ms is not None else self.min_delay_ms
        max_delay = max_ms if max_ms is not None else self.max_delay_ms

        delay_ms = random.uniform(min_delay, max_delay)
        await asyncio.sleep(delay_ms / 1000)

        logger.debug("human_like_delay", delay_ms=int(delay_ms))

    async def _capture_error_screenshot(self, page: Page, url: str) -> None:
        """
        Capture screenshot on navigation error.

        Args:
            page: Page object
            url: URL that failed
        """
        try:
            # Create safe filename from URL
            safe_name = url.replace("https://", "").replace("http://", "")
            safe_name = safe_name.replace("/", "_").replace(":", "_")[:50]
            safe_name = f"error_{safe_name}"

            screenshot_path = await self.screenshot(page, safe_name)
            logger.error("error_screenshot_saved", screenshot_path=screenshot_path)

        except Exception as e:
            logger.error("error_screenshot_failed", error=str(e))
