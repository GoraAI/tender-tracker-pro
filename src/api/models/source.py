"""Source SQLAlchemy model."""

from datetime import datetime
from typing import TYPE_CHECKING, List

from sqlalchemy import Boolean, DateTime, String, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base

if TYPE_CHECKING:
    from src.api.models.job import Job
    from src.api.models.tender import Tender


class Source(Base):
    """Tender source configuration model."""

    __tablename__ = "sources"

    id: Mapped[str] = mapped_column(String(50), primary_key=True)  # e.g., 'joburg'
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    url: Mapped[str] = mapped_column(String(500), nullable=False)
    config_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    last_scraped_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    # Relationships
    tenders: Mapped[List["Tender"]] = relationship(
        "Tender", back_populates="source", cascade="all, delete-orphan"
    )
    jobs: Mapped[List["Job"]] = relationship(
        "Job", back_populates="source", cascade="all, delete-orphan"
    )

    def __repr__(self) -> str:
        return f"<Source {self.id}: {self.name}>"
