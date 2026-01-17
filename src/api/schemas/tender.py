"""Tender schemas."""

from datetime import datetime
from decimal import Decimal
from typing import List, Optional
from uuid import UUID

from pydantic import BaseModel, Field


class TenderBase(BaseModel):
    """Base tender schema."""

    title: Optional[str] = None
    reference: Optional[str] = None
    deadline: Optional[datetime] = None
    estimated_value: Optional[Decimal] = None
    requirements_summary: Optional[str] = None
    contact_name: Optional[str] = None
    contact_email: Optional[str] = None


class TenderResponse(TenderBase):
    """Schema for tender response."""

    id: UUID
    source_id: str
    pdf_url: str
    pdf_path: Optional[str] = None
    extraction_confidence: Optional[float] = None
    discovered_at: datetime
    extracted_at: Optional[datetime] = None

    # Score info (if available for current user)
    score: Optional[int] = None
    relevance_category: Optional[str] = None
    score_explanation: Optional[str] = None

    class Config:
        from_attributes = True


class TenderListItem(BaseModel):
    """Schema for tender in list view (lighter than full response)."""

    id: UUID
    source_id: str
    title: Optional[str] = None
    reference: Optional[str] = None
    deadline: Optional[datetime] = None
    estimated_value: Optional[Decimal] = None
    discovered_at: datetime
    pdf_url: Optional[str] = None
    score: Optional[int] = None
    relevance_category: Optional[str] = None

    class Config:
        from_attributes = True


class TenderList(BaseModel):
    """Schema for paginated tender list."""

    items: List[TenderListItem]
    total: int
    page: int
    per_page: int
    pages: int


class TenderFilters(BaseModel):
    """Schema for tender filtering options."""

    search: Optional[str] = Field(None, description="Search in title/reference")
    source_id: Optional[str] = Field(None, description="Filter by source")
    min_score: Optional[int] = Field(None, ge=0, le=100, description="Minimum score")
    deadline_from: Optional[datetime] = Field(None, description="Deadline start")
    deadline_to: Optional[datetime] = Field(None, description="Deadline end")
    sort_by: str = Field("discovered_at", description="Sort field")
    sort_order: str = Field("desc", pattern="^(asc|desc)$", description="Sort order")
