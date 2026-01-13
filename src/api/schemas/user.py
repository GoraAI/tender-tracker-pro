"""User schemas."""

from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, EmailStr, Field


class UserBase(BaseModel):
    """Base user schema."""

    email: EmailStr
    full_name: Optional[str] = None
    company_name: Optional[str] = None


class UserResponse(UserBase):
    """Schema for user response (excludes password)."""

    id: UUID
    is_active: bool
    is_admin: bool
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class UserUpdate(BaseModel):
    """Schema for updating user profile."""

    full_name: Optional[str] = Field(None, max_length=255)
    company_name: Optional[str] = Field(None, max_length=255)


class PasswordChange(BaseModel):
    """Schema for changing password."""

    current_password: str = Field(..., min_length=6)
    new_password: str = Field(..., min_length=6)
