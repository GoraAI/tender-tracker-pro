"""Pydantic schemas for Tender Tracker Pro API."""

from src.api.schemas.auth import Token, TokenData, UserCreate, UserLogin
from src.api.schemas.dashboard import (
    DashboardResponse,
    DashboardStats,
    RecentActivity,
    RecentTender,
)
from src.api.schemas.score import ScoreCreate, ScoreResponse
from src.api.schemas.tender import (
    TenderFilters,
    TenderList,
    TenderListItem,
    TenderResponse,
)
from src.api.schemas.user import PasswordChange, UserResponse, UserUpdate

__all__ = [
    # Auth
    "UserCreate",
    "UserLogin",
    "Token",
    "TokenData",
    # User
    "UserResponse",
    "UserUpdate",
    "PasswordChange",
    # Tender
    "TenderResponse",
    "TenderListItem",
    "TenderList",
    "TenderFilters",
    # Score
    "ScoreCreate",
    "ScoreResponse",
    # Dashboard
    "DashboardStats",
    "RecentTender",
    "RecentActivity",
    "DashboardResponse",
]
