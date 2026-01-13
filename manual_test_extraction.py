#!/usr/bin/env python3
"""
FR-EXTRACT-001: PDF Text Extraction - Manual Test

Tests text extraction from downloaded PDFs.
"""

import asyncio
import sys
from pathlib import Path

# Add src to path
sys.path.insert(0, str(Path(__file__).parent / "src"))

from extractor import PDFExtractor


async def main():
    print("=" * 80)
    print("FR-EXTRACT-001: PDF Text Extraction - Manual Test")
    print("=" * 80)
    print()

    # Find downloaded PDFs
    download_dir = Path("documents/downloads/jhb-water/2026-01")
    if not download_dir.exists():
        print(f"❌ Download directory not found: {download_dir}")
        print("   Run manual_test_download.py first!")
        return

    pdf_files = list(download_dir.glob("*.pdf"))
    if not pdf_files:
        print(f"❌ No PDFs found in {download_dir}")
        return

    print(f"📁 Found {len(pdf_files)} PDFs to extract")
    print()

    # Initialize extractor
    config = {
        "method": "auto",  # Auto-detect digital vs scanned
        "ocr_language": "eng",
        "ocr_confidence_threshold": 0.7,
        "min_text_length": 100,
        "max_concurrent": 3,
        "skip_existing": False,  # Force re-extraction for test
        "output_base_path": "documents/extracted",
    }

    extractor = PDFExtractor(config)

    # Extract from all PDFs
    print("1️⃣  Extracting text from PDFs...")
    print(f"   Max concurrent: {config['max_concurrent']}")
    print(f"   Extraction method: {config['method']}")
    print()

    results = await extractor.extract_batch(pdf_files)

    # Display results
    print()
    print("=" * 80)
    print("📊 Extraction Results")
    print("=" * 80)
    print()

    success_count = sum(1 for r in results if r.success)
    failed_count = len(results) - success_count
    total_chars = sum(r.text_length for r in results if r.success)
    ocr_count = sum(1 for r in results if r.extraction_method == "ocr")

    print(f"✅ Successful: {success_count}/{len(results)}")
    print(f"❌ Failed: {failed_count}/{len(results)}")
    print(f"📝 Total characters: {total_chars:,}")
    print(f"🔍 OCR used: {ocr_count}/{len(results)}")
    print()

    print("Individual Results:")
    print("-" * 80)
    print()

    for i, result in enumerate(results, 1):
        pdf_name = result.pdf_path.name[:50]
        if result.success:
            print(f"{i}. {pdf_name}")
            print(f"   ✅ Success")
            print(f"   📁 Text: {result.text_path}")
            print(f"   📄 Pages: {result.page_count}")
            print(f"   📝 Characters: {result.text_length:,}")
            print(f"   🔧 Method: {result.extraction_method}")
            if result.ocr_confidence:
                print(f"   🎯 OCR Confidence: {result.ocr_confidence:.1%}")
            print(f"   ⏱️  Time: {result.extraction_time_ms / 1000:.2f}s")

            # Show first 200 chars of extracted text
            if result.text_path and result.text_path.exists():
                text_sample = result.text_path.read_text()[:200]
                print(f"   📖 Sample: {text_sample}...")
        else:
            print(f"{i}. {pdf_name}")
            print(f"   ❌ Failed: {result.error}")

        print()

    print("=" * 80)
    print("✅ Extraction test complete!")
    print("=" * 80)
    print()
    print("📁 Extracted text location:")
    print(f"   documents/extracted/jhb-water/2026-01/")
    print()
    print("   You can verify extracted text with:")
    print("   ls -lh documents/extracted/jhb-water/2026-01/")
    print("   cat documents/extracted/jhb-water/2026-01/MBD-4.txt")
    print()


if __name__ == "__main__":
    asyncio.run(main())
