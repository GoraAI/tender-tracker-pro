"""User SQLAlchemy model."""

import uuid
from datetime import datetime
from typing import TYPE_CHECKING, List

from sqlalchemy import Boolean, DateTime, String, func
from sqlalchemy.types import Uuid
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.api.database import Base

if TYPE_CHECKING:
    from src.api.models.criteria import UserCriteria
    from src.api.models.job import Job
    from src.api.models.score import Score
    from src.api.models.company_profile import CompanyProfile
    from src.api.models.compliance_registration import ComplianceRegistration
    from src.api.models.cost_item import CostItem
    from src.api.models.tender_outcome import TenderOutcome
    from src.api.models.pipeline_tender import PipelineTender


class User(Base):
    """User account model."""

    __tablename__ = "users"

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )
    email: Mapped[str] = mapped_column(String(255), unique=True, nullable=False, index=True)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    full_name: Mapped[str | None] = mapped_column(String(255), nullable=True)
    company_name: Mapped[str | None] = mapped_column(String(255), nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    is_admin: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    # Relationships
    criteria: Mapped[List["UserCriteria"]] = relationship(
        "UserCriteria", back_populates="user", cascade="all, delete-orphan"
    )
    scores: Mapped[List["Score"]] = relationship(
        "Score", back_populates="user", cascade="all, delete-orphan"
    )
    jobs: Mapped[List["Job"]] = relationship(
        "Job", back_populates="user", cascade="all, delete-orphan"
    )
    # Analytics relationships
    company_profile: Mapped["CompanyProfile"] = relationship(
        "CompanyProfile", back_populates="user", uselist=False, cascade="all, delete-orphan"
    )
    compliance_registrations: Mapped[List["ComplianceRegistration"]] = relationship(
        "ComplianceRegistration", back_populates="user", cascade="all, delete-orphan"
    )
    cost_items: Mapped[List["CostItem"]] = relationship(
        "CostItem", back_populates="user", cascade="all, delete-orphan"
    )
    tender_outcomes: Mapped[List["TenderOutcome"]] = relationship(
        "TenderOutcome", back_populates="user", cascade="all, delete-orphan"
    )
    pipeline_tenders: Mapped[List["PipelineTender"]] = relationship(
        "PipelineTender", back_populates="user", cascade="all, delete-orphan"
    )

    def __repr__(self) -> str:
        return f"<User {self.email}>"
