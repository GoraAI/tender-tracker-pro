"""
Extractor module for PDF text extraction.

Main components:
- PDFExtractor: Extract text from PDFs with OCR fallback
- ExtractionResult: Extraction result data model
- PDFMetadata: PDF metadata data model
"""

from .pdf_extractor import PDFExtractor
from .models import ExtractionResult, PDFMetadata

__all__ = [
    "PDFExtractor",
    "ExtractionResult",
    "PDFMetadata",
]
