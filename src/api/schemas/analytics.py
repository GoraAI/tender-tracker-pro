"""Analytics schemas for tender visualization skill.

Updated to use UUID types and support empty state for new users.
NO MOCK DATA - real database queries only.
"""
from datetime import date as date_type, datetime
from typing import Optional
from uuid import UUID
from pydantic import BaseModel, Field


# =============================================================================
# FUNNEL ANALYTICS
# =============================================================================

class FunnelStage(BaseModel):
    """Stage in the opportunity funnel."""
    name: str
    count: int
    drop_off_count: int = 0
    drop_off_reason: str = ""


class FunnelAnalytics(BaseModel):
    """Opportunity funnel analytics."""
    stages: list[FunnelStage]
    win_rate: float = Field(ge=0, le=1)
    cost_per_win: float = 0.0
    avg_days_to_award: int = 0
    period_start: date_type
    period_end: date_type
    is_empty: bool = False  # True when user has no data


# =============================================================================
# COST TRACKING
# =============================================================================

class CostItemCreate(BaseModel):
    """Create a cost item."""
    category: str = Field(..., description="Cost category: platform, compliance, preparation, submission, opportunity")
    description: str
    amount: float = Field(gt=0)
    date: date_type = Field(default_factory=date_type.today)
    tender_id: Optional[UUID] = None
    recurring: bool = False
    frequency: Optional[str] = None  # annual, monthly, quarterly


class CostItemResponse(BaseModel):
    """Cost item response."""
    id: UUID
    user_id: UUID
    tender_id: Optional[UUID] = None
    category: str
    description: str
    amount: float
    date: date_type
    recurring: bool
    frequency: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True


class CostCategory(BaseModel):
    """Cost category summary."""
    name: str
    amount: float
    percentage: float = Field(ge=0, le=1)
    items: list[CostItemResponse] = []


class CostAnalytics(BaseModel):
    """Cost analytics summary."""
    categories: list[CostCategory]
    total_spent: float
    tenders_won: int
    cost_per_win: float
    period_start: date_type
    period_end: date_type
    is_empty: bool = False


# =============================================================================
# COMPLIANCE STATUS
# =============================================================================

class ComplianceItemCreate(BaseModel):
    """Create/update a compliance item."""
    type: str = Field(..., description="Type: csd, cidb, bbbee, sars, uif, coid")
    name: str
    short_name: str
    registration_number: str
    issue_date: date_type
    expiry_date: Optional[date_type] = None
    expiry_type: str = "annual"  # annual, perpetual, continuous
    renewal_url: Optional[str] = None
    documents: Optional[list[dict]] = None


class ComplianceItemResponse(BaseModel):
    """Compliance item response with computed fields."""
    id: UUID
    user_id: UUID
    type: str
    name: str
    short_name: str
    registration_number: str
    issue_date: date_type
    expiry_date: Optional[date_type] = None
    expiry_type: str
    renewal_url: Optional[str] = None
    documents: Optional[list[dict]] = None
    days_remaining: Optional[int]  # Computed - None for perpetual
    is_expired: bool
    is_valid: bool
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class ComplianceStatus(BaseModel):
    """Overall compliance status."""
    items: list[ComplianceItemResponse]
    valid_count: int
    total_count: int
    days_to_next_expiry: Optional[int] = None
    next_expiry_item: Optional[str] = None
    is_empty: bool = False


# =============================================================================
# PAYMENT DELAY
# =============================================================================

class PaymentDelayData(BaseModel):
    """Payment delay data for a region/department."""
    id: str
    name: str
    short_name: str
    avg_days_to_payment: int
    sample_size: int = 0
    trend: float = 0.0  # Positive = getting worse
    source: str = "reference"  # "reference" or "user" to indicate data source


class PaymentDelayAnalytics(BaseModel):
    """Payment delay analytics."""
    by_province: list[PaymentDelayData]
    by_department: list[PaymentDelayData]
    overall_average: int
    user_data_available: bool = False  # True when user has payment data
    reference_data_source: str = "National Treasury Reports 2024-2025"


