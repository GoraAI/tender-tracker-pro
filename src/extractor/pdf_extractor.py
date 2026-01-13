"""
PDF text extraction with OCR fallback.

This module provides functionality to extract text from PDFs using:
- pdfplumber for digital PDFs (fast)
- pytesseract OCR for scanned/image PDFs (slower)
- Automatic detection and fallback
- Metadata extraction
- Batch processing with concurrency
"""

import asyncio
import json
import time
from pathlib import Path
from typing import List, Optional, Tuple
from datetime import datetime

import pdfplumber
import pytesseract
import structlog

from .models import ExtractionResult, PDFMetadata

logger = structlog.get_logger()


class PDFExtractor:
    """
    Extract text from PDFs with OCR fallback for scanned documents.

    Features:
    - Digital PDF text extraction (pdfplumber)
    - OCR for scanned PDFs (pytesseract)
    - Automatic scanned detection
    - Metadata extraction
    - Concurrent batch processing
    - Organized storage by source and date
    - Skip already extracted files
    """

    def __init__(self, config: dict):
        """
        Initialize PDF extractor.

        Args:
            config: extraction section from source config with keys:
                - method: "auto", "pdfplumber", or "ocr"
                - ocr_language: str (default "eng" for English)
                - ocr_confidence_threshold: float (default 0.7)
                - min_text_length: int (minimum chars, default 100)
                - max_concurrent: int (default 3)
                - skip_existing: bool (default True)
                - output_base_path: str (default "documents/extracted")
        """
        self.config = config
        self.extraction_method = config.get("method", "auto")
        self.ocr_language = config.get("ocr_language", "eng")
        self.ocr_confidence_threshold = config.get("ocr_confidence_threshold", 0.7)
        self.min_text_length = config.get("min_text_length", 100)
        self.max_concurrent = config.get("max_concurrent", 3)
        self.skip_existing = config.get("skip_existing", True)
        self.output_base_path = Path(
            config.get("output_base_path", "documents/extracted")
        )

    async def extract_batch(
        self, pdf_paths: List[Path], max_concurrent: Optional[int] = None
    ) -> List[ExtractionResult]:
        """
        Extract text from multiple PDFs concurrently.

        Args:
            pdf_paths: List of PDF file paths
            max_concurrent: Override default concurrency limit

        Returns:
            List of ExtractionResults (one per PDF)
        """
        concurrent_limit = max_concurrent or self.max_concurrent
        semaphore = asyncio.Semaphore(concurrent_limit)

        async def extract_with_limit(pdf_path: Path) -> ExtractionResult:
            async with semaphore:
                return await self.extract_single(pdf_path)

        logger.info(
            "extraction_batch_started",
            total=len(pdf_paths),
            concurrent=concurrent_limit,
        )

        # Create tasks
        tasks = [extract_with_limit(path) for path in pdf_paths]

        # Execute concurrently
        results = await asyncio.gather(*tasks, return_exceptions=True)

        # Convert exceptions to failed ExtractionResults
        processed_results = []
        for i, result in enumerate(results):
            if isinstance(result, Exception):
                processed_results.append(
                    ExtractionResult(
                        pdf_path=pdf_paths[i],
                        success=False,
                        text_path=None,
                        metadata_path=None,
                        page_count=0,
                        text_length=0,
                        extraction_method="error",
                        ocr_confidence=None,
                        extraction_time_ms=0,
                        error=str(result),
                    )
                )
            else:
                processed_results.append(result)

        # Log summary
        success_count = sum(1 for r in processed_results if r.success)
        ocr_count = sum(1 for r in processed_results if r.extraction_method == "ocr")
        total_chars = sum(r.text_length for r in processed_results if r.success)

        logger.info(
            "extraction_batch_complete",
            total=len(processed_results),
            success=success_count,
            failed=len(processed_results) - success_count,
            ocr_count=ocr_count,
            total_chars=total_chars,
        )

        return processed_results

    async def extract_single(self, pdf_path: Path) -> ExtractionResult:
        """
        Extract text from a single PDF.

        Args:
            pdf_path: Path to PDF file

        Returns:
            ExtractionResult with extracted text location and metadata
        """
        start_time = time.time()

        try:
            # Build output paths
            text_path, metadata_path = self._build_output_paths(pdf_path)

            # Check if should skip
            if self.skip_existing and self._should_skip(pdf_path, text_path):
                logger.info("extraction_skipped_existing", pdf_path=str(pdf_path))

                # Load metadata to get page count
                if metadata_path.exists():
                    with open(metadata_path, "r") as f:
                        metadata_dict = json.load(f)
                        page_count = metadata_dict.get("page_count", 0)
                else:
                    page_count = 0

                # Get text length
                text_length = len(text_path.read_text()) if text_path.exists() else 0

                return ExtractionResult(
                    pdf_path=pdf_path,
                    success=True,
                    text_path=text_path,
                    metadata_path=metadata_path,
                    page_count=page_count,
                    text_length=text_length,
                    extraction_method="skip",
                    ocr_confidence=None,
                    extraction_time_ms=0,
                    error=None,
                )

            # Run extraction in thread pool (blocking I/O)
            loop = asyncio.get_event_loop()
            result = await loop.run_in_executor(
                None, self._extract_sync, pdf_path, text_path, metadata_path
            )

            duration_ms = int((time.time() - start_time) * 1000)
            result.extraction_time_ms = duration_ms

            logger.info(
                "extraction_success",
                pdf_path=str(pdf_path),
                page_count=result.page_count,
                text_length=result.text_length,
                method=result.extraction_method,
                duration_ms=duration_ms,
            )

            return result

        except Exception as e:
            duration_ms = int((time.time() - start_time) * 1000)
            logger.error("extraction_failed", pdf_path=str(pdf_path), error=str(e))

            return ExtractionResult(
                pdf_path=pdf_path,
                success=False,
                text_path=None,
                metadata_path=None,
                page_count=0,
                text_length=0,
                extraction_method="error",
                ocr_confidence=None,
                extraction_time_ms=duration_ms,
                error=str(e),
            )

    def _extract_sync(
        self, pdf_path: Path, text_path: Path, metadata_path: Path
    ) -> ExtractionResult:
        """
        Synchronous extraction (runs in thread pool).

        Args:
            pdf_path: PDF file path
            text_path: Where to save extracted text
            metadata_path: Where to save metadata

        Returns:
            ExtractionResult
        """
        # Extract metadata
        metadata = self.extract_metadata(pdf_path)

        # Detect if scanned
        is_scanned = self.detect_scanned(pdf_path)
        metadata.is_scanned = is_scanned

        # Choose extraction method
        if self.extraction_method == "auto":
            if is_scanned:
                extracted_text, ocr_confidence = self.extract_text_ocr(pdf_path)
                method = "ocr"
            else:
                extracted_text, _ = self.extract_text_pdfplumber(pdf_path)
                ocr_confidence = None
                method = "pdfplumber"
        elif self.extraction_method == "pdfplumber":
            extracted_text, _ = self.extract_text_pdfplumber(pdf_path)
            ocr_confidence = None
            method = "pdfplumber"
        elif self.extraction_method == "ocr":
            extracted_text, ocr_confidence = self.extract_text_ocr(pdf_path)
            method = "ocr"
        else:
            raise ValueError(f"Unknown extraction method: {self.extraction_method}")

        # Validate extracted text
        if len(extracted_text.strip()) < self.min_text_length:
            raise ValueError(
                f"Extracted text too short: {len(extracted_text)} chars "
                f"(min: {self.min_text_length})"
            )

        # Save text
        text_path.parent.mkdir(parents=True, exist_ok=True)
        text_path.write_text(extracted_text, encoding="utf-8")

        # Save metadata
        metadata_path.parent.mkdir(parents=True, exist_ok=True)
        with open(metadata_path, "w") as f:
            json.dump(metadata.to_dict(), f, indent=2)

        return ExtractionResult(
            pdf_path=pdf_path,
            success=True,
            text_path=text_path,
            metadata_path=metadata_path,
            page_count=metadata.page_count,
            text_length=len(extracted_text),
            extraction_method=method,
            ocr_confidence=ocr_confidence,
            extraction_time_ms=0,  # Will be set by caller
            error=None,
        )

    def extract_text_pdfplumber(self, pdf_path: Path) -> Tuple[str, PDFMetadata]:
        """
        Extract text using pdfplumber (digital PDFs).

        Args:
            pdf_path: Path to PDF file

        Returns:
            Tuple of (extracted_text, metadata)
        """
        with pdfplumber.open(pdf_path) as pdf:
            pages_text = []

            for page_num, page in enumerate(pdf.pages, 1):
                page_text = page.extract_text()
                if page_text:
                    pages_text.append(f"=== Page {page_num} ===\n{page_text}\n")
                else:
                    pages_text.append(
                        f"=== Page {page_num} ===\n[No text on this page]\n"
                    )

            extracted_text = "\n".join(pages_text)

            # Extract metadata
            metadata = self.extract_metadata(pdf_path)

            return extracted_text, metadata

    def extract_text_ocr(self, pdf_path: Path) -> Tuple[str, float]:
        """
        Extract text using pytesseract OCR (scanned PDFs).

        Args:
            pdf_path: Path to PDF file

        Returns:
            Tuple of (extracted_text, average_confidence)
        """
        with pdfplumber.open(pdf_path) as pdf:
            pages_text = []
            confidences = []

            for page_num, page in enumerate(pdf.pages, 1):
                # Convert page to image
                img = page.to_image(resolution=300)
                pil_img = img.original

                # OCR with confidence
                ocr_data = pytesseract.image_to_data(
                    pil_img, lang=self.ocr_language, output_type=pytesseract.Output.DICT
                )

                # Extract text and confidence
                page_text_parts = []
                page_confidences = []

                for i, conf in enumerate(ocr_data["conf"]):
                    if conf > 0:  # Valid confidence score
                        text = ocr_data["text"][i]
                        if text.strip():
                            page_text_parts.append(text)
                            page_confidences.append(conf / 100.0)  # Convert to 0-1

                page_text = " ".join(page_text_parts)
                avg_confidence = (
                    sum(page_confidences) / len(page_confidences)
                    if page_confidences
                    else 0.0
                )

                pages_text.append(
                    f"=== Page {page_num} (OCR confidence: {avg_confidence:.2f}) ===\n"
                    f"{page_text}\n"
                )
                confidences.append(avg_confidence)

            extracted_text = "\n".join(pages_text)
            overall_confidence = (
                sum(confidences) / len(confidences) if confidences else 0.0
            )

            return extracted_text, overall_confidence

    def detect_scanned(self, pdf_path: Path) -> bool:
        """
        Detect if PDF is image-based (needs OCR).

        Args:
            pdf_path: Path to PDF file

        Returns:
            True if scanned (>80% of pages have no text), False otherwise
        """
        with pdfplumber.open(pdf_path) as pdf:
            total_pages = len(pdf.pages)
            pages_with_no_text = 0

            for page in pdf.pages:
                text = page.extract_text()
                if (
                    not text or len(text.strip()) < 50
                ):  # Less than 50 chars = likely scanned
                    pages_with_no_text += 1

            scanned_ratio = pages_with_no_text / total_pages if total_pages > 0 else 0
            return scanned_ratio > 0.8  # More than 80% pages have no text

    def extract_metadata(self, pdf_path: Path) -> PDFMetadata:
        """
        Extract PDF metadata.

        Args:
            pdf_path: Path to PDF file

        Returns:
            PDFMetadata object
        """
        with pdfplumber.open(pdf_path) as pdf:
            metadata_dict = pdf.metadata or {}

            # Parse dates
            creation_date = None
            if metadata_dict.get("CreationDate"):
                try:
                    # PDF dates are in format: D:YYYYMMDDHHmmSS
                    date_str = metadata_dict["CreationDate"].replace("D:", "")
                    creation_date = datetime.strptime(date_str[:14], "%Y%m%d%H%M%S")
                except Exception:
                    pass

            modification_date = None
            if metadata_dict.get("ModDate"):
                try:
                    date_str = metadata_dict["ModDate"].replace("D:", "")
                    modification_date = datetime.strptime(date_str[:14], "%Y%m%d%H%M%S")
                except Exception:
                    pass

            return PDFMetadata(
                title=metadata_dict.get("Title"),
                author=metadata_dict.get("Author"),
                subject=metadata_dict.get("Subject"),
                keywords=metadata_dict.get("Keywords"),
                creation_date=creation_date,
                modification_date=modification_date,
                page_count=len(pdf.pages),
                file_size=pdf_path.stat().st_size,
                pdf_version=str(metadata_dict.get("PDFVersion", "unknown")),
                is_encrypted=(
                    pdf.is_encrypted if hasattr(pdf, "is_encrypted") else False
                ),
                is_scanned=False,  # Will be set by caller
            )

    def _build_output_paths(self, pdf_path: Path) -> Tuple[Path, Path]:
        """
        Build organized paths for extracted text and metadata.

        Format:
        - Text: documents/extracted/{source_id}/{YYYY-MM}/filename.txt
        - Metadata: documents/extracted/{source_id}/{YYYY-MM}/filename_meta.json

        Args:
            pdf_path: Original PDF path

        Returns:
            Tuple of (text_path, metadata_path)
        """
        # Extract source_id and date from PDF path
        # PDF path format: documents/downloads/{source_id}/{YYYY-MM}/filename.pdf
        parts = pdf_path.parts

        # Find source_id (second-to-last before date folder)
        if len(parts) >= 4 and "downloads" in parts:
            downloads_idx = list(parts).index("downloads")
            source_id = parts[downloads_idx + 1]
            date_folder = parts[downloads_idx + 2]
        else:
            # Fallback: use "unknown" if path doesn't match expected format
            source_id = "unknown"
            date_folder = datetime.now().strftime("%Y-%m")

        # Build output directory
        output_dir = self.output_base_path / source_id / date_folder

        # Build file paths
        text_filename = pdf_path.stem + ".txt"
        metadata_filename = pdf_path.stem + "_meta.json"

        text_path = output_dir / text_filename
        metadata_path = output_dir / metadata_filename

        return text_path, metadata_path

    def _should_skip(self, pdf_path: Path, text_path: Path) -> bool:
        """
        Check if PDF should be skipped (already extracted).

        Args:
            pdf_path: Original PDF path
            text_path: Expected text output path

        Returns:
            True if should skip, False otherwise
        """
        return text_path.exists()
