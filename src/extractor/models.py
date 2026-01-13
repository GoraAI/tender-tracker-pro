"""
Data models for PDF text extraction.

Contains:
- ExtractionResult: Result of a PDF text extraction attempt
- PDFMetadata: Metadata extracted from PDF
"""

from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Optional


@dataclass
class PDFMetadata:
    """Metadata extracted from a PDF file."""

    title: Optional[str]
    author: Optional[str]
    subject: Optional[str]
    keywords: Optional[str]
    creation_date: Optional[datetime]
    modification_date: Optional[datetime]
    page_count: int
    file_size: int  # bytes
    pdf_version: str
    is_encrypted: bool
    is_scanned: bool  # True if image-based PDF

    def to_dict(self) -> dict:
        """Convert to dictionary for JSON serialization."""
        return {
            "title": self.title,
            "author": self.author,
            "subject": self.subject,
            "keywords": self.keywords,
            "creation_date": (
                self.creation_date.isoformat() if self.creation_date else None
            ),
            "modification_date": (
                self.modification_date.isoformat() if self.modification_date else None
            ),
            "page_count": self.page_count,
            "file_size": self.file_size,
            "pdf_version": self.pdf_version,
            "is_encrypted": self.is_encrypted,
            "is_scanned": self.is_scanned,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "PDFMetadata":
        """Create PDFMetadata from dictionary."""
        return cls(
            title=data.get("title"),
            author=data.get("author"),
            subject=data.get("subject"),
            keywords=data.get("keywords"),
            creation_date=(
                datetime.fromisoformat(data["creation_date"])
                if data.get("creation_date")
                else None
            ),
            modification_date=(
                datetime.fromisoformat(data["modification_date"])
                if data.get("modification_date")
                else None
            ),
            page_count=data["page_count"],
            file_size=data["file_size"],
            pdf_version=data["pdf_version"],
            is_encrypted=data["is_encrypted"],
            is_scanned=data["is_scanned"],
        )


@dataclass
class ExtractionResult:
    """Result of a PDF text extraction attempt."""

    pdf_path: Path
    success: bool
    text_path: Optional[Path]  # Where extracted text was saved
    metadata_path: Optional[Path]  # Where metadata was saved
    page_count: int
    text_length: int  # Character count
    extraction_method: str  # "pdfplumber", "ocr", or "hybrid"
    ocr_confidence: Optional[float]  # 0.0-1.0 if OCR was used
    extraction_time_ms: int
    error: Optional[str]

    def to_dict(self) -> dict:
        """Convert to dictionary for logging."""
        return {
            "pdf_path": str(self.pdf_path),
            "success": self.success,
            "text_path": str(self.text_path) if self.text_path else None,
            "metadata_path": str(self.metadata_path) if self.metadata_path else None,
            "page_count": self.page_count,
            "text_length": self.text_length,
            "extraction_method": self.extraction_method,
            "ocr_confidence": self.ocr_confidence,
            "extraction_time_ms": self.extraction_time_ms,
            "error": self.error,
        }
