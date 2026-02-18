"""Config router for user scoring criteria and sources."""

from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from ..database import get_db
from ..dependencies import get_current_user
from ..models.user import User
from ..models.criteria import UserCriteria
from ..models.source import Source
from ..schemas.config import (
    CriteriaCreate,
    CriteriaUpdate,
    CriteriaResponse,
    SourceCreate,
    SourceResponse,
    SourceToggle,
)

router = APIRouter()


# === Criteria Endpoints ===

@router.get("/criteria", response_model=list[CriteriaResponse])
async def list_criteria(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """List user's scoring criteria."""
    result = await db.execute(
        select(UserCriteria)
        .where(UserCriteria.user_id == current_user.id)
        .order_by(UserCriteria.created_at.desc())
    )
    return result.scalars().all()


@router.get("/criteria/{criteria_id}", response_model=CriteriaResponse)
async def get_criteria(
    criteria_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get a specific criteria set."""
    result = await db.execute(
        select(UserCriteria).where(
            UserCriteria.id == criteria_id,
            UserCriteria.user_id == current_user.id,
        )
    )
    criteria = result.scalar_one_or_none()
    if not criteria:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Criteria not found",
        )
    return criteria


@router.post("/criteria", response_model=CriteriaResponse, status_code=status.HTTP_201_CREATED)
async def create_criteria(
    data: CriteriaCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Create new scoring criteria."""
    # If this is set as default, unset other defaults
    if data.is_default:
        await db.execute(
            update(UserCriteria)
            .where(UserCriteria.user_id == current_user.id)
            .values(is_default=False)
        )

    criteria = UserCriteria(
        user_id=current_user.id,
        name=data.name,
        capabilities=data.capabilities,
        preferred_sectors=data.preferred_sectors,
        preferred_locations=data.preferred_locations,
        value_range=data.value_range.model_dump(),
        keywords=data.keywords,
        weights=data.weights,
        is_default=data.is_default,
    )
    db.add(criteria)
    await db.commit()
    await db.refresh(criteria)
    return criteria


@router.put("/criteria/{criteria_id}", response_model=CriteriaResponse)
async def update_criteria(
    criteria_id: UUID,
    data: CriteriaUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Update scoring criteria."""
    result = await db.execute(
        select(UserCriteria).where(
            UserCriteria.id == criteria_id,
            UserCriteria.user_id == current_user.id,
        )
    )
    criteria = result.scalar_one_or_none()
    if not criteria:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Criteria not found",
        )

    # If setting as default, unset other defaults
    if data.is_default:
        await db.execute(
            update(UserCriteria)
            .where(UserCriteria.user_id == current_user.id, UserCriteria.id != criteria_id)
            .values(is_default=False)
        )

    # Update fields
    update_data = data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(criteria, field, value)

    await db.commit()
    await db.refresh(criteria)
    return criteria


@router.delete("/criteria/{criteria_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_criteria(
    criteria_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Delete scoring criteria."""
    result = await db.execute(
        select(UserCriteria).where(
            UserCriteria.id == criteria_id,
            UserCriteria.user_id == current_user.id,
        )
    )
    criteria = result.scalar_one_or_none()
    if not criteria:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Criteria not found",
        )

    await db.delete(criteria)
    await db.commit()


# === Sources Endpoints ===

@router.get("/sources", response_model=list[SourceResponse])
async def list_sources(
    db: AsyncSession = Depends(get_db),
    _: User = Depends(get_current_user),
):
    """List all tender sources."""
    result = await db.execute(
        select(Source).order_by(Source.name)
    )
    return result.scalars().all()


@router.post("/sources", response_model=SourceResponse, status_code=status.HTTP_201_CREATED)
async def create_source(
    data: SourceCreate,
    db: AsyncSession = Depends(get_db),
    _: User = Depends(get_current_user),
):
    """Create a new tender source."""
    # Check if source already exists
    result = await db.execute(
        select(Source).where(Source.id == data.id)
    )
    existing = result.scalar_one_or_none()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Source with id '{data.id}' already exists",
        )

    source = Source(
        id=data.id,
        name=data.name,
        url=data.url,
        config_path=data.config_path,
        is_active=data.is_active,
    )
    db.add(source)
    await db.commit()
    await db.refresh(source)
    return source


@router.put("/sources/{source_id}/toggle", response_model=SourceResponse)
async def toggle_source(
    source_id: str,
    db: AsyncSession = Depends(get_db),
    _: User = Depends(get_current_user),
):
    """Toggle source active state."""
    result = await db.execute(
        select(Source).where(Source.id == source_id)
    )
    source = result.scalar_one_or_none()
    if not source:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Source not found",
        )

    source.is_active = not source.is_active
    await db.commit()
    await db.refresh(source)
    return source


@router.put("/sources/{source_id}", response_model=SourceResponse)
async def update_source(
    source_id: str,
    data: SourceToggle,
    db: AsyncSession = Depends(get_db),
    _: User = Depends(get_current_user),
):
    """Update source settings."""
    result = await db.execute(
        select(Source).where(Source.id == source_id)
    )
    source = result.scalar_one_or_none()
    if not source:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Source not found",
        )

    if data.is_active is not None:
        source.is_active = data.is_active

    await db.commit()
    await db.refresh(source)
    return source
