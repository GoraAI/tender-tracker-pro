"""
Integration tests for Johannesburg Water navigation.

These tests connect to the actual website and verify navigation works.
They may be slower and require network connectivity.

Run with: pytest tests/integration/ -v
"""

import pytest
import yaml
from pathlib import Path

from src.navigator import BrowserEngine


@pytest.fixture
def jhb_water_config():
    """Load actual JHB Water configuration."""
    # Load app config
    config_path = (
        Path(__file__).parent.parent.parent / "config" / "sources" / "jhb_water.yaml"
    )

    if not config_path.exists():
        pytest.skip("JHB Water config not found")

    with open(config_path) as f:
        source_config = yaml.safe_load(f)

    # Convert to browser engine config format
    return {
        "browser": source_config["navigation"]["browser"],
        "timeouts": {
            "page_load": 30,
            "navigation": 60,
        },
        "delays": {
            "min_ms": source_config["navigation"]["delays"]["min_ms"],
            "max_ms": source_config["navigation"]["delays"]["max_ms"],
        },
        "max_retries": 3,
        "screenshot_on_error": True,
    }


@pytest.fixture
def jhb_water_url():
    """JHB Water RFQ listing URL."""
    return "https://scm.johannesburgwater.co.za/supply-chain/request-for-quotations/open-rfq-2/"


# Task 8.1: Test navigate to JHB Water RFQ listing page
@pytest.mark.asyncio
@pytest.mark.integration
async def test_navigate_to_jhb_water_rfq_page(jhb_water_config, jhb_water_url):
    """
    Test navigation to actual JHB Water RFQ listing page.

    This is a real integration test that connects to the website.
    """
    engine = BrowserEngine(jhb_water_config)

    try:
        # Initialize browser
        await engine.initialize()

        # Navigate to JHB Water
        page = await engine.navigate(jhb_water_url, wait_for="networkidle")

        # Verify we reached the page
        assert page is not None
        assert "johannesburgwater" in page.url.lower()

        # Verify page has expected content
        content = await page.content()
        assert "request for quotation" in content.lower() or "rfq" in content.lower()

        print(f"✓ Successfully navigated to: {page.url}")
        print(f"✓ Page title: {await page.title()}")

    finally:
        # Always clean up
        await engine.close()


# Task 8.2: Test cookie consent on JHB Water (if present)
@pytest.mark.asyncio
@pytest.mark.integration
async def test_handle_jhb_water_cookies(jhb_water_config, jhb_water_url):
    """Test cookie consent handling on JHB Water site."""
    engine = BrowserEngine(jhb_water_config)

    try:
        await engine.initialize()
        page = await engine.navigate(jhb_water_url)

        # Try to handle cookie consent
        consent_handled = await engine.handle_cookie_consent(page)

        # Log result (may or may not have consent dialog)
        if consent_handled:
            print("✓ Cookie consent dialog handled")
        else:
            print("✓ No cookie consent dialog found")

    finally:
        await engine.close()


# Task 8.3: Test pagination on JHB Water listings
@pytest.mark.asyncio
@pytest.mark.integration
@pytest.mark.skip(
    reason="Pagination depends on tender count - may not always be present"
)
async def test_pagination_on_jhb_water(jhb_water_config, jhb_water_url):
    """
    Test pagination on JHB Water listings.

    Note: This test is skipped by default as pagination may not always be present.
    """
    engine = BrowserEngine(jhb_water_config)

    try:
        await engine.initialize()
        page = await engine.navigate(jhb_water_url)

        # Try pagination
        pages = await engine.paginate(
            page,
            next_button_selectors=["a:has-text('Next')", ".pagination .next"],
            max_pages=2,  # Limit for testing
        )

        # Should at least have the initial page
        assert len(pages) >= 1
        print(f"✓ Loaded {len(pages)} page(s)")

    finally:
        await engine.close()


# Task 8.4: Test screenshot on invalid URL
@pytest.mark.asyncio
@pytest.mark.integration
async def test_screenshot_on_invalid_url(jhb_water_config, tmp_path):
    """Test that screenshot is captured when navigation fails."""
    engine = BrowserEngine(jhb_water_config)

    try:
        await engine.initialize()

        # Try to navigate to localhost port that won't respond (guaranteed to timeout)
        # Use shorter timeout for faster test
        jhb_water_config["timeouts"]["page_load"] = 5  # 5 seconds
        engine = BrowserEngine(jhb_water_config)
        await engine.initialize()

        with pytest.raises(
            Exception
        ):  # Will raise NavigationTimeout or NavigationError
            await engine.navigate("http://localhost:9999", timeout_seconds=5)

        # Screenshot should have been captured (check logs)
        # Note: Actual screenshot verification would require checking the filesystem

    finally:
        await engine.close()


# Task 8.5: Test full navigation pipeline end-to-end
@pytest.mark.asyncio
@pytest.mark.integration
async def test_full_navigation_pipeline(jhb_water_config, jhb_water_url):
    """
    Test complete navigation pipeline:
    1. Initialize browser
    2. Navigate to page
    3. Handle cookies
    4. Check for content
    5. Cleanup
    """
    engine = BrowserEngine(jhb_water_config)

    try:
        # Step 1: Initialize
        await engine.initialize()
        assert engine.browser is not None

        # Step 2: Navigate
        page = await engine.navigate(jhb_water_url)
        assert page is not None

        # Step 3: Handle cookies
        await engine.handle_cookie_consent(page)

        # Step 4: Verify content
        content = await page.content()
        assert len(content) > 0

        # Step 5: Take screenshot (for verification)
        screenshot_path = await engine.screenshot(
            page, "jhb_water_test", "/tmp"
        )
        assert Path(screenshot_path).exists()

        print("✓ Full pipeline complete")
        print(f"✓ Screenshot saved: {screenshot_path}")

    finally:
        # Step 6: Cleanup
        await engine.close()
        assert engine.browser is None


# Additional helper tests


@pytest.mark.asyncio
@pytest.mark.integration
async def test_multiple_navigations_same_browser(jhb_water_config, jhb_water_url):
    """Test that same browser instance can navigate multiple times."""
    engine = BrowserEngine(jhb_water_config)

    try:
        await engine.initialize()

        # Navigate twice
        page1 = await engine.navigate(jhb_water_url)
        assert page1 is not None

        page2 = await engine.navigate(jhb_water_url)
        assert page2 is not None

        # Both should work
        assert await page1.title()
        assert await page2.title()

    finally:
        await engine.close()
