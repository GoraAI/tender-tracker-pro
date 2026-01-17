"""Analytics schemas for tender visualization skill."""
from datetime import date, datetime
from typing import Optional
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
    period_start: date
    period_end: date


# =============================================================================
# COST TRACKING
# =============================================================================

class CostItemCreate(BaseModel):
    """Create a cost item."""
    category: str = Field(..., description="Cost category: platform, compliance, preparation, submission, opportunity")
    description: str
    amount: float = Field(gt=0)
    date: date = Field(default_factory=date.today)
    tender_id: Optional[str] = None


class CostItem(CostItemCreate):
    """Cost item with ID."""
    id: str
    user_id: str
    created_at: datetime


class CostCategory(BaseModel):
    """Cost category summary."""
    name: str
    amount: float
    percentage: float = Field(ge=0, le=1)
    items: list[CostItem] = []


class CostAnalytics(BaseModel):
    """Cost analytics summary."""
    categories: list[CostCategory]
    total_spent: float
    tenders_won: int
    cost_per_win: float
    period_start: date
    period_end: date


# =============================================================================
# COMPLIANCE STATUS
# =============================================================================

class ComplianceItemCreate(BaseModel):
    """Create/update a compliance item."""
    registration_type: str = Field(..., description="Type: csd, cidb, bbbee, sars, uif, coid")
    name: str
    short_name: str
    expiry_date: Optional[date] = None
    renewal_url: Optional[str] = None
    notes: Optional[str] = None


class ComplianceItem(ComplianceItemCreate):
    """Compliance item with computed fields."""
    id: str
    user_id: str
    days_remaining: int
    is_valid: bool
    created_at: datetime
    updated_at: datetime


class ComplianceStatus(BaseModel):
    """Overall compliance status."""
    items: list[ComplianceItem]
    valid_count: int
    total_count: int
    days_to_next_expiry: int
    next_expiry_item: str


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


class PaymentDelayAnalytics(BaseModel):
    """Payment delay analytics."""
    by_province: list[PaymentDelayData]
    by_department: list[PaymentDelayData]
    overall_average: int


# =============================================================================
# PIPELINE
# =============================================================================

class PipelinePhase(BaseModel):
    """Phase in tender preparation."""
    name: str
    start_date: date
    end_date: date
    is_complete: bool = False


class PipelineTenderCreate(BaseModel):
    """Add tender to pipeline."""
    tender_id: str
    status: str = "considering"  # considering, preparing, reviewing, ready_to_submit, submitted, awarded, lost


class PipelineTender(BaseModel):
    """Tender in user's pipeline."""
    id: str
    tender_id: str
    user_id: str
    title: str
    reference: str
    deadline: date
    status: str
    start_date: date
    phases: list[PipelinePhase] = []
    has_conflict: bool = False
    conflict_message: str = ""


class PipelineStatus(BaseModel):
    """Pipeline status summary."""
    tenders: list[PipelineTender]
    active_count: int
    due_this_week: int
    has_conflicts: bool


# =============================================================================
# TENDER OUTCOMES
# =============================================================================

class TenderOutcomeCreate(BaseModel):
    """Record tender outcome."""
    tender_id: str
    outcome: str = Field(..., description="won, lost, withdrawn, cancelled")
    reason_code: Optional[str] = None
    reason_details: Optional[str] = None
    award_date: Optional[date] = None
    award_amount: Optional[float] = None


class TenderOutcome(TenderOutcomeCreate):
    """Tender outcome with metadata."""
    id: str
    user_id: str
    created_at: datetime


class OutcomeAnalytics(BaseModel):
    """Outcome analytics."""
    total_submitted: int
    total_won: int
    total_lost: int
    win_rate: float
    outcomes_by_reason: dict[str, int]
    period_start: date
    period_end: date
