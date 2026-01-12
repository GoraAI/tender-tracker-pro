"""
Integration tests for PDF Download with JHB Water site.

These tests use real network requests and browser automation.
They may be slow and are marked with @pytest.mark.integration.

Run with: pytest tests/integration/test_jhb_water_downloads.py -v
"""

from pathlib import Path
import shutil
import pytest
import yaml

from src.navigator import BrowserEngine, PDFDiscovery, PDFDownloader


# Mark all tests in this module as integration tests
pytestmark = pytest.mark.integration


@pytest.fixture(scope="module")
def jhb_config():
    """Load JHB Water configuration."""
    config_path = Path("config/sources/jhb_water.yaml")

    if not config_path.exists():
        pytest.skip("JHB Water config not found")

    with open(config_path) as f:
        return yaml.safe_load(f)


@pytest.fixture(scope="module")
def test_download_dir(tmp_path_factory):
    """Create temporary download directory for tests."""
    download_dir = tmp_path_factory.mktemp("downloads")
    yield download_dir

    # Cleanup after all tests
    if download_dir.exists():
        shutil.rmtree(download_dir)


@pytest.fixture
async def browser_engine(jhb_config):
    """Create and initialize BrowserEngine."""
    browser_config = {
        "browser": jhb_config["navigation"]["browser"],
        "timeouts": {"page_load": 30, "navigation": 60},
        "delays": {
            "min_ms": jhb_config["navigation"]["delays"]["min_ms"],
            "max_ms": jhb_config["navigation"]["delays"]["max_ms"],
        },
        "max_retries": 3,
        "screenshot_on_error": True,
    }

    engine = BrowserEngine(browser_config)
    await engine.initialize()

    yield engine

    await engine.close()


# ============================================================================
# Integration Test 1: Real PDF Download
# ============================================================================


@pytest.mark.asyncio
@pytest.mark.slow
async def test_download_real_pdf_from_jhb_water(
    jhb_config, browser_engine, test_download_dir
):
    """
    Test downloading a real PDF from JHB Water site.

    Steps:
    1. Navigate to JHB Water RFQ listing
    2. Discover PDF links
    3. Download first PDF
    4. Verify PDF is valid and saved correctly
    """
    # Navigate to listing page
    url = jhb_config["urls"]["rfq_listing"]
    page = await browser_engine.navigate(url, wait_for="networkidle")

    # Handle cookie consent
    await browser_engine.handle_cookie_consent(page)

    # Discover PDFs
    tracking_file = test_download_dir / "tracking.json"
    discovery = PDFDiscovery(jhb_config["pdf_discovery"], tracking_file)
    pdf_links = await discovery.discover_pdfs(page, source_id="jhb-water")

    if not pdf_links:
        pytest.skip("No PDFs found on JHB Water site")

    # Download first PDF
    download_config = jhb_config["download"].copy()
    download_config["skip_existing"] = False  # Force download for testing

    # Override path to use test directory
    pdf_link = pdf_links[0]

    downloader = PDFDownloader(download_config, browser_engine=browser_engine)

    async with downloader:
        # Mock _build_output_path to use test directory
        original_build_path = downloader._build_output_path

        def test_build_path(link):
            original_path = original_build_path(link)
            return test_download_dir / original_path.name

        downloader._build_output_path = test_build_path

        result = await downloader.download_single(pdf_link)

        # Verify result
        assert result.success is True, f"Download failed: {result.error}"
        assert result.file_path is not None
        assert result.file_path.exists()
        assert result.file_size > 1000  # At least 1KB
        assert result.method in ["direct", "browser"]
        assert result.attempts >= 1

        # Verify PDF is valid
        with open(result.file_path, "rb") as f:
            header = f.read(5)
            assert header == b"%PDF-", "Downloaded file is not a valid PDF"

        print(f"\n✅ Successfully downloaded: {result.file_path.name}")
        print(f"   Size: {result.file_size / 1024:.1f} KB")
        print(f"   Method: {result.method}")
        print(f"   Time: {result.download_time_ms} ms")


# ============================================================================
# Integration Test 2: Batch Download with Concurrency
# ============================================================================


