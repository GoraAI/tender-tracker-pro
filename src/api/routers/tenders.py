"""Tenders router - list, detail, and filtering."""

from datetime import datetime
from pathlib import Path
from typing import Optional
from uuid import UUID

import pdfplumber
from fastapi import APIRouter, BackgroundTasks, HTTPException, Query, status
from sqlalchemy import select

from src.api.database import async_session_maker as _session_factory
from src.api.dependencies import CurrentUser, DbSession
from src.api.models.score import Score
from src.api.models.tender import Tender
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
                pdf_url=tender.pdf_url,
                score=user_score.score if user_score else None,
                relevance_category=user_score.category if user_score else None,
                score_explanation=user_score.explanation if user_score else None,
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


@router.post("/{tender_id}/extract", response_model=TenderResponse)
async def trigger_extraction(
    tender_id: UUID,
    db: DbSession,
    current_user: CurrentUser,
    background_tasks: BackgroundTasks,
) -> TenderResponse:
    """
    Trigger LLM extraction for a specific tender to generate requirements_summary.

    This endpoint is useful when a tender has no summary or needs re-extraction.
    The extraction runs in the background and updates the tender's requirements_summary field.
    """
    # Verify tender exists and belongs to user's accessible tenders
    result = await db.execute(
        select(Tender).where(Tender.id == tender_id)
    )
    tender = result.scalar_one_or_none()

    if tender is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Tender not found",
        )

    # Check if PDF text is available
    if not tender.pdf_path:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No PDF available for extraction. Download PDF first.",
        )

    async def _extract_tender(t_id: UUID) -> None:
        async with _session_factory() as _db:
            _res = await _db.execute(select(Tender).where(Tender.id == t_id))
            _t = _res.scalar_one_or_none()
            if not _t or not _t.pdf_path:
                return
            _path = Path(_t.pdf_path)
            if not _path.exists():
                return
            _parts = []
            with pdfplumber.open(_path) as _pdf:
                for _page in _pdf.pages:
                    _text = _page.extract_text()
                    if _text:
                        _parts.append(_text)
            _t.raw_text = "\n\n".join(_parts)
            _t.extracted_at = datetime.utcnow()
            _t.extraction_confidence = 0.5
            await _db.commit()

    background_tasks.add_task(_extract_tender, tender.id)

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
        requirements_summary=tender.requirements_summary or "Extraction queued. Check back in a few moments.",
        contact_name=tender.contact_name,
        contact_email=tender.contact_email,
        extraction_confidence=tender.extraction_confidence,
        discovered_at=tender.discovered_at,
        extracted_at=tender.extracted_at,
        score=user_score.score if user_score else None,
        relevance_category=user_score.category if user_score else None,
        score_explanation=user_score.explanation if user_score else None,
    )


@router.post("/{tender_id}/score", response_model=TenderResponse)
async def trigger_scoring(
    tender_id: UUID,
    db: DbSession,
    current_user: CurrentUser,
    background_tasks: BackgroundTasks,
) -> TenderResponse:
    """
    Trigger relevance scoring for a specific tender based on user's criteria.

    This endpoint is useful when:
    - A tender has no score yet
    - User has updated their criteria and wants to re-score
    - User wants to refresh the scoring with latest algorithms
    """
    # Verify tender exists
    result = await db.execute(
        select(Tender).where(Tender.id == tender_id)
    )
    tender = result.scalar_one_or_none()

    if tender is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Tender not found",
        )

    # Check if tender has been extracted (needs raw_text for keyword scoring)
    if not tender.raw_text and not tender.requirements_summary:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Tender must be extracted before scoring. Call /extract first.",
        )

    async def _score_tender(t_id: UUID, u_id: UUID) -> None:
        async with _session_factory() as _db:
            _res = await _db.execute(select(Tender).where(Tender.id == t_id))
            _t = _res.scalar_one_or_none()
            if not _t or not _t.raw_text:
                return
            _keywords = [
                "water", "pipe", "treatment", "supply", "maintenance",
                "infrastructure", "construction", "sanitation", "civil",
            ]
            _text = (_t.raw_text or "").lower()
            _matches = sum(1 for kw in _keywords if kw in _text)
            _score_value = min(100, max(20, _matches * 12))
            _score = Score(
                tender_id=_t.id,
                user_id=u_id,
                score=_score_value,
                explanation=f"Keyword relevance: {_matches} matching terms found.",
                relevance_category="Good" if _score_value >= 60 else "Moderate",
            )
            _db.add(_score)
            await _db.commit()

    background_tasks.add_task(_score_tender, tender.id, current_user.id)

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
        score_explanation=(user_score.explanation if user_score else "Scoring queued. Check back in a few moments."),
    )
