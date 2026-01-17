"""Pipeline Tender model for tracking user's active tender pipeline."""

from datetime import date, datetime
from typing import List, Optional
from uuid import uuid4

from sqlalchemy import Boolean, Date, DateTime, ForeignKey, JSON, String, Text, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base


class PipelineTender(Base):
    """User's pipeline tender for tracking active bid progress."""

    __tablename__ = "pipeline_tenders"

    id: Mapped[UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid4
    )
    user_id: Mapped[UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), nullable=False
    )
    tender_id: Mapped[UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("tenders.id"), nullable=False
    )

    # Pipeline status
    status: Mapped[str] = mapped_column(String(20), nullable=False)  # discovered, qualified, preparation, review, submission, submitted
    start_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)

    # Phases (stored as JSON array)
    phases: Mapped[Optional[List[dict]]] = mapped_column(JSON, default=list)

    # Assignment
    assigned_to: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # Conflict detection
    has_conflict: Mapped[bool] = mapped_column(Boolean, default=False)

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    # Relationships
    user = relationship("User", back_populates="pipeline_tenders")
    tender = relationship("Tender", back_populates="pipeline_entries")

    def __repr__(self) -> str:
        return f"<PipelineTender {self.status}: {self.tender_id}>"
