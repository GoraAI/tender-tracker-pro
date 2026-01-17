"""Analytics API endpoints for tender visualization skill."""
from datetime import date, timedelta
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession

from ..database import get_db
from ..schemas.analytics import (
    FunnelAnalytics,
    FunnelStage,
    CostAnalytics,
    CostCategory,
    CostItemCreate,
    CostItem,
    ComplianceStatus,
    ComplianceItem,
    ComplianceItemCreate,
    PaymentDelayAnalytics,
    PaymentDelayData,
    PipelineStatus,
    PipelineTender,
    PipelineTenderCreate,
    TenderOutcome,
    TenderOutcomeCreate,
    OutcomeAnalytics,
)

router = APIRouter(prefix="/analytics", tags=["analytics"])


# =============================================================================
# FUNNEL ANALYTICS
# =============================================================================

@router.get("/funnel", response_model=FunnelAnalytics)
async def get_funnel_analytics(
    period_months: int = Query(12, ge=1, le=24),
    db: AsyncSession = Depends(get_db)
):
    """
    Get opportunity funnel analytics.

    Shows conversion rates through: Discovered -> Qualified -> Submitted -> Awarded
    """
    period_end = date.today()
    period_start = period_end - timedelta(days=period_months * 30)

    # TODO: In production, aggregate from tender status history
    # For now, return representative mock data based on research
    discovered = 45
    qualified = 28
    submitted = 18
    awarded = 6

    stages = [
        FunnelStage(
            name="Discovered",
            count=discovered,
            drop_off_count=discovered - qualified,
            drop_off_reason="filtered out"
        ),
        FunnelStage(
            name="Qualified",
            count=qualified,
            drop_off_count=qualified - submitted,
            drop_off_reason="no-bid decision"
        ),
        FunnelStage(
            name="Submitted",
            count=submitted,
            drop_off_count=submitted - awarded,
            drop_off_reason="unsuccessful"
        ),
        FunnelStage(
            name="Awarded",
            count=awarded,
            drop_off_count=0
        )
    ]

    win_rate = awarded / submitted if submitted > 0 else 0

    return FunnelAnalytics(
        stages=stages,
        win_rate=win_rate,
        cost_per_win=42500.0,  # R42,500 average
        avg_days_to_award=47,
        period_start=period_start,
        period_end=period_end
    )


# =============================================================================
# COST TRACKING
# =============================================================================

@router.get("/costs", response_model=CostAnalytics)
async def get_cost_analytics(
    period_months: int = Query(12, ge=1, le=24),
    db: AsyncSession = Depends(get_db)
):
    """
    Get cost-per-successful-bid analytics.

    Breaks down costs by category: platform, compliance, preparation, submission, opportunity.
    """
    period_end = date.today()
    period_start = period_end - timedelta(days=period_months * 30)

    # TODO: In production, aggregate from cost_items table
    # Using research-based estimates
    categories = [
        CostCategory(
            name="platform",
            amount=17100.0,
            percentage=0.09,
            items=[]
        ),
        CostCategory(
            name="compliance",
            amount=24400.0,
            percentage=0.13,
            items=[]
        ),
        CostCategory(
            name="preparation",
            amount=98000.0,
            percentage=0.52,
            items=[]
        ),
        CostCategory(
            name="submission",
            amount=18000.0,
            percentage=0.10,
            items=[]
        ),
        CostCategory(
            name="opportunity",
            amount=30000.0,
            percentage=0.16,
            items=[]
        )
    ]

    total_spent = sum(c.amount for c in categories)
    tenders_won = 5
    cost_per_win = total_spent / tenders_won if tenders_won > 0 else 0

    return CostAnalytics(
        categories=categories,
        total_spent=total_spent,
        tenders_won=tenders_won,
        cost_per_win=cost_per_win,
        period_start=period_start,
        period_end=period_end
    )


@router.post("/costs", response_model=CostItem)
async def add_cost_item(
    item: CostItemCreate,
    db: AsyncSession = Depends(get_db)
):
    """Add a cost item for tracking."""
    # TODO: Persist to database
    return CostItem(
        id="cost_001",
        user_id="user_001",
        category=item.category,
        description=item.description,
        amount=item.amount,
        date=item.date,
        tender_id=item.tender_id,
        created_at=date.today()
    )


# =============================================================================
# COMPLIANCE STATUS
# =============================================================================

