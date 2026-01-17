"""Compliance Registration model for tracking user's compliance documents."""

from datetime import date, datetime
from typing import List, Optional
from uuid import uuid4

from sqlalchemy import Date, DateTime, ForeignKey, JSON, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base


class ComplianceRegistration(Base):
    """User's compliance registration (CSD, CIDB, B-BBEE, SARS, UIF, COID)."""

    __tablename__ = "compliance_registrations"

    id: Mapped[UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid4
    )
    user_id: Mapped[UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), nullable=False
    )

    # Registration details
    type: Mapped[str] = mapped_column(String(20), nullable=False)  # csd, cidb, bbbee, sars, uif, coid
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    short_name: Mapped[str] = mapped_column(String(20), nullable=False)
    registration_number: Mapped[str] = mapped_column(String(100), nullable=False)

    # Dates
    issue_date: Mapped[date] = mapped_column(Date, nullable=False)
    expiry_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)
    expiry_type: Mapped[str] = mapped_column(String(20), nullable=False)  # annual, perpetual, continuous

    # Renewal info
    renewal_url: Mapped[Optional[str]] = mapped_column(String(500), nullable=True)

    # Documents (stored as JSON array)
    documents: Mapped[Optional[List[dict]]] = mapped_column(JSON, default=list)

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    # Relationships
    user = relationship("User", back_populates="compliance_registrations")

    @property
    def days_remaining(self) -> Optional[int]:
        """Calculate days until expiry. Returns None for perpetual."""
        if self.expiry_date is None or self.expiry_type == "perpetual":
            return None
        delta = self.expiry_date - date.today()
        return delta.days

    @property
    def is_expired(self) -> bool:
        """Check if registration is expired."""
        if self.expiry_date is None or self.expiry_type == "perpetual":
            return False
        return self.expiry_date < date.today()

    @property
    def is_valid(self) -> bool:
        """Check if registration is currently valid."""
        return not self.is_expired

    def __repr__(self) -> str:
        return f"<ComplianceRegistration {self.type}: {self.registration_number}>"