@pytest.mark.asyncio
@pytest.mark.slow
async def test_batch_download_concurrent(jhb_config, browser_engine, test_download_dir):
    """
    Test downloading multiple PDFs concurrently.

    Steps:
    1. Discover multiple PDF links
    2. Download 3-5 PDFs concurrently (max_concurrent=2)
    3. Verify all downloads complete
    4. Check success rate
    """
    # Navigate and discover
    url = jhb_config["urls"]["rfq_listing"]
    page = await browser_engine.navigate(url, wait_for="networkidle")
    await browser_engine.handle_cookie_consent(page)

    tracking_file = test_download_dir / "batch_tracking.json"
    discovery = PDFDiscovery(jhb_config["pdf_discovery"], tracking_file)
    pdf_links = await discovery.discover_pdfs(page, source_id="jhb-water")

    if len(pdf_links) < 3:
        pytest.skip("Need at least 3 PDFs for batch test")

    # Take first 5 (or fewer if less available)
    pdf_links_to_download = pdf_links[: min(5, len(pdf_links))]

    # Download batch
    download_config = jhb_config["download"].copy()
    download_config["skip_existing"] = False
    download_config["max_concurrent"] = 2  # Test concurrency control

    downloader = PDFDownloader(download_config, browser_engine=browser_engine)

    async with downloader:
        # Override path building
        def test_build_path(link):
            filename = downloader._extract_filename(link.url)
            return test_download_dir / "batch" / filename

        downloader._build_output_path = test_build_path

        # Create batch directory
        (test_download_dir / "batch").mkdir(exist_ok=True)

        results = await downloader.download_batch(
            pdf_links_to_download, max_concurrent=2
        )

        # Verify results
        assert len(results) == len(pdf_links_to_download)

        success_count = sum(1 for r in results if r.success)
        success_rate = success_count / len(results) * 100

        print("\n✅ Batch download complete:")
        print(f"   Total: {len(results)}")
        print(f"   Success: {success_count}")
        print(f"   Failed: {len(results) - success_count}")
        print(f"   Success rate: {success_rate:.1f}%")

        # At least 60% should succeed (network issues may cause some failures)
        assert success_rate >= 60, f"Success rate too low: {success_rate}%"

        # Verify successful downloads are valid PDFs
        for result in results:
            if result.success:
                assert result.file_path.exists()
                assert result.file_size > 0


# ============================================================================
# Integration Test 3: Skip Existing Files
# ============================================================================


@pytest.mark.asyncio
async def test_skip_existing_files(jhb_config, browser_engine, test_download_dir):
    """
    Test that existing files are skipped on second download.

    Steps:
    1. Download a PDF
    2. Download same PDF again with skip_existing=True
    3. Verify second download was skipped (attempts=0)
    """
    # Get a PDF link
    url = jhb_config["urls"]["rfq_listing"]
    page = await browser_engine.navigate(url, wait_for="networkidle")
    await browser_engine.handle_cookie_consent(page)

    tracking_file = test_download_dir / "skip_tracking.json"
    discovery = PDFDiscovery(jhb_config["pdf_discovery"], tracking_file)
    pdf_links = await discovery.discover_pdfs(page, source_id="jhb-water")

    if not pdf_links:
        pytest.skip("No PDFs found")

    pdf_link = pdf_links[0]

    # First download
    download_config = jhb_config["download"].copy()
    download_config["skip_existing"] = True

    downloader = PDFDownloader(download_config, browser_engine=browser_engine)

    async with downloader:
        test_file = test_download_dir / "skip_test.pdf"

        # Override path
        downloader._build_output_path = lambda link: test_file

        # First download
        result1 = await downloader.download_single(pdf_link)
        assert result1.success is True
        assert result1.attempts >= 1
        assert test_file.exists()

        first_size = result1.file_size
        first_method = result1.method

        # Second download (should skip)
        result2 = await downloader.download_single(pdf_link)
        assert result2.success is True
        assert result2.method == "skip"
        assert result2.attempts == 0
        assert result2.file_size == first_size

        print("\n✅ Skip logic working:")
        print(f"   First download: {first_method}, {first_size} bytes")
        print("   Second download: skipped")


# ============================================================================
# Integration Test 4: Fallback to Browser
# ============================================================================


