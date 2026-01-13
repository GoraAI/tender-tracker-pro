"""Dashboard router - stats and recent activity."""

from typing import List

from fastapi import APIRouter

from src.api.dependencies import CurrentUser, DbSession
from src.api.schemas import DashboardStats, RecentTender
from src.api.services.tender_service import (
    get_dashboard_stats,
    get_recent_high_priority_tenders,
)

router = APIRouter(prefix="/dashboard", tags=["Dashboard"])


@router.get("/stats", response_model=DashboardStats)
async def get_stats(db: DbSession, current_user: CurrentUser) -> DashboardStats:
    """Get dashboard statistics."""
    stats = await get_dashboard_stats(db, current_user.id)
    return DashboardStats(**stats)


@router.get("/recent", response_model=List[RecentTender])
async def get_recent_tenders(
    db: DbSession, current_user: CurrentUser
) -> List[RecentTender]:
    """Get recent high-priority tenders (top 5 by score)."""
    tenders = await get_recent_high_priority_tenders(db, current_user.id)
    return [RecentTender(**t) for t in tenders]
