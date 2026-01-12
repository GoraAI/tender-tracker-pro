#!/usr/bin/env python3
"""
Manual test script for PDF Discovery + Download Pipeline.

This script demonstrates the full workflow:
1. Navigate to JHB Water RFQ listing
2. Discover PDF links
3. Download PDFs with validation and retry
4. Display results

Usage:
    python manual_test_download.py
"""

import asyncio
import yaml
from pathlib import Path

from src.navigator import BrowserEngine, PDFDiscovery, PDFDownloader


async def main():
    print("=" * 80)
    print("FR-NAV-002 + FR-NAV-003: PDF Discovery & Download - Manual Test")
    print("=" * 80)
    print()

    # Load configuration
    config_path = Path("config/sources/jhb_water.yaml")
    with open(config_path) as f:
        jhb_config = yaml.safe_load(f)

    # Browser config
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

    # PDF discovery config
    tracking_file = Path("data/tracking/jhb_water_pdfs_manual_test_download.json")

    # Initialize
    print("1️⃣  Initializing browser...")
    engine = BrowserEngine(browser_config)
    discovery = PDFDiscovery(jhb_config["pdf_discovery"], tracking_file)
    downloader = PDFDownloader(jhb_config["download"], browser_engine=engine)

    try:
        await engine.initialize()
        print("   ✅ Browser initialized")
        print()

        # Navigate
        url = jhb_config["urls"]["rfq_listing"]
        print(f"2️⃣  Navigating to: {url}")
        page = await engine.navigate(url, wait_for="networkidle")
        print(f"   ✅ Page loaded: {page.url}")
        print()

        # Handle cookies
        print("3️⃣  Handling cookie consent...")
        consent_handled = await engine.handle_cookie_consent(page)
        if consent_handled:
            print("   ✅ Cookie consent accepted")
        else:
            print("   ℹ️  No cookie consent dialog found")
        print()

        # Discover PDFs
        print("4️⃣  Discovering PDF links...")
        pdf_links = await discovery.discover_pdfs(page, source_id="jhb-water")
        print(f"   ✅ Found {len(pdf_links)} NEW PDF links")
        print()

        if not pdf_links:
            print("   ℹ️  No new PDFs found (may already be tracked)")
            print("   💡 Delete the tracking file to re-discover:")
            print(f"      rm {tracking_file}")
            print()
            return

        # Display discovered PDFs
        print("📄 Discovered PDFs:")
        print("-" * 80)
        for i, pdf in enumerate(pdf_links[:5], 1):  # Show first 5
            print(f"\n   {i}. {pdf.link_text[:60]}...")
            print(f"      URL: {pdf.url}")
            if pdf.context:
                print(f"      Context: {pdf.context[:80]}...")

        if len(pdf_links) > 5:
            print(f"\n   ... and {len(pdf_links) - 5} more PDFs")
        print()

        # Save tracking
        print("5️⃣  Saving discovery to tracking file...")
        discovery.save_tracking(pdf_links)
        print(f"   ✅ Tracking saved to: {tracking_file}")
        print()

        # Download PDFs
        print("6️⃣  Downloading PDFs...")
        print(f"   Max concurrent: {jhb_config['download']['max_concurrent']}")
        print(f"   Download method: {jhb_config['download']['method']}")
        print()

        # Limit to first 3 PDFs for manual testing
        pdfs_to_download = pdf_links[:min(3, len(pdf_links))]
        print(f"   Downloading first {len(pdfs_to_download)} PDFs...")
        print()

        async with downloader:
            results = await downloader.download_batch(
                pdfs_to_download,
                max_concurrent=2  # Slower for manual observation
            )

        # Display results
        print()
        print("=" * 80)
        print("📊 Download Results")
        print("=" * 80)
        print()

        success_count = sum(1 for r in results if r.success)
        failed_count = len(results) - success_count
        total_size = sum(r.file_size for r in results if r.success)
        total_time = sum(r.download_time_ms for r in results)

        print(f"✅ Successful: {success_count}/{len(results)}")
        print(f"❌ Failed: {failed_count}/{len(results)}")
        print(f"📦 Total size: {total_size / 1024 / 1024:.2f} MB")
        print(f"⏱️  Total time: {total_time / 1000:.1f}s")
        print()

        # Show individual results
        print("Individual Results:")
        print("-" * 80)
        for i, result in enumerate(results, 1):
            print(f"\n{i}. {result.pdf_link.link_text[:50]}...")
            if result.success:
                print(f"   ✅ Success")
                print(f"   📁 Path: {result.file_path}")
                print(f"   📦 Size: {result.file_size / 1024:.1f} KB")
                print(f"   ⏱️  Time: {result.download_time_ms / 1000:.2f}s")
                print(f"   🔧 Method: {result.method}")
                print(f"   🔄 Attempts: {result.attempts}")
            else:
                print(f"   ❌ Failed")
                print(f"   ⚠️  Error: {result.error}")
                print(f"   🔄 Attempts: {result.attempts}")

        print()
        print("=" * 80)
        print("✅ Manual test complete!")
        print("=" * 80)
        print()

        # Show where files are stored
        if success_count > 0:
            print("📁 Downloaded files location:")
            print("   documents/downloads/jhb-water/2026-01/")
            print()
            print("   You can verify PDFs with:")
            print("   ls -lh documents/downloads/jhb-water/2026-01/")
            print("   file documents/downloads/jhb-water/2026-01/*.pdf")
            print()

        print(f"📁 Tracking file: {tracking_file}")
        print(f"📁 Logs: logs/tender_tracker.log")

    except Exception as e:
        print(f"❌ Error: {e}")
        import traceback
        traceback.print_exc()

    finally:
        # Cleanup
        print()
        print("7️⃣  Cleaning up...")
        await engine.close()
        print("   ✅ Browser closed")


if __name__ == "__main__":
    asyncio.run(main())
