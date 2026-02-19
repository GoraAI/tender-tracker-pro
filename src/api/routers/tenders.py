"""Tenders router - list, detail, and filtering."""

from datetime import datetime
from pathlib import Path
from typing import Optional
from uuid import UUID

import pdfplumber
from fastapi import APIRouter, BackgroundTasks, HTTPException, Query, status
from pydantic import BaseModel, Field
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


class ScoreSubmissionBody(BaseModel):
    """Optional body for submitting a pre-computed score (e.g. from on-device Gemma)."""

    score: int = Field(..., ge=0, le=100)
    explanation: str = ""
    scored_at: Optional[str] = None


@router.post("/{tender_id}/score", response_model=TenderResponse)
async def trigger_scoring(
    tender_id: UUID,
    db: DbSession,
    current_user: CurrentUser,
    background_tasks: BackgroundTasks,
    body: Optional[ScoreSubmissionBody] = None,
) -> TenderResponse:
    """
    Score a tender. Accepts two modes:

    1. **With body** (from Android/Gemma): saves the submitted score directly.
    2. **Without body**: runs server-side keyword scoring using user's criteria.
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

    if body is not None:
        # Mode 1: Accept pre-computed score from Android (Gemma LLM)
        await _upsert_score(db, tender.id, current_user.id, body.score, body.explanation)
    else:
        # Mode 2: Server-side keyword scoring
        if not tender.raw_text and not tender.requirements_summary:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Tender must be extracted before scoring. Call /extract first.",
            )
        background_tasks.add_task(_keyword_score_tender, tender.id, current_user.id)

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
        score=body.score if body else (user_score.score if user_score else None),
        relevance_category=_category_for_score(body.score) if body else (user_score.category if user_score else None),
        score_explanation=body.explanation if body else (user_score.explanation if user_score else "Scoring queued. Check back in a few moments."),
    )


# ---------------------------------------------------------------------------
# Helper functions for scoring
# ---------------------------------------------------------------------------

def _category_for_score(score: int) -> str:
    """Compute relevance category from a numeric score."""
    if score >= 90:
        return "Excellent"
    if score >= 70:
        return "Good"
    if score >= 50:
        return "Moderate"
    if score >= 30:
        return "Weak"
    return "Poor"


def _build_keyword_set(criteria) -> list[str]:
    """Build a combined keyword list from criteria fields."""
    keywords: list[str] = []
    if criteria:
        keywords.extend(criteria.keywords or [])
        keywords.extend(criteria.capabilities or [])
        keywords.extend(criteria.preferred_sectors or [])
    return keywords


async def _upsert_score(
    db, tender_id: UUID, user_id: UUID, score_value: int, explanation: str,
    criteria_id: UUID | None = None,
) -> None:
    """Insert or update a score row."""
    from src.api.models.criteria import UserCriteria

    # If no criteria_id given, look up the user's default
    if criteria_id is None:
        _crit_res = await db.execute(
            select(UserCriteria).where(
                UserCriteria.user_id == user_id,
                UserCriteria.is_default == True,
            )
        )
        _criteria = _crit_res.scalar_one_or_none()
        criteria_id = _criteria.id if _criteria else None

    category = _category_for_score(score_value)

    if criteria_id:
        where_clause = (
            (Score.tender_id == tender_id)
            & (Score.user_id == user_id)
            & (Score.criteria_id == criteria_id)
        )
    else:
        where_clause = (
            (Score.tender_id == tender_id)
            & (Score.user_id == user_id)
            & (Score.criteria_id.is_(None))
        )

    existing = await db.execute(select(Score).where(where_clause))
    existing_score = existing.scalar_one_or_none()

    if existing_score:
        existing_score.score = score_value
        existing_score.explanation = explanation
        existing_score.relevance_category = category
    else:
        db.add(Score(
            tender_id=tender_id,
            user_id=user_id,
            criteria_id=criteria_id,
            score=score_value,
            explanation=explanation,
            relevance_category=category,
        ))
    await db.commit()


async def _keyword_score_tender(t_id: UUID, u_id: UUID) -> None:
    """Background task: score a tender using keyword matching against user criteria."""
    async with _session_factory() as _db:
        _res = await _db.execute(select(Tender).where(Tender.id == t_id))
        _t = _res.scalar_one_or_none()
        if not _t or (not _t.raw_text and not _t.requirements_summary):
            return

        from src.api.models.criteria import UserCriteria

        _crit_res = await _db.execute(
            select(UserCriteria).where(
                UserCriteria.user_id == u_id,
                UserCriteria.is_default == True,
            )
        )
        _criteria = _crit_res.scalar_one_or_none()

        _keywords = _build_keyword_set(_criteria)
        if not _keywords:
            # No criteria configured — can't score meaningfully
            return

        _text = (_t.raw_text or _t.requirements_summary or "").lower()
        _matches = sum(1 for kw in _keywords if kw.lower() in _text)
        _score_value = min(100, max(20, _matches * 12))
        _criteria_id = _criteria.id if _criteria else None
        _explanation = f"Keyword relevance: {_matches}/{len(_keywords)} matching terms found."

        await _upsert_score(_db, _t.id, u_id, _score_value, _explanation, _criteria_id)
