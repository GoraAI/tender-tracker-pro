"""Tender Outcome model for tracking win/loss results."""

from datetime import date, datetime
from decimal import Decimal
from typing import Optional
from uuid import uuid4

from sqlalchemy import Date, DateTime, ForeignKey, Integer, Numeric, String, Text, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base


class TenderOutcome(Base):
    """User's tender outcome record (won, lost, withdrawn, cancelled)."""

    __tablename__ = "tender_outcomes"

    id: Mapped[UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid4
    )
    user_id: Mapped[UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), nullable=False
    )
    tender_id: Mapped[UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("tenders.id"), nullable=False
    )

    # Outcome details
    outcome: Mapped[str] = mapped_column(String(20), nullable=False)  # won, lost, withdrawn, cancelled
    reason_code: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)  # price, functionality, bbbee, disqualified, other
    reason_details: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # Award details
    award_date: Mapped[date] = mapped_column(Date, nullable=False)
    award_amount: Mapped[Optional[Decimal]] = mapped_column(Numeric(15, 2), nullable=True)

    # Payment tracking
    payment_received_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)
    _days_to_payment: Mapped[Optional[int]] = mapped_column("days_to_payment", Integer, nullable=True)

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    # Relationships
    user = relationship("User", back_populates="tender_outcomes")
    tender = relationship("Tender", back_populates="outcomes")

    @property
    def days_to_payment(self) -> Optional[int]:
        """Calculate days from award to payment. Uses stored value or calculates."""
        if self._days_to_payment is not None:
            return self._days_to_payment
        if self.payment_received_date and self.award_date:
            delta = self.payment_received_date - self.award_date
            return delta.days
        return None

    @days_to_payment.setter
    def days_to_payment(self, value: Optional[int]):
        self._days_to_payment = value

    def __repr__(self) -> str:
        return f"<TenderOutcome {self.outcome}: {self.tender_id}>"