@router.get("/compliance", response_model=ComplianceStatus)
async def get_compliance_status(
    db: AsyncSession = Depends(get_db)
):
    """
    Get user's compliance registration status.

    Tracks: CSD, CIDB, B-BBEE, SARS, UIF, COID registrations.
    """
    today = date.today()

    # TODO: In production, load from user's compliance_items table
    items = [
        ComplianceItem(
            id="csd",
            user_id="user_001",
            registration_type="csd",
            name="Central Supplier Database",
            short_name="CSD",
            expiry_date=None,  # CSD doesn't expire
            renewal_url="https://secure.csd.gov.za",
            days_remaining=180,
            is_valid=True,
            created_at=today,
            updated_at=today
        ),
        ComplianceItem(
            id="cidb",
            user_id="user_001",
            registration_type="cidb",
            name="CIDB Registration",
            short_name="CIDB",
            expiry_date=today + timedelta(days=45),
            renewal_url="https://www.cidb.org.za",
            days_remaining=45,
            is_valid=True,
            created_at=today,
            updated_at=today
        ),
        ComplianceItem(
            id="bbbee",
            user_id="user_001",
            registration_type="bbbee",
            name="B-BBEE Certificate",
            short_name="B-BBEE",
            expiry_date=today + timedelta(days=12),
            renewal_url=None,
            days_remaining=12,
            is_valid=True,
            created_at=today,
            updated_at=today
        ),
        ComplianceItem(
            id="sars",
            user_id="user_001",
            registration_type="sars",
            name="Tax Clearance (SARS)",
            short_name="SARS",
            expiry_date=None,  # Continuous
            renewal_url="https://www.sarsefiling.co.za",
            days_remaining=120,
            is_valid=True,
            created_at=today,
            updated_at=today
        ),
        ComplianceItem(
            id="uif",
            user_id="user_001",
            registration_type="uif",
            name="UIF Letter of Good Standing",
            short_name="UIF",
            expiry_date=today + timedelta(days=98),
            renewal_url="https://www.ufiling.co.za",
            days_remaining=98,
            is_valid=True,
            created_at=today,
            updated_at=today
        ),
        ComplianceItem(
            id="coid",
            user_id="user_001",
            registration_type="coid",
            name="COID Letter of Good Standing",
            short_name="COID",
            expiry_date=today + timedelta(days=156),
            renewal_url=None,
            days_remaining=156,
            is_valid=True,
            created_at=today,
            updated_at=today
        )
    ]

    valid_items = [i for i in items if i.is_valid and i.days_remaining > 0]
    next_expiry = min(items, key=lambda x: x.days_remaining if x.days_remaining > 0 else 999)

    return ComplianceStatus(
        items=items,
        valid_count=len(valid_items),
        total_count=len(items),
        days_to_next_expiry=next_expiry.days_remaining,
        next_expiry_item=next_expiry.short_name
    )


@router.put("/compliance/{item_id}", response_model=ComplianceItem)
async def update_compliance_item(
    item_id: str,
    item: ComplianceItemCreate,
    db: AsyncSession = Depends(get_db)
):
    """Update a compliance registration item."""
    today = date.today()
    days_remaining = (item.expiry_date - today).days if item.expiry_date else 180

    return ComplianceItem(
        id=item_id,
        user_id="user_001",
        registration_type=item.registration_type,
        name=item.name,
        short_name=item.short_name,
        expiry_date=item.expiry_date,
        renewal_url=item.renewal_url,
        days_remaining=days_remaining,
        is_valid=days_remaining > 0,
        created_at=today,
        updated_at=today
    )


# =============================================================================
# PAYMENT DELAY
# =============================================================================

