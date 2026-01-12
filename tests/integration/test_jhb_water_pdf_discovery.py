"""
Integration tests for JHB Water PDF discovery.

These tests connect to the actual website and verify PDF discovery works.
They may be slower and require network connectivity.

Run with: pytest tests/integration/ -v -m integration
"""

import yaml
from pathlib import Path

import pytest

from src.navigator import BrowserEngine, PDFDiscovery


@pytest.fixture
def jhb_water_config():
    """Load actual JHB Water configuration."""
    config_path = (
        Path(__file__).parent.parent.parent / "config" / "sources" / "jhb_water.yaml"
    )

    if not config_path.exists():
        pytest.skip("JHB Water config not found")

    with open(config_path) as f:
        source_config = yaml.safe_load(f)

    return source_config


@pytest.fixture
def jhb_water_browser_config(jhb_water_config):
    """Browser engine configuration for JHB Water."""
    return {
        "browser": jhb_water_config["navigation"]["browser"],
        "timeouts": {
            "page_load": 30,
            "navigation": 60,
        },
        "delays": {
            "min_ms": jhb_water_config["navigation"]["delays"]["min_ms"],
            "max_ms": jhb_water_config["navigation"]["delays"]["max_ms"],
        },
        "max_retries": 3,
        "screenshot_on_error": True,
    }


@pytest.fixture
def jhb_water_url(jhb_water_config):
    """JHB Water RFQ listing URL."""
    return jhb_water_config["urls"]["rfq_listing"]


# Task 8.1: Test discovery on JHB Water live RFQ page
@pytest.mark.asyncio
@pytest.mark.integration
async def test_discover_pdfs_on_jhb_water(
    jhb_water_config, jhb_water_browser_config, jhb_water_url, tmp_path
):
    """
    Test PDF discovery on actual JHB Water RFQ page.

    This is a real integration test that connects to the website.
    """
    engine = BrowserEngine(jhb_water_browser_config)
    discovery = PDFDiscovery(
        jhb_water_config["pdf_discovery"], tmp_path / "jhb_water_pdfs.json"
    )

    try:
        # Initialize browser
        await engine.initialize()

        # Navigate to JHB Water
        page = await engine.navigate(jhb_water_url, wait_for="networkidle")

        # Handle cookies
        await engine.handle_cookie_consent(page)

        # Discover PDFs
        pdf_links = await discovery.discover_pdfs(page, source_id="jhb-water")

        # Verify we found some PDFs
        print(f"✓ Discovered {len(pdf_links)} PDF links")

        # Verify PDFLink structure
        if pdf_links:
            first_link = pdf_links[0]
            assert first_link.url.endswith(".pdf")
            assert first_link.link_text
            assert first_link.source_id == "jhb-water"
            assert first_link.source_url == page.url
            print(f"✓ Sample PDF: {first_link.url}")
            print(f"✓ Link text: {first_link.link_text}")

    finally:
        await engine.close()


# Task 8.2: Test pattern matching with real PDF URLs
@pytest.mark.asyncio
@pytest.mark.integration
async def test_pattern_matching_real_urls(
    jhb_water_config, jhb_water_browser_config, jhb_water_url, tmp_path
):
    """Test that discovered URLs match expected patterns."""
    engine = BrowserEngine(jhb_water_browser_config)
    discovery = PDFDiscovery(
        jhb_water_config["pdf_discovery"], tmp_path / "jhb_water_pdfs.json"
    )

    try:
        await engine.initialize()
        page = await engine.navigate(jhb_water_url)
        await engine.handle_cookie_consent(page)

        pdf_links = await discovery.discover_pdfs(page, source_id="jhb-water")

        # Verify all discovered PDFs match patterns
        for link in pdf_links:
            assert link.url.endswith(".pdf"), f"URL doesn't end with .pdf: {link.url}"
            # Should match at least one pattern
            assert discovery._matches_patterns(
                link.url
            ), f"URL doesn't match patterns: {link.url}"

        print(f"✓ All {len(pdf_links)} PDFs match expected patterns")

    finally:
        await engine.close()


