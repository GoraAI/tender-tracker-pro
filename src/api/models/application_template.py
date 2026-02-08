"""ApplicationTemplate SQLAlchemy model."""

import uuid
from datetime import datetime

from sqlalchemy import DateTime, Index, JSON, String, func
from sqlalchemy.types import Uuid
from sqlalchemy.orm import Mapped, mapped_column

from src.api.database import Base


class ApplicationTemplate(Base):
    """Application template model for tender source-specific forms."""

    __tablename__ = "application_templates"
    __table_args__ = (
        Index("idx_application_templates_source", "source_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )

    # Source identifier (e.g., "joburg", "cape-town")
    source_id: Mapped[str] = mapped_column(String(50), nullable=False, unique=True)

    # Template name
    template_name: Mapped[str] = mapped_column(String(200), nullable=False)

    # Fields schema stored as JSON
    # Format: [{name, display_name, type, required, section, description, validation_rules}]
    fields: Mapped[list] = mapped_column(JSON, nullable=False, default=list)

    # Section order stored as JSON array
    # Format: [{section_id, section_name, order}]
    section_order: Mapped[list] = mapped_column(JSON, nullable=False, default=list)

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    def __repr__(self) -> str:
        return f"<ApplicationTemplate {self.source_id}: {self.template_name}>"
