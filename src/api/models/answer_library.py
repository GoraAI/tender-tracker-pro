"""AnswerLibrary SQLAlchemy model."""

import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import DateTime, ForeignKey, Index, String, Text, func
from sqlalchemy.types import Uuid
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base

if TYPE_CHECKING:
    from src.api.models.user import User


class AnswerLibrary(Base):
    """Answer library model for reusable tender responses."""

    __tablename__ = "answer_library"
    __table_args__ = (
        Index("idx_answer_library_user", "user_id"),
        Index("idx_answer_library_category", "category"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )
    user_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    # Answer categorization
    category: Mapped[str] = mapped_column(
        String(100), nullable=False
    )  # e.g., "company_overview", "past_projects", "technical_capabilities"

    # Question and answer
    question: Mapped[str] = mapped_column(String(500), nullable=False)
    answer_text: Mapped[str] = mapped_column(Text, nullable=False)

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    # Relationships
    user: Mapped["User"] = relationship("User")

    def __repr__(self) -> str:
        return f"<AnswerLibrary {self.category}: {self.question[:50]}>"
