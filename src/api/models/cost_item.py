"""Cost Item model for tracking tender-related expenses."""

from datetime import date, datetime
from decimal import Decimal
from typing import Optional
from uuid import uuid4

from sqlalchemy import Boolean, Date, DateTime, ForeignKey, Numeric, String, Text, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base


class CostItem(Base):
    """User's cost item for tracking tender expenses."""

    __tablename__ = "cost_items"

    id: Mapped[UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid4
    )
    user_id: Mapped[UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), nullable=False
    )
    tender_id: Mapped[Optional[UUID]] = mapped_column(
        UUID(as_uuid=True), ForeignKey("tenders.id"), nullable=True
    )

    # Cost details
    category: Mapped[str] = mapped_column(String(50), nullable=False)  # platform, compliance, preparation, submission, opportunity
    description: Mapped[str] = mapped_column(Text, nullable=False)
    amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=False)
    date: Mapped[date] = mapped_column(Date, nullable=False)

    # Recurring cost tracking
    recurring: Mapped[bool] = mapped_column(Boolean, default=False)
    frequency: Mapped[Optional[str]] = mapped_column(String(20), nullable=True)  # annual, monthly, quarterly

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    # Relationships
    user = relationship("User", back_populates="cost_items")
    tender = relationship("Tender", back_populates="cost_items")

    def __repr__(self) -> str:
        return f"<CostItem {self.category}: R{self.amount}>"