# =============================================================================
# PIPELINE
# =============================================================================

class PipelinePhase(BaseModel):
    """Phase in tender preparation."""
    name: str
    start_date: date_type
    end_date: Optional[date_type] = None
    is_complete: bool = False


class PipelineTenderCreate(BaseModel):
    """Add tender to pipeline."""
    tender_id: UUID
    status: str = "discovered"  # discovered, qualified, preparation, review, submission, submitted
    assigned_to: Optional[str] = None
    notes: Optional[str] = None


class PipelineTenderUpdate(BaseModel):
    """Update pipeline tender."""
    status: Optional[str] = None
    assigned_to: Optional[str] = None
    notes: Optional[str] = None
    phases: Optional[list[dict]] = None
    has_conflict: Optional[bool] = None


class PipelineTenderResponse(BaseModel):
    """Tender in user's pipeline."""
    id: UUID
    user_id: UUID
    tender_id: UUID
    # Tender details (joined from tender table)
    title: Optional[str] = None
    reference: Optional[str] = None
    deadline: Optional[datetime] = None
    # Pipeline fields
    status: str
    start_date: Optional[date_type] = None
    phases: Optional[list[dict]] = None
    assigned_to: Optional[str] = None
    notes: Optional[str] = None
    has_conflict: bool = False
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class PipelineStatus(BaseModel):
    """Pipeline status summary."""
    tenders: list[PipelineTenderResponse]
    active_count: int
    due_this_week: int
    has_conflicts: bool
    is_empty: bool = False


# =============================================================================
# TENDER OUTCOMES
# =============================================================================

class TenderOutcomeCreate(BaseModel):
    """Record tender outcome."""
    tender_id: UUID
    outcome: str = Field(..., description="won, lost, withdrawn, cancelled")
    reason_code: Optional[str] = None  # price, functionality, bbbee, disqualified, other
    reason_details: Optional[str] = None
    award_date: date_type
    award_amount: Optional[float] = None
    payment_received_date: Optional[date_type] = None


class TenderOutcomeResponse(BaseModel):
    """Tender outcome response."""
    id: UUID
    user_id: UUID
    tender_id: UUID
    outcome: str
    reason_code: Optional[str] = None
    reason_details: Optional[str] = None
    award_date: date_type
    award_amount: Optional[float] = None
    payment_received_date: Optional[date_type] = None
    days_to_payment: Optional[int] = None
    created_at: datetime

    class Config:
        from_attributes = True


class OutcomeAnalytics(BaseModel):
    """Outcome analytics."""
    total_submitted: int
    total_won: int
    total_lost: int
    total_withdrawn: int = 0
    total_cancelled: int = 0
    win_rate: float
    total_award_amount: float = 0.0
    avg_days_to_payment: Optional[int] = None
    outcomes_by_reason: dict[str, int]
    period_start: date_type
    period_end: date_type
    is_empty: bool = False


# =============================================================================
# COMPANY PROFILE
# =============================================================================

class CompanyProfileCreate(BaseModel):
    """Create/update company profile."""
    legal_name: str
    trading_name: Optional[str] = None
    industry: str
    capabilities: Optional[list[str]] = None
    preferred_sectors: Optional[list[str]] = None
    preferred_provinces: Optional[list[str]] = None
    keywords: Optional[list[str]] = None
    value_range_min: Optional[float] = None
    value_range_max: Optional[float] = None


class CompanyProfileResponse(BaseModel):
    """Company profile response."""
    id: UUID
    user_id: UUID
    legal_name: str
    trading_name: Optional[str] = None
    industry: str
    capabilities: Optional[list[str]] = None
    preferred_sectors: Optional[list[str]] = None
    preferred_provinces: Optional[list[str]] = None
    keywords: Optional[list[str]] = None
    value_range_min: Optional[float] = None
    value_range_max: Optional[float] = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
