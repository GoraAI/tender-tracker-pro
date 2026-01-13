"""
Data models for tender relevance scoring.
"""

from dataclasses import dataclass

from extraction.models import TenderInfo


@dataclass
class ScoredTender:
    """Tender with relevance score."""

    tender_info: TenderInfo
    score: int  # 0-100
    explanation: str
    scoring_time_ms: int

    def to_dict(self) -> dict:
        """Convert to dictionary for JSON serialization."""
        return {
            "tender_info": self.tender_info.to_dict(),
            "score": self.score,
            "explanation": self.explanation,
            "scoring_time_ms": self.scoring_time_ms,
        }

    @property
    def relevance_category(self) -> str:
        """Get human-readable relevance category."""
        if self.score >= 90:
            return "Excellent"
        elif self.score >= 70:
            return "Good"
        elif self.score >= 50:
            return "Moderate"
        elif self.score >= 30:
            return "Weak"
        else:
            return "Poor"


@dataclass
class ScoringResult:
    """Result of batch scoring operation."""

    scored_tenders: list  # List[ScoredTender]
    total_count: int
    avg_score: float
    min_score: int
    max_score: int
    filtered_count: int  # After applying threshold
