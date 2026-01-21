"""Score schemas."""

from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, computed_field


class ScoreBase(BaseModel):
    """Base score schema."""

    score: int = Field(..., ge=0, le=100)
    explanation: Optional[str] = None


class ScoreCreate(ScoreBase):
    """Schema for creating a score."""

    tender_id: UUID
    criteria_id: Optional[UUID] = None


class ScoreResponse(ScoreBase):
    """Schema for score response."""

    id: UUID
    tender_id: UUID
    user_id: UUID
    criteria_id: Optional[UUID] = None
    scored_at: datetime

    @computed_field
    @property
    def relevance_category(self) -> str:
        """Get relevance category based on score."""
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

    model_config = ConfigDict(from_attributes=True)
