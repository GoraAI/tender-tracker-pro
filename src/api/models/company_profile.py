"""Company Profile model for user scoring criteria."""

from datetime import datetime
from decimal import Decimal
from typing import List, Optional
from uuid import uuid4

from sqlalchemy import DateTime, ForeignKey, JSON, Numeric, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base


class CompanyProfile(Base):
    """Company profile with tender preferences for scoring."""

    __tablename__ = "company_profiles"

    id: Mapped[UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid4
    )
    user_id: Mapped[UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), nullable=False, unique=True
    )

    # Company details
    legal_name: Mapped[str] = mapped_column(String(255), nullable=False)
    trading_name: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    industry: Mapped[str] = mapped_column(String(100), nullable=False)

    # Capabilities and preferences (stored as JSON arrays)
    capabilities: Mapped[Optional[List[str]]] = mapped_column(JSON, default=list)
    preferred_sectors: Mapped[Optional[List[str]]] = mapped_column(JSON, default=list)
    preferred_provinces: Mapped[Optional[List[str]]] = mapped_column(JSON, default=list)
    keywords: Mapped[Optional[List[str]]] = mapped_column(JSON, default=list)

    # Value range for filtering
    value_range_min: Mapped[Optional[Decimal]] = mapped_column(
        Numeric(15, 2), nullable=True
    )
    value_range_max: Mapped[Optional[Decimal]] = mapped_column(
        Numeric(15, 2), nullable=True
    )

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    # Relationships
    user = relationship("User", back_populates="company_profile")

    def __repr__(self) -> str:
        return f"<CompanyProfile {self.legal_name}>"
