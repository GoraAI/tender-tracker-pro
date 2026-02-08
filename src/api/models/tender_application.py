"""TenderApplication SQLAlchemy model."""

import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import DateTime, ForeignKey, Index, JSON, String, Text, func
from sqlalchemy.types import Uuid
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base

if TYPE_CHECKING:
    from src.api.models.tender import Tender
    from src.api.models.user import User


class TenderApplication(Base):
    """Tender application model for auto-filled applications."""

    __tablename__ = "tender_applications"
    __table_args__ = (
        Index("idx_applications_tender", "tender_id"),
        Index("idx_applications_user", "user_id"),
        Index("idx_applications_status", "status"),
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
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default="draft"
    )  # draft, submitted

    # Auto-filled fields stored as JSON
    # Format: {field_name: {value: str, auto_filled: bool, source: str}}
    auto_filled_fields: Mapped[dict] = mapped_column(JSON, nullable=False, default=dict)

    # Pricing data stored as JSON
    # Format: {line_items: [{item_no, description, unit, quantity, unit_cost, total}], grand_total: float}
    pricing_data: Mapped[dict | None] = mapped_column(JSON, nullable=True)

    # To-do items stored as JSON array
    # Format: [{id, category, priority, title, description, section_reference, completed, completed_at}]
    to_do_items: Mapped[list] = mapped_column(JSON, nullable=False, default=list)

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    submitted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    # Relationships
    tender: Mapped["Tender"] = relationship("Tender")
    user: Mapped["User"] = relationship("User")

    def __repr__(self) -> str:
        return f"<TenderApplication {self.id} for Tender {self.tender_id}>"