@router.get("/payment-delays", response_model=PaymentDelayAnalytics)
async def get_payment_delay_analytics(
    db: AsyncSession = Depends(get_db)
):
    """
    Get payment delay analytics by province and department.

    Based on research: North West >100 days, Western Cape ~29 days, etc.
    """
    # Research-based data
    by_province = [
        PaymentDelayData(id="GP", name="Gauteng", short_name="GP", avg_days_to_payment=34, sample_size=45, trend=-2),
        PaymentDelayData(id="WC", name="Western Cape", short_name="WC", avg_days_to_payment=29, sample_size=38, trend=-5),
        PaymentDelayData(id="KZN", name="KwaZulu-Natal", short_name="KZN", avg_days_to_payment=38, sample_size=52, trend=3),
        PaymentDelayData(id="EC", name="Eastern Cape", short_name="EC", avg_days_to_payment=71, sample_size=23, trend=8),
        PaymentDelayData(id="FS", name="Free State", short_name="FS", avg_days_to_payment=67, sample_size=19, trend=5),
        PaymentDelayData(id="NW", name="North West", short_name="NW", avg_days_to_payment=98, sample_size=15, trend=12),
        PaymentDelayData(id="LP", name="Limpopo", short_name="LP", avg_days_to_payment=52, sample_size=28, trend=0),
        PaymentDelayData(id="MP", name="Mpumalanga", short_name="MP", avg_days_to_payment=45, sample_size=31, trend=-3),
        PaymentDelayData(id="NC", name="Northern Cape", short_name="NC", avg_days_to_payment=41, sample_size=12, trend=2),
    ]

    by_department = [
        PaymentDelayData(id="DOH", name="Dept of Health", short_name="DOH", avg_days_to_payment=42, sample_size=67),
        PaymentDelayData(id="DOE", name="Dept of Education", short_name="DOE", avg_days_to_payment=38, sample_size=89),
        PaymentDelayData(id="DPW", name="Dept Public Works", short_name="DPW", avg_days_to_payment=55, sample_size=45),
        PaymentDelayData(id="DOT", name="Dept of Transport", short_name="DOT", avg_days_to_payment=48, sample_size=34),
        PaymentDelayData(id="DOD", name="Dept of Defence", short_name="DOD", avg_days_to_payment=105, sample_size=23, trend=15),
    ]

    overall_avg = sum(p.avg_days_to_payment for p in by_province) // len(by_province)

    return PaymentDelayAnalytics(
        by_province=by_province,
        by_department=by_department,
        overall_average=overall_avg
    )


# =============================================================================
# PIPELINE
# =============================================================================

@router.get("/pipeline", response_model=PipelineStatus)
async def get_pipeline_status(
    db: AsyncSession = Depends(get_db)
):
    """Get user's tender pipeline status."""
    today = date.today()

    # TODO: In production, load from user's pipeline_tenders table
    # Return empty for now - tenders added via POST
    return PipelineStatus(
        tenders=[],
        active_count=0,
        due_this_week=0,
        has_conflicts=False
    )


@router.post("/pipeline", response_model=PipelineTender)
async def add_to_pipeline(
    item: PipelineTenderCreate,
    db: AsyncSession = Depends(get_db)
):
    """Add a tender to user's pipeline."""
    today = date.today()

    # TODO: Load tender details from database
    # TODO: Persist to pipeline_tenders table

    return PipelineTender(
        id="pipeline_001",
        tender_id=item.tender_id,
        user_id="user_001",
        title="Sample Tender",
        reference="REF-001",
        deadline=today + timedelta(days=14),
        status=item.status,
        start_date=today,
        phases=[],
        has_conflict=False
    )


@router.patch("/pipeline/{pipeline_id}", response_model=PipelineTender)
async def update_pipeline_status(
    pipeline_id: str,
    status: str,
    db: AsyncSession = Depends(get_db)
):
    """Update a pipeline tender's status."""
    today = date.today()

    return PipelineTender(
        id=pipeline_id,
        tender_id="tender_001",
        user_id="user_001",
        title="Sample Tender",
        reference="REF-001",
        deadline=today + timedelta(days=14),
        status=status,
        start_date=today,
        phases=[],
        has_conflict=False
    )


@router.delete("/pipeline/{pipeline_id}")
async def remove_from_pipeline(
    pipeline_id: str,
    db: AsyncSession = Depends(get_db)
):
    """Remove a tender from user's pipeline."""
    return {"status": "removed", "pipeline_id": pipeline_id}


# =============================================================================
# TENDER OUTCOMES
# =============================================================================

@router.post("/outcomes", response_model=TenderOutcome)
async def record_outcome(
    outcome: TenderOutcomeCreate,
    db: AsyncSession = Depends(get_db)
):
    """Record a tender outcome (won, lost, withdrawn, cancelled)."""
    today = date.today()

    return TenderOutcome(
        id="outcome_001",
        user_id="user_001",
        tender_id=outcome.tender_id,
        outcome=outcome.outcome,
        reason_code=outcome.reason_code,
        reason_details=outcome.reason_details,
        award_date=outcome.award_date,
        award_amount=outcome.award_amount,
        created_at=today
    )


@router.get("/outcomes", response_model=OutcomeAnalytics)
async def get_outcome_analytics(
    period_months: int = Query(12, ge=1, le=24),
    db: AsyncSession = Depends(get_db)
):
    """Get tender outcome analytics."""
    period_end = date.today()
    period_start = period_end - timedelta(days=period_months * 30)

    # TODO: Aggregate from outcomes table
    return OutcomeAnalytics(
        total_submitted=18,
        total_won=6,
        total_lost=12,
        win_rate=0.33,
        outcomes_by_reason={
            "price": 5,
            "functionality": 3,
            "bbbee": 2,
            "disqualified": 2
        },
        period_start=period_start,
        period_end=period_end
    )
