"""Tenders router - list, detail, and filtering."""

from datetime import datetime
from typing import Optional
from uuid import UUID

from fastapi import APIRouter, HTTPException, Query, status

from src.api.dependencies import CurrentUser, DbSession
from src.api.schemas import TenderList, TenderListItem, TenderResponse
from src.api.services.tender_service import get_tender_by_id, list_tenders

router = APIRouter(prefix="/tenders", tags=["Tenders"])


@router.get("", response_model=TenderList)
async def get_tenders(
    db: DbSession,
    current_user: CurrentUser,
    page: int = Query(1, ge=1, description="Page number"),
    per_page: int = Query(20, ge=1, le=100, description="Items per page"),
    search: Optional[str] = Query(None, description="Search in title/reference"),
    source_id: Optional[str] = Query(None, description="Filter by source"),
    min_score: Optional[int] = Query(None, ge=0, le=100, description="Minimum score"),
    deadline_from: Optional[datetime] = Query(None, description="Deadline start"),
    deadline_to: Optional[datetime] = Query(None, description="Deadline end"),
    sort_by: str = Query("discovered_at", description="Sort field"),
    sort_order: str = Query("desc", pattern="^(asc|desc)$", description="Sort order"),
) -> TenderList:
    """Get paginated list of tenders with optional filters."""
    tenders, total = await list_tenders(
        db=db,
        user_id=current_user.id,
        page=page,
        per_page=per_page,
        search=search,
        source_id=source_id,
        min_score=min_score,
        deadline_from=deadline_from,
        deadline_to=deadline_to,
        sort_by=sort_by,
        sort_order=sort_order,
    )

    # Calculate total pages
    pages = (total + per_page - 1) // per_page if total > 0 else 0

    # Build response items
    items = []
    for tender in tenders:
        user_score = getattr(tender, "_user_score", None)
        items.append(
            TenderListItem(
                id=tender.id,
                source_id=tender.source_id,
                title=tender.title,
                reference=tender.reference,
                deadline=tender.deadline,
                estimated_value=tender.estimated_value,
                discovered_at=tender.discovered_at,
                score=user_score.score if user_score else None,
                relevance_category=user_score.category if user_score else None,
            )
        )

    return TenderList(
        items=items,
        total=total,
        page=page,
        per_page=per_page,
        pages=pages,
    )


@router.get("/{tender_id}", response_model=TenderResponse)
async def get_tender(
    tender_id: UUID,
    db: DbSession,
    current_user: CurrentUser,
) -> TenderResponse:
    """Get tender details with user's score."""
    tender = await get_tender_by_id(db, tender_id, current_user.id)

    if tender is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Tender not found",
        )

    user_score = getattr(tender, "_user_score", None)

    return TenderResponse(
        id=tender.id,
        source_id=tender.source_id,
        pdf_url=tender.pdf_url,
        pdf_path=tender.pdf_path,
        title=tender.title,
        reference=tender.reference,
        deadline=tender.deadline,
        estimated_value=tender.estimated_value,
        requirements_summary=tender.requirements_summary,
        contact_name=tender.contact_name,
        contact_email=tender.contact_email,
        extraction_confidence=tender.extraction_confidence,
        discovered_at=tender.discovered_at,
        extracted_at=tender.extracted_at,
        score=user_score.score if user_score else None,
        relevance_category=user_score.category if user_score else None,
        score_explanation=user_score.explanation if user_score else None,
    )
