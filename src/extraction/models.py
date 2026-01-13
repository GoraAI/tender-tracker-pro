"""
Data models for tender information extraction.
"""

from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Optional


@dataclass
class TenderInfo:
    """Extracted tender information."""

    # Source
    pdf_path: Path

    # Required fields
    title: str
    reference: str

    # Optional fields
    deadline: Optional[datetime]
    estimated_value: Optional[float]  # ZAR
    requirements_summary: Optional[str]
    contact_name: Optional[str]
    contact_email: Optional[str]

    # Metadata
    confidence: float  # 0.0-1.0
    extraction_time_ms: int
    raw_response: str  # LLM raw output

    def to_dict(self) -> dict:
        """Convert to dictionary for JSON serialization."""
        return {
            "pdf_path": str(self.pdf_path),
            "title": self.title,
            "reference": self.reference,
            "deadline": self.deadline.isoformat() if self.deadline else None,
            "estimated_value": self.estimated_value,
            "requirements_summary": self.requirements_summary,
            "contact_name": self.contact_name,
            "contact_email": self.contact_email,
            "confidence": self.confidence,
            "extraction_time_ms": self.extraction_time_ms,
            "raw_response": self.raw_response,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "TenderInfo":
        """Create TenderInfo from dictionary."""
        return cls(
            pdf_path=Path(data["pdf_path"]),
            title=data["title"],
            reference=data["reference"],
            deadline=(
                datetime.fromisoformat(data["deadline"])
                if data.get("deadline")
                else None
            ),
            estimated_value=data.get("estimated_value"),
            requirements_summary=data.get("requirements_summary"),
            contact_name=data.get("contact_name"),
            contact_email=data.get("contact_email"),
            confidence=data["confidence"],
            extraction_time_ms=data["extraction_time_ms"],
            raw_response=data["raw_response"],
        )


@dataclass
class ExtractionResult:
    """Result of extraction attempt (success or failure)."""

    pdf_path: Path
    success: bool
    tender_info: Optional[TenderInfo]
    error: Optional[str]
