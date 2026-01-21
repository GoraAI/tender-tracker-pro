"""Pydantic schemas for config operations."""

from datetime import datetime
from typing import Optional, Any
from uuid import UUID

from pydantic import BaseModel, ConfigDict


# === Criteria Schemas ===

class ValueRange(BaseModel):
    """Value range for criteria."""
    min: float = 0
    max: Optional[float] = None


class CriteriaBase(BaseModel):
    """Base criteria schema."""
    name: str
    capabilities: list[str] = []
    preferred_sectors: list[str] = []
    preferred_locations: list[str] = []
    value_range: ValueRange = ValueRange()
    keywords: list[str] = []
    weights: dict[str, float] = {}
    is_default: bool = False


class CriteriaCreate(CriteriaBase):
    """Schema for creating criteria."""
    pass


class CriteriaUpdate(BaseModel):
    """Schema for updating criteria."""
    name: Optional[str] = None
    capabilities: Optional[list[str]] = None
    preferred_sectors: Optional[list[str]] = None
    preferred_locations: Optional[list[str]] = None
    value_range: Optional[ValueRange] = None
    keywords: Optional[list[str]] = None
    weights: Optional[dict[str, float]] = None
    is_default: Optional[bool] = None


class CriteriaResponse(CriteriaBase):
    """Criteria response schema."""
    id: UUID
    user_id: UUID
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


# === Source Schemas ===

class SourceResponse(BaseModel):
    """Source response schema."""
    id: str
    name: str
    url: str
    config_path: Optional[str]
    is_active: bool
    last_scraped_at: Optional[datetime]
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class SourceToggle(BaseModel):
    """Schema for toggling source."""
    is_active: Optional[bool] = None


class SourceCreate(BaseModel):
    """Schema for creating a source."""
    id: str
    name: str
    url: str
    config_path: Optional[str] = None
    is_active: bool = True
