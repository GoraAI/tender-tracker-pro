#!/usr/bin/env python3
"""
Manual test script for PDF Link Discovery.

Usage:
    python manual_test_discovery.py
"""

import asyncio
import yaml
from pathlib import Path

from src.navigator import BrowserEngine, PDFDiscovery


async def main():
    print("=" * 70)
    print("FR-NAV-002: PDF Link Discovery - Manual Test")
    print("=" * 70)
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
    tracking_file = Path("data/tracking/jhb_water_pdfs_manual_test.json")

    # Initialize
    print("1️⃣  Initializing browser...")
    engine = BrowserEngine(browser_config)
    discovery = PDFDiscovery(jhb_config["pdf_discovery"], tracking_file)

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

        # Display results
        if pdf_links:
            print("📄 Discovered PDFs:")
            print("-" * 70)
            for i, pdf in enumerate(pdf_links[:5], 1):  # Show first 5
                print(f"\n   {i}. {pdf.link_text[:60]}...")
                print(f"      URL: {pdf.url}")
                if pdf.context:
                    print(f"      Context: {pdf.context[:80]}...")

            if len(pdf_links) > 5:
                print(f"\n   ... and {len(pdf_links) - 5} more PDFs")
        else:
            print("   ℹ️  No new PDFs found (may already be tracked)")
        print()

        # Save tracking
        print("5️⃣  Saving to tracking file...")
        discovery.save_tracking(pdf_links)
        print(f"   ✅ Tracking saved to: {tracking_file}")
        print()

        # Show tracking file contents
        print("📊 Tracking File Status:")
        print("-" * 70)
        tracking = discovery.load_tracking()
        total_pdfs = len(tracking["discovered_pdfs"])
        print(f"   Total PDFs tracked: {total_pdfs}")
        print(f"   Last updated: {tracking['last_updated']}")
        print()

        # Test deduplication
        print("6️⃣  Testing deduplication (running discovery again)...")
        pdf_links_run2 = await discovery.discover_pdfs(page, source_id="jhb-water")
        print(f"   ✅ Second run found {len(pdf_links_run2)} new PDFs")
        print(f"   ✅ Deduplication working: {len(pdf_links_run2) == 0}")
        print()

        print("=" * 70)
        print("✅ Manual test complete!")
        print("=" * 70)
        print()
        print(f"📁 Check tracking file: {tracking_file}")
        print(f"📁 View logs: logs/tender_tracker.log")

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