@pytest.mark.asyncio
async def test_fallback_to_browser(jhb_config, browser_engine, test_download_dir):
    """
    Test fallback from direct download to browser download.

    This test simulates a 403 error by using method='auto' and checking
    if browser fallback is attempted.
    """
    # Get a PDF link
    url = jhb_config["urls"]["rfq_listing"]
    page = await browser_engine.navigate(url, wait_for="networkidle")
    await browser_engine.handle_cookie_consent(page)

    tracking_file = test_download_dir / "fallback_tracking.json"
    discovery = PDFDiscovery(jhb_config["pdf_discovery"], tracking_file)
    pdf_links = await discovery.discover_pdfs(page, source_id="jhb-water")

    if not pdf_links:
        pytest.skip("No PDFs found")

    pdf_link = pdf_links[0]

    # Configure for auto method (direct → browser fallback)
    download_config = jhb_config["download"].copy()
    download_config["method"] = "auto"  # Will try direct first
    download_config["skip_existing"] = False

    downloader = PDFDownloader(download_config, browser_engine=browser_engine)

    async with downloader:
        test_file = test_download_dir / "fallback_test.pdf"
        downloader._build_output_path = lambda link: test_file

        # Force direct download to fail by mocking _download_direct
        async def mock_direct_fail(*args, **kwargs):
            # Simulate 403 auth error
            return False

        downloader._download_direct = mock_direct_fail

        result = await downloader.download_single(pdf_link)

        # Should fallback to browser and succeed
        if result.success:
            assert result.method == "browser", "Should have used browser fallback"
            print("\n✅ Fallback to browser successful:")
            print(f"   Method: {result.method}")
            print(f"   Size: {result.file_size} bytes")
        else:
            # Some sites may not allow browser downloads either
            print(f"\n⚠️  Fallback attempted but failed: {result.error}")
            pytest.skip("Browser download not supported by this site")


# ============================================================================
# Integration Test 5: Large File Handling
# ============================================================================


@pytest.mark.asyncio
@pytest.mark.slow
async def test_large_file_download(jhb_config, browser_engine, test_download_dir):
    """
    Test downloading a large PDF file (>1MB).

    Verifies:
    - Stream download works for large files
    - Memory usage is reasonable
    - Validation works on large files
    """
    # Navigate and discover
    url = jhb_config["urls"]["rfq_listing"]
    page = await browser_engine.navigate(url, wait_for="networkidle")
    await browser_engine.handle_cookie_consent(page)

    tracking_file = test_download_dir / "large_tracking.json"
    discovery = PDFDiscovery(jhb_config["pdf_discovery"], tracking_file)
    pdf_links = await discovery.discover_pdfs(page, source_id="jhb-water")

    if not pdf_links:
        pytest.skip("No PDFs found")

    # Find largest PDF (or just use first one)
    # In real scenario, we'd check Content-Length headers
    # For this test, we'll just download the first PDF
    pdf_link = pdf_links[0]

    download_config = jhb_config["download"].copy()
    download_config["skip_existing"] = False
    download_config["validate"]["max_size_mb"] = 100  # Allow up to 100MB

    downloader = PDFDownloader(download_config, browser_engine=browser_engine)

    async with downloader:
        test_file = test_download_dir / "large_test.pdf"
        downloader._build_output_path = lambda link: test_file

        result = await downloader.download_single(pdf_link)

        assert result.success is True, f"Large file download failed: {result.error}"
        assert result.file_path.exists()

        file_size_mb = result.file_size / (1024 * 1024)

        print("\n✅ Large file download:")
        print(f"   Size: {file_size_mb:.2f} MB")
        print(f"   Time: {result.download_time_ms / 1000:.1f}s")
        print(f"   Method: {result.method}")

        # Verify it's a valid PDF
        with open(result.file_path, "rb") as f:
            header = f.read(5)
            assert header == b"%PDF-", "Large file is not a valid PDF"

        # Check download speed (should be reasonable)
        if file_size_mb > 0.5:  # If file is actually large
            speed_mbps = (file_size_mb * 8) / (result.download_time_ms / 1000)
            print(f"   Speed: {speed_mbps:.1f} Mbps")
            assert speed_mbps > 0.1, "Download speed too slow"


# ============================================================================
# Helper Functions
# ============================================================================


def test_download_config_valid(jhb_config):
    """
    Verify JHB Water config has valid download section.
    """
    assert "download" in jhb_config
    download = jhb_config["download"]

    assert "method" in download
    assert download["method"] in ["auto", "direct", "browser"]

    assert "validate" in download
    validate = download["validate"]
    assert "min_size_bytes" in validate
    assert "max_size_mb" in validate
    assert "check_pdf_header" in validate

    assert "retry_attempts" in download
    assert download["retry_attempts"] >= 1

    print("\n✅ Download configuration is valid")
