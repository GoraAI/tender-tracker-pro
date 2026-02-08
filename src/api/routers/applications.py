"""API endpoints for tender applications."""

from typing import Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.database import get_db
from src.api.dependencies import get_current_user
from src.api.models.user import User
from src.api.models.tender_application import TenderApplication
from src.api.services.application_assembly_service import ApplicationAssemblyService
from src.api.services.todo_generator_service import ToDoGeneratorService

router = APIRouter(prefix="/applications", tags=["applications"])


# Request/Response models
class ApplicationCreateRequest(BaseModel):
    tender_id: UUID


class ApplicationUpdateRequest(BaseModel):
    field_updates: dict


class PricingUpdateRequest(BaseModel):
    pricing_data: dict


class TodoUpdateRequest(BaseModel):
    todo_id: str
    completed: bool


class ApplicationResponse(BaseModel):
    id: UUID
    tender_id: UUID
    user_id: UUID
    status: str
    auto_filled_fields: dict
    pricing_data: Optional[dict]
    to_do_items: list
    progress: dict
    created_at: str
    submitted_at: Optional[str]

    class Config:
        from_attributes = True


@router.post("", response_model=ApplicationResponse, status_code=status.HTTP_201_CREATED)
async def create_application(
    request: ApplicationCreateRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Create a new draft application with auto-filled fields."""
    service = ApplicationAssemblyService(db)
    todo_service = ToDoGeneratorService(db)

    try:
        # Generate draft application
        application = await service.generate_draft(request.tender_id, current_user.id)

        # Generate to-do items
        todos = await todo_service.generate_todos(application.id, current_user.id)
        application.to_do_items = todos
        await db.commit()
        await db.refresh(application)

        # Calculate progress
        progress = todo_service.calculate_progress(todos)

        return ApplicationResponse(
            id=application.id,
            tender_id=application.tender_id,
            user_id=application.user_id,
            status=application.status,
            auto_filled_fields=application.auto_filled_fields,
            pricing_data=application.pricing_data,
            to_do_items=application.to_do_items,
            progress=progress,
            created_at=application.created_at.isoformat(),
            submitted_at=application.submitted_at.isoformat() if application.submitted_at else None,
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e)
        )


@router.get("/{application_id}", response_model=ApplicationResponse)
async def get_application(
    application_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Get application by ID."""
    from sqlalchemy import select

    result = await db.execute(
        select(TenderApplication).where(
            TenderApplication.id == application_id,
            TenderApplication.user_id == current_user.id,
        )
    )
    application = result.scalar_one_or_none()

    if not application:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Application not found"
        )

    # Calculate progress
    todo_service = ToDoGeneratorService(db)
    progress = todo_service.calculate_progress(application.to_do_items)

    return ApplicationResponse(
        id=application.id,
        tender_id=application.tender_id,
        user_id=application.user_id,
        status=application.status,
        auto_filled_fields=application.auto_filled_fields,
        pricing_data=application.pricing_data,
        to_do_items=application.to_do_items,
        progress=progress,
        created_at=application.created_at.isoformat(),
        submitted_at=application.submitted_at.isoformat() if application.submitted_at else None,
    )


@router.put("/{application_id}/fields", response_model=ApplicationResponse)
async def update_application_fields(
    application_id: UUID,
    request: ApplicationUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Update application fields."""
    service = ApplicationAssemblyService(db)
    todo_service = ToDoGeneratorService(db)

    try:
        application = await service.update_application(
            application_id, current_user.id, request.field_updates
        )
        progress = todo_service.calculate_progress(application.to_do_items)

        return ApplicationResponse(
            id=application.id,
            tender_id=application.tender_id,
            user_id=application.user_id,
            status=application.status,
            auto_filled_fields=application.auto_filled_fields,
            pricing_data=application.pricing_data,
            to_do_items=application.to_do_items,
            progress=progress,
            created_at=application.created_at.isoformat(),
            submitted_at=application.submitted_at.isoformat() if application.submitted_at else None,
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.put("/{application_id}/pricing", response_model=ApplicationResponse)
async def update_pricing(
    application_id: UUID,
    request: PricingUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Update application pricing."""
    service = ApplicationAssemblyService(db)
    todo_service = ToDoGeneratorService(db)

    try:
        application = await service.update_pricing(
            application_id, current_user.id, request.pricing_data
        )
        progress = todo_service.calculate_progress(application.to_do_items)

        return ApplicationResponse(
            id=application.id,
            tender_id=application.tender_id,
            user_id=application.user_id,
            status=application.status,
            auto_filled_fields=application.auto_filled_fields,
            pricing_data=application.pricing_data,
            to_do_items=application.to_do_items,
            progress=progress,
            created_at=application.created_at.isoformat(),
            submitted_at=application.submitted_at.isoformat() if application.submitted_at else None,
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.put("/{application_id}/todos/{todo_id}", response_model=ApplicationResponse)
async def update_todo(
    application_id: UUID,
    todo_id: str,
    request: TodoUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Update to-do item status."""
    todo_service = ToDoGeneratorService(db)

    try:
        application = await todo_service.update_todo_status(
            application_id, current_user.id, request.todo_id, request.completed
        )
        progress = todo_service.calculate_progress(application.to_do_items)

        return ApplicationResponse(
            id=application.id,
            tender_id=application.tender_id,
            user_id=application.user_id,
            status=application.status,
            auto_filled_fields=application.auto_filled_fields,
            pricing_data=application.pricing_data,
            to_do_items=application.to_do_items,
            progress=progress,
            created_at=application.created_at.isoformat(),
            submitted_at=application.submitted_at.isoformat() if application.submitted_at else None,
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.post("/{application_id}/submit", response_model=ApplicationResponse)
async def submit_application(
    application_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Submit application."""
    service = ApplicationAssemblyService(db)
    todo_service = ToDoGeneratorService(db)

    try:
        application = await service.submit_application(application_id, current_user.id)
        progress = todo_service.calculate_progress(application.to_do_items)

        return ApplicationResponse(
            id=application.id,
            tender_id=application.tender_id,
            user_id=application.user_id,
            status=application.status,
            auto_filled_fields=application.auto_filled_fields,
            pricing_data=application.pricing_data,
            to_do_items=application.to_do_items,
            progress=progress,
            created_at=application.created_at.isoformat(),
            submitted_at=application.submitted_at.isoformat() if application.submitted_at else None,
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.delete("/{application_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_application(
    application_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Delete application."""
    from sqlalchemy import delete

    result = await db.execute(
        delete(TenderApplication).where(
            TenderApplication.id == application_id,
            TenderApplication.user_id == current_user.id,
        )
    )

    if result.rowcount == 0:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Application not found"
        )

    await db.commit()
