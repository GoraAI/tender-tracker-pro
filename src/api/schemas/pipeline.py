"""Pydantic schemas for pipeline operations."""

from datetime import datetime
from typing import Optional, Any
from uuid import UUID

from pydantic import BaseModel


class PipelineRunRequest(BaseModel):
    """Request to run a pipeline step."""
    source_id: Optional[str] = None


class JobCreate(BaseModel):
    """Internal schema for creating a job."""
    user_id: UUID
    job_type: str
    source_id: Optional[str] = None


class JobResponse(BaseModel):
    """Job response schema."""
    id: UUID
    job_type: str
    source_id: Optional[str]
    status: str
    progress: int
    total_items: Optional[int]
    processed_items: int
    error_message: Optional[str]
    started_at: Optional[datetime]
    completed_at: Optional[datetime]
    created_at: datetime

    class Config:
        from_attributes = True


class JobLogResponse(BaseModel):
    """Job log entry response."""
    id: UUID
    job_id: UUID
    level: str
    message: str
    extra_data: Optional[dict[str, Any]]
    created_at: datetime

    class Config:
        from_attributes = True


class JobDetailResponse(JobResponse):
    """Job with logs."""
    logs: list[JobLogResponse] = []


class JobLogCreate(BaseModel):
    """Internal schema for creating a job log."""
    job_id: UUID
    level: str = "info"
    message: str
    extra_data: Optional[dict[str, Any]] = None
