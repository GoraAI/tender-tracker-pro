"""Dashboard schemas."""

from datetime import datetime
from decimal import Decimal
from typing import List, Optional
from uuid import UUID

from pydantic import BaseModel


class DashboardStats(BaseModel):
    """Schema for dashboard statistics."""

    total_tenders: int
    high_priority_count: int  # Score >= 70
    avg_score: Optional[float] = None
    tenders_this_week: int
    sources_active: int


class RecentTender(BaseModel):
    """Schema for recent high-priority tender."""

    id: UUID
    title: Optional[str] = None
    reference: Optional[str] = None
    source_id: str
    deadline: Optional[datetime] = None
    estimated_value: Optional[Decimal] = None
    score: int
    relevance_category: str
    discovered_at: datetime

    class Config:
        from_attributes = True


class RecentActivity(BaseModel):
    """Schema for recent pipeline activity."""

    job_id: UUID
    job_type: str
    status: str
    source_id: Optional[str] = None
    processed_items: int
    total_items: Optional[int] = None
    created_at: datetime
    completed_at: Optional[datetime] = None


class DashboardResponse(BaseModel):
    """Schema for full dashboard response."""

    stats: DashboardStats
    recent_tenders: List[RecentTender]
    recent_activity: List[RecentActivity]
