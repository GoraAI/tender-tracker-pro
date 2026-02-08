"""DocumentLibrary SQLAlchemy model."""

import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import DateTime, ForeignKey, Index, String, func
from sqlalchemy.types import Uuid
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base

if TYPE_CHECKING:
    from src.api.models.user import User


class DocumentLibrary(Base):
    """Document library model for storing common tender documents."""

    __tablename__ = "document_library"
    __table_args__ = (
        Index("idx_document_library_user", "user_id"),
        Index("idx_document_library_type", "document_type"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )
    user_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    # Document metadata
    document_type: Mapped[str] = mapped_column(
        String(50), nullable=False
    )  # e.g., "company_registration", "tax_clearance", "bbee_certificate"

    # File path (stored locally on Android, only metadata synced to backend)
    file_path: Mapped[str] = mapped_column(String(500), nullable=False)

    # Expiry tracking
    expires_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    # Timestamps
    uploaded_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    # Relationships
    user: Mapped["User"] = relationship("User")

    def __repr__(self) -> str:
        return f"<DocumentLibrary {self.document_type} for User {self.user_id}>"