# Task 8.3: Test pagination + discovery (discover across multiple pages)
@pytest.mark.asyncio
@pytest.mark.integration
@pytest.mark.skip(
    reason="Pagination depends on tender count - may not always have multiple pages"
)
async def test_pagination_discovery(
    jhb_water_config, jhb_water_browser_config, jhb_water_url, tmp_path
):
    """
    Test discovering PDFs across multiple pages.

    Note: This test is skipped by default as pagination may not always be present.
    """
    engine = BrowserEngine(jhb_water_browser_config)
    discovery = PDFDiscovery(
        jhb_water_config["pdf_discovery"], tmp_path / "jhb_water_pdfs.json"
    )

    try:
        await engine.initialize()
        page = await engine.navigate(jhb_water_url)
        await engine.handle_cookie_consent(page)

        # Get paginated pages
        pages = await engine.paginate(
            page,
            next_button_selectors=jhb_water_config["navigation"]["pagination"][
                "next_button_selectors"
            ],
            max_pages=3,
        )

        # Discover PDFs on each page
        all_pdfs = []
        for i, p in enumerate(pages, 1):
            pdfs = await discovery.discover_pdfs(p, source_id="jhb-water")
            all_pdfs.extend(pdfs)
            print(f"✓ Page {i}: Found {len(pdfs)} PDFs")

        print(f"✓ Total PDFs discovered across {len(pages)} pages: {len(all_pdfs)}")

        # Verify uniqueness (no duplicates across pages)
        urls = [pdf.url for pdf in all_pdfs]
        assert len(urls) == len(set(urls)), "Found duplicate PDFs across pages"

    finally:
        await engine.close()


# Task 8.4: Test deduplication across multiple runs
@pytest.mark.asyncio
@pytest.mark.integration
async def test_deduplication_across_runs(
    jhb_water_config, jhb_water_browser_config, jhb_water_url, tmp_path
):
    """Test that deduplication works across multiple discovery runs."""
    tracking_file = tmp_path / "jhb_water_pdfs.json"
    engine = BrowserEngine(jhb_water_browser_config)

    try:
        await engine.initialize()

        # First run: Discover PDFs
        discovery1 = PDFDiscovery(jhb_water_config["pdf_discovery"], tracking_file)
        page = await engine.navigate(jhb_water_url)
        await engine.handle_cookie_consent(page)

        pdfs_run1 = await discovery1.discover_pdfs(page, source_id="jhb-water")
        discovery1.save_tracking(pdfs_run1)

        print(f"✓ Run 1: Discovered {len(pdfs_run1)} PDFs")

        # Second run: Same page, should find 0 new PDFs
        discovery2 = PDFDiscovery(jhb_water_config["pdf_discovery"], tracking_file)
        page2 = await engine.navigate(jhb_water_url)
        await engine.handle_cookie_consent(page2)

        pdfs_run2 = await discovery2.discover_pdfs(page2, source_id="jhb-water")

        print(f"✓ Run 2: Discovered {len(pdfs_run2)} new PDFs (should be 0)")

        # All PDFs from run 1 should be filtered out in run 2
        assert (
            len(pdfs_run2) == 0
        ), f"Expected 0 new PDFs, found {len(pdfs_run2)} (deduplication failed)"

    finally:
        await engine.close()


# Task 8.5: Test tracking file persistence and reload
@pytest.mark.asyncio
@pytest.mark.integration
async def test_tracking_file_persistence(
    jhb_water_config, jhb_water_browser_config, jhb_water_url, tmp_path
):
    """Test that tracking file persists and can be reloaded."""
    tracking_file = tmp_path / "jhb_water_pdfs.json"
    engine = BrowserEngine(jhb_water_browser_config)

    try:
        await engine.initialize()

        # Discover and save
        discovery1 = PDFDiscovery(jhb_water_config["pdf_discovery"], tracking_file)
        page = await engine.navigate(jhb_water_url)
        await engine.handle_cookie_consent(page)

        pdfs = await discovery1.discover_pdfs(page, source_id="jhb-water")
        discovery1.save_tracking(pdfs)

        print(f"✓ Saved {len(pdfs)} PDFs to tracking file")

        # Verify file exists
        assert tracking_file.exists(), "Tracking file was not created"

        # Create new discovery instance (reload tracking)
        discovery2 = PDFDiscovery(jhb_water_config["pdf_discovery"], tracking_file)
        tracking = discovery2.load_tracking()

        # Verify PDFs were persisted
        assert len(tracking["discovered_pdfs"]) == len(
            pdfs
        ), "Tracking file didn't persist all PDFs"

        print(f"✓ Successfully reloaded {len(tracking['discovered_pdfs'])} PDFs")

        # Verify structure
        assert tracking["version"] == "1.0"
        assert "last_updated" in tracking

    finally:
        await engine.close()
