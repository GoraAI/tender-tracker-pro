"""Tender service - list, filter, and retrieve tenders."""

from datetime import datetime, timedelta, timezone
from typing import List, Optional, Tuple
from uuid import UUID

from sqlalchemy import and_, func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import joinedload

from src.api.models import Score, Source, Tender, User


async def get_tender_by_id(
    db: AsyncSession, tender_id: UUID, user_id: Optional[UUID] = None
) -> Optional[Tender]:
    """Get a tender by ID, optionally with user's score."""
    query = select(Tender).where(Tender.id == tender_id)
    result = await db.execute(query)
    tender = result.scalar_one_or_none()

    if tender and user_id:
        # Get user's score for this tender
        score_query = select(Score).where(
            and_(Score.tender_id == tender_id, Score.user_id == user_id)
        )
        score_result = await db.execute(score_query)
        score = score_result.scalar_one_or_none()
        # Attach score to tender object for response
        tender._user_score = score

    return tender


async def list_tenders(
    db: AsyncSession,
    user_id: UUID,
    page: int = 1,
    per_page: int = 20,
    search: Optional[str] = None,
    source_id: Optional[str] = None,
    min_score: Optional[int] = None,
    deadline_from: Optional[datetime] = None,
    deadline_to: Optional[datetime] = None,
    sort_by: str = "discovered_at",
    sort_order: str = "desc",
) -> Tuple[List[Tender], int]:
    """List tenders with pagination and filtering."""
    # Base query
    query = select(Tender)

    # Apply filters
    conditions = []

    if search:
        search_pattern = f"%{search}%"
        conditions.append(
            or_(
                Tender.title.ilike(search_pattern),
                Tender.reference.ilike(search_pattern),
            )
        )

    if source_id:
        conditions.append(Tender.source_id == source_id)

    if deadline_from:
        conditions.append(Tender.deadline >= deadline_from)

    if deadline_to:
        conditions.append(Tender.deadline <= deadline_to)

    if conditions:
        query = query.where(and_(*conditions))

    # Handle score filtering (requires join with scores)
    if min_score is not None:
        query = query.join(Score, and_(
            Score.tender_id == Tender.id,
            Score.user_id == user_id
        )).where(Score.score >= min_score)

    # Get total count
    count_query = select(func.count()).select_from(query.subquery())
    total_result = await db.execute(count_query)
    total = total_result.scalar() or 0

    # Apply sorting
    sort_column = getattr(Tender, sort_by, Tender.discovered_at)
    if sort_order == "desc":
        query = query.order_by(sort_column.desc())
    else:
        query = query.order_by(sort_column.asc())

    # Apply pagination
    offset = (page - 1) * per_page
    query = query.offset(offset).limit(per_page)

    # Execute query
    result = await db.execute(query)
    tenders = list(result.scalars().all())

    # Get scores for these tenders
    if tenders:
        tender_ids = [t.id for t in tenders]
        scores_query = select(Score).where(
            and_(
                Score.tender_id.in_(tender_ids),
                Score.user_id == user_id,
            )
        )
        scores_result = await db.execute(scores_query)
        scores = {s.tender_id: s for s in scores_result.scalars().all()}

        # Attach scores to tenders
        for tender in tenders:
            tender._user_score = scores.get(tender.id)

    return tenders, total


async def get_dashboard_stats(db: AsyncSession, user_id: UUID) -> dict:
    """Get dashboard statistics for a user."""
    # Total tenders
    total_result = await db.execute(select(func.count(Tender.id)))
    total_tenders = total_result.scalar() or 0

    # High priority count (score >= 70)
    high_priority_query = select(func.count(Score.id)).where(
        and_(Score.user_id == user_id, Score.score >= 70)
    )
    high_priority_result = await db.execute(high_priority_query)
    high_priority_count = high_priority_result.scalar() or 0

    # Average score
    avg_score_query = select(func.avg(Score.score)).where(Score.user_id == user_id)
    avg_score_result = await db.execute(avg_score_query)
    avg_score = avg_score_result.scalar()
    if avg_score is not None:
        avg_score = round(float(avg_score), 1)

    # Tenders this week
    week_ago = datetime.now(timezone.utc) - timedelta(days=7)
    week_query = select(func.count(Tender.id)).where(Tender.discovered_at >= week_ago)
    week_result = await db.execute(week_query)
    tenders_this_week = week_result.scalar() or 0

    # Active sources
    sources_query = select(func.count(Source.id)).where(Source.is_active == True)
    sources_result = await db.execute(sources_query)
    sources_active = sources_result.scalar() or 0

    return {
        "total_tenders": total_tenders,
        "high_priority_count": high_priority_count,
        "avg_score": avg_score,
        "tenders_this_week": tenders_this_week,
        "sources_active": sources_active,
    }


async def get_recent_high_priority_tenders(
    db: AsyncSession, user_id: UUID, limit: int = 5
) -> List[dict]:
    """Get recent high-priority tenders for dashboard."""
    query = (
        select(Tender, Score)
        .join(Score, and_(Score.tender_id == Tender.id, Score.user_id == user_id))
        .where(Score.score >= 70)
        .order_by(Score.score.desc(), Tender.discovered_at.desc())
        .limit(limit)
    )

    result = await db.execute(query)
    rows = result.all()

    return [
        {
            "id": tender.id,
            "title": tender.title,
            "reference": tender.reference,
            "source_id": tender.source_id,
            "deadline": tender.deadline,
            "estimated_value": tender.estimated_value,
            "score": score.score,
            "relevance_category": score.category,
            "discovered_at": tender.discovered_at,
        }
        for tender, score in rows
    ]
