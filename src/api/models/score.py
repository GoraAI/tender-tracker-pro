"""Score SQLAlchemy model."""

import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String, Text, UniqueConstraint, func
from sqlalchemy.types import Uuid
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base

if TYPE_CHECKING:
    from src.api.models.criteria import UserCriteria
    from src.api.models.tender import Tender
    from src.api.models.user import User


class Score(Base):
    """User-specific tender score model."""

    __tablename__ = "scores"
    __table_args__ = (
        UniqueConstraint(
            "tender_id", "user_id", "criteria_id", name="uq_score_tender_user_criteria"
        ),
        Index("idx_scores_user", "user_id"),
        Index("idx_scores_tender", "tender_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )
    tender_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("tenders.id", ondelete="CASCADE"), nullable=False
    )
    user_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    criteria_id: Mapped[uuid.UUID | None] = mapped_column(
        Uuid, ForeignKey("user_criteria.id", ondelete="SET NULL"), nullable=True
    )
    score: Mapped[int] = mapped_column(Integer, nullable=False)
    explanation: Mapped[str | None] = mapped_column(Text, nullable=True)
    relevance_category: Mapped[str | None] = mapped_column(String(20), nullable=True)
    scored_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    # Relationships
    tender: Mapped["Tender"] = relationship("Tender", back_populates="scores")
    user: Mapped["User"] = relationship("User", back_populates="scores")
    criteria: Mapped["UserCriteria | None"] = relationship(
        "UserCriteria", back_populates="scores"
    )

    @property
    def category(self) -> str:
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

    def __repr__(self) -> str:
        return f"<Score {self.score} for tender {self.tender_id}>"
