"""
Data models for navigator module.

Contains:
- PDFLink: Represents a discovered PDF link with metadata
"""

from dataclasses import dataclass
from datetime import datetime
from typing import Optional


@dataclass
class PDFLink:
    """Represents a discovered PDF link with metadata."""

    url: str  # Full absolute URL to PDF
    link_text: str  # Anchor text (innerText)
    title: Optional[str]  # Link title attribute
    context: str  # Surrounding text (e.g., from parent <tr>)
    discovered_at: datetime  # Discovery timestamp
    source_url: str  # Page URL where found
    source_id: str  # Source identifier (e.g., "jhb-water")

    def to_dict(self) -> dict:
        """Convert to dictionary for JSON serialization."""
        return {
            "url": self.url,
            "link_text": self.link_text,
            "title": self.title,
            "context": self.context,
            "discovered_at": self.discovered_at.isoformat(),
            "source_url": self.source_url,
            "source_id": self.source_id,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "PDFLink":
        """Create PDFLink from dictionary."""
        return cls(
            url=data["url"],
            link_text=data["link_text"],
            title=data.get("title"),
            context=data["context"],
            discovered_at=datetime.fromisoformat(data["discovered_at"]),
            source_url=data["source_url"],
            source_id=data["source_id"],
        )
