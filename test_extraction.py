#!/usr/bin/env python3
"""
Test PDF extraction with existing downloaded PDFs.
"""

import asyncio
from pathlib import Path
from src.extractor.pdf_extractor import PDFExtractor

async def main():
    # Configure extractor
    config = {
        "method": "auto",
        "ocr_language": "eng",
        "ocr_confidence_threshold": 0.7,
        "min_text_length": 100,
        "max_concurrent": 3,
        "skip_existing": False,  # Re-extract for testing
        "output_base_path": "documents/extracted"
    }

    extractor = PDFExtractor(config)

    # Find all PDFs
    pdf_dir = Path("documents/downloads/jhb-water/2026-01")
    pdf_files = list(pdf_dir.glob("*.pdf"))

    print(f"\n{'='*60}")
    print(f"PDF EXTRACTION TEST")
    print(f"{'='*60}")
    print(f"Found {len(pdf_files)} PDFs to extract\n")

    for pdf_path in pdf_files:
        print(f"  - {pdf_path.name} ({pdf_path.stat().st_size:,} bytes)")

    print(f"\n{'='*60}\n")

    # Extract batch
    results = await extractor.extract_batch(pdf_files, max_concurrent=3)

    # Print results
    print(f"\n{'='*60}")
    print(f"EXTRACTION RESULTS")
    print(f"{'='*60}\n")

    success_count = sum(1 for r in results if r.success)
    total_chars = sum(r.text_length for r in results if r.success)
    total_pages = sum(r.page_count for r in results if r.success)

    print(f"Total PDFs: {len(results)}")
    print(f"Successful: {success_count}")
    print(f"Failed: {len(results) - success_count}")
    print(f"Total Pages: {total_pages}")
    print(f"Total Characters: {total_chars:,}")
    print(f"\nPer-file results:\n")

    for result in results:
        status = "✅ SUCCESS" if result.success else "❌ FAILED"
        print(f"{status} | {result.pdf_path.name}")
        print(f"  Method: {result.extraction_method}")
        print(f"  Pages: {result.page_count}")
        print(f"  Characters: {result.text_length:,}")
        print(f"  Time: {result.extraction_time_ms}ms")

        if result.success:
            print(f"  Text: {result.text_path}")
            print(f"  Metadata: {result.metadata_path}")
        else:
            print(f"  Error: {result.error}")

        print()

if __name__ == "__main__":
    asyncio.run(main())
