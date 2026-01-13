"""Pipeline router for running tender discovery/extraction pipeline."""

from typing import Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status, BackgroundTasks
from sqlalchemy.ext.asyncio import AsyncSession

from ..database import get_db
from ..dependencies import get_current_user
from ..models.user import User
from ..models.job import Job, JobLog
from ..schemas.pipeline import (
    JobCreate,
    JobResponse,
    JobDetailResponse,
    JobLogResponse,
    PipelineRunRequest,
)
from ..services.pipeline_service import PipelineService

router = APIRouter()


@router.post("/discover", response_model=JobResponse)
async def run_discover(
    request: PipelineRunRequest,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Start PDF discovery job."""
    service = PipelineService(db)
    job = await service.create_job(
        user_id=current_user.id,
        job_type="discover",
        source_id=request.source_id,
    )
    background_tasks.add_task(service.run_discover, job.id)
    return job


@router.post("/download", response_model=JobResponse)
async def run_download(
    request: PipelineRunRequest,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Start PDF download job."""
    service = PipelineService(db)
    job = await service.create_job(
        user_id=current_user.id,
        job_type="download",
        source_id=request.source_id,
    )
    background_tasks.add_task(service.run_download, job.id)
    return job


@router.post("/extract", response_model=JobResponse)
async def run_extract(
    request: PipelineRunRequest,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Start text extraction job."""
    service = PipelineService(db)
    job = await service.create_job(
        user_id=current_user.id,
        job_type="extract",
        source_id=request.source_id,
    )
    background_tasks.add_task(service.run_extract, job.id)
    return job


@router.post("/score", response_model=JobResponse)
async def run_score(
    request: PipelineRunRequest,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Start relevance scoring job."""
    service = PipelineService(db)
    job = await service.create_job(
        user_id=current_user.id,
        job_type="score",
        source_id=request.source_id,
    )
    background_tasks.add_task(service.run_score, job.id)
    return job


@router.post("/full", response_model=JobResponse)
async def run_full_pipeline(
    request: PipelineRunRequest,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Run full pipeline (discover → download → extract → score)."""
    service = PipelineService(db)
    job = await service.create_job(
        user_id=current_user.id,
        job_type="full_pipeline",
        source_id=request.source_id,
    )
    background_tasks.add_task(service.run_full_pipeline, job.id)
    return job


@router.get("/jobs", response_model=list[JobResponse])
async def list_jobs(
    limit: int = 20,
    offset: int = 0,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """List user's pipeline jobs."""
    service = PipelineService(db)
    jobs = await service.get_user_jobs(current_user.id, limit=limit, offset=offset)
    return jobs


@router.get("/jobs/{job_id}", response_model=JobDetailResponse)
async def get_job(
    job_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get job details with logs."""
    service = PipelineService(db)
    job = await service.get_job(job_id, current_user.id)
    if not job:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Job not found",
        )
    logs = await service.get_job_logs(job_id)
    return JobDetailResponse(
        **job.__dict__,
        logs=[JobLogResponse(**log.__dict__) for log in logs],
    )


@router.post("/jobs/{job_id}/cancel", response_model=JobResponse)
async def cancel_job(
    job_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Cancel a running job."""
    service = PipelineService(db)
    job = await service.cancel_job(job_id, current_user.id)
    if not job:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Job not found or cannot be cancelled",
        )
    return job
