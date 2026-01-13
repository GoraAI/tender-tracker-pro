"""UserCriteria SQLAlchemy model."""

import uuid
from datetime import datetime
from typing import TYPE_CHECKING, Any, Dict, List

from sqlalchemy import Boolean, DateTime, ForeignKey, String, UniqueConstraint, func
from sqlalchemy.types import JSON, Uuid
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base

if TYPE_CHECKING:
    from src.api.models.score import Score
    from src.api.models.user import User


class UserCriteria(Base):
    """User scoring criteria model."""

    __tablename__ = "user_criteria"
    __table_args__ = (UniqueConstraint("user_id", "name", name="uq_user_criteria_name"),)

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )
    user_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    capabilities: Mapped[List[str]] = mapped_column(JSON, default=list)
    preferred_sectors: Mapped[List[str]] = mapped_column(JSON, default=list)
    preferred_locations: Mapped[List[str]] = mapped_column(JSON, default=list)
    value_range: Mapped[Dict[str, Any]] = mapped_column(
        JSON, default=lambda: {"min": 0, "max": None}
    )
    keywords: Mapped[List[str]] = mapped_column(JSON, default=list)
    weights: Mapped[Dict[str, float]] = mapped_column(JSON, default=dict)
    is_default: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    # Relationships
    user: Mapped["User"] = relationship("User", back_populates="criteria")
    scores: Mapped[List["Score"]] = relationship(
        "Score", back_populates="criteria", cascade="all, delete-orphan"
    )

    def __repr__(self) -> str:
        return f"<UserCriteria {self.name} for user {self.user_id}>"
