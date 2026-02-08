"""API endpoints for tender qualification."""

from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.database import get_db
from src.api.dependencies import get_current_user
from src.api.models.user import User
from src.api.services.qualification_service import QualificationService

router = APIRouter(prefix="/qualification", tags=["qualification"])


# Response models
class QualificationResponse(BaseModel):
    qualifies: bool
    score: int
    reasons: list[str]
    confidence: float
    tender_id: UUID
    user_id: UUID
    checked_at: str

    class Config:
        from_attributes = True


@router.get("/tenders/{tender_id}", response_model=QualificationResponse)
async def check_tender_qualification(
    tender_id: UUID,
    min_score: int = 70,
    min_capabilities: int = 2,
    min_deadline_days: int = 7,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Check if a tender qualifies for auto-fill.

    Query parameters:
    - min_score: Minimum score threshold (default 70)
    - min_capabilities: Minimum capability matches (default 2)
    - min_deadline_days: Minimum days before deadline (default 7)
    """
    service = QualificationService(db)

    try:
        result = await service.evaluate_tender(
            tender_id,
            current_user.id,
            min_score=min_score,
            min_capabilities=min_capabilities,
            min_deadline_days=min_deadline_days,
        )

        return QualificationResponse(
            qualifies=result.qualifies,
            score=result.score,
            reasons=result.reasons,
            confidence=result.confidence,
            tender_id=result.tender_id,
            user_id=result.user_id,
            checked_at=result.checked_at.isoformat(),
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e)
        )


@router.get("/tenders/qualifying", response_model=list[UUID])
async def get_qualifying_tenders(
    source_id: str = None,
    limit: int = 50,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Get list of tender IDs that qualify for auto-fill.

    Query parameters:
    - source_id: Optional filter by source
    - limit: Maximum results (default 50)
    """
    service = QualificationService(db)

    try:
        tenders = await service.get_qualifying_tenders(
            current_user.id, source_id=source_id, limit=limit
        )
        return [tender.id for tender in tenders]
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e)
        )
