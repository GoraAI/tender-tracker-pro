"""Tender SQLAlchemy model."""

import uuid
from datetime import datetime
from decimal import Decimal
from typing import TYPE_CHECKING, List

from sqlalchemy import DateTime, Float, ForeignKey, Index, Numeric, String, Text, UniqueConstraint, func
from sqlalchemy.types import Uuid
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base

if TYPE_CHECKING:
    from src.api.models.score import Score
    from src.api.models.source import Source


class Tender(Base):
    """Discovered tender model."""

    __tablename__ = "tenders"
    __table_args__ = (
        UniqueConstraint("source_id", "reference", name="uq_tender_source_reference"),
        Index("idx_tenders_source", "source_id"),
        Index("idx_tenders_deadline", "deadline"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )
    source_id: Mapped[str] = mapped_column(
        String(50), ForeignKey("sources.id", ondelete="CASCADE"), nullable=False
    )
    pdf_url: Mapped[str] = mapped_column(String(1000), nullable=False)
    pdf_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    title: Mapped[str | None] = mapped_column(String(500), nullable=True)
    reference: Mapped[str | None] = mapped_column(String(255), nullable=True)
    deadline: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    estimated_value: Mapped[Decimal | None] = mapped_column(
        Numeric(15, 2), nullable=True
    )
    requirements_summary: Mapped[str | None] = mapped_column(Text, nullable=True)
    contact_name: Mapped[str | None] = mapped_column(String(255), nullable=True)
    contact_email: Mapped[str | None] = mapped_column(String(255), nullable=True)
    extraction_confidence: Mapped[float | None] = mapped_column(Float, nullable=True)
    raw_text: Mapped[str | None] = mapped_column(Text, nullable=True)
    discovered_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    extracted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    # Relationships
    source: Mapped["Source"] = relationship("Source", back_populates="tenders")
    scores: Mapped[List["Score"]] = relationship(
        "Score", back_populates="tender", cascade="all, delete-orphan"
    )

    def __repr__(self) -> str:
        return f"<Tender {self.reference or self.id}>"
