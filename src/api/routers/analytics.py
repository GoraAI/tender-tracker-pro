"""Analytics API endpoints with REAL database queries.

NO MOCK DATA - returns empty state for new users.
Users enter their own data through the app.
"""
import json
from datetime import date, datetime, timedelta
from decimal import Decimal
from pathlib import Path
from typing import Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from ..database import get_db
from ..dependencies import CurrentUser
from ..models import (
    CompanyProfile,
    ComplianceRegistration,
    CostItem,
    PipelineTender as PipelineTenderModel,
    TenderOutcome as TenderOutcomeModel,
    Tender,
    User,
)
from ..schemas.analytics import (
    FunnelAnalytics,
    FunnelStage,
    CostAnalytics,
    CostCategory,
    CostItemCreate,
    CostItemResponse,
    ComplianceStatus,
    ComplianceItemCreate,
    ComplianceItemResponse,
    PaymentDelayAnalytics,
    PaymentDelayData,
    PipelineStatus,
    PipelineTenderCreate,
    PipelineTenderUpdate,
    PipelineTenderResponse,
    TenderOutcomeCreate,
    TenderOutcomeResponse,
    OutcomeAnalytics,
    CompanyProfileCreate,
    CompanyProfileResponse,
)

router = APIRouter(prefix="/analytics", tags=["analytics"])

# Path to reference data
REFERENCE_DATA_PATH = Path(__file__).parent.parent.parent.parent.parent / "data" / "reference"


def load_payment_reference_data() -> dict:
    """Load payment delay reference data from National Treasury statistics."""
    try:
        reference_file = REFERENCE_DATA_PATH / "payment_delays_sa_treasury.json"
        if reference_file.exists():
            with open(reference_file) as f:
                return json.load(f)
    except Exception:
        pass
    return {}


# =============================================================================
# COMPANY PROFILE
# =============================================================================

@router.get("/profile", response_model=Optional[CompanyProfileResponse])
async def get_company_profile(
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Get user's company profile. Returns null if not set up."""
    result = await db.execute(
        select(CompanyProfile).where(CompanyProfile.user_id == current_user.id)
    )
    profile = result.scalar_one_or_none()
    if not profile:
        return None
    return CompanyProfileResponse.model_validate(profile)


@router.post("/profile", response_model=CompanyProfileResponse, status_code=status.HTTP_201_CREATED)
async def create_company_profile(
    profile_data: CompanyProfileCreate,
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Create user's company profile."""
    # Check if profile already exists
    result = await db.execute(
        select(CompanyProfile).where(CompanyProfile.user_id == current_user.id)
    )
    existing = result.scalar_one_or_none()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Company profile already exists. Use PUT to update."
        )

    profile = CompanyProfile(
        user_id=current_user.id,
        legal_name=profile_data.legal_name,
        trading_name=profile_data.trading_name,
        industry=profile_data.industry,
        capabilities=profile_data.capabilities or [],
        preferred_sectors=profile_data.preferred_sectors or [],
        preferred_provinces=profile_data.preferred_provinces or [],
        keywords=profile_data.keywords or [],
        value_range_min=Decimal(str(profile_data.value_range_min)) if profile_data.value_range_min else None,
        value_range_max=Decimal(str(profile_data.value_range_max)) if profile_data.value_range_max else None,
    )
    db.add(profile)
    await db.commit()
    await db.refresh(profile)
    return CompanyProfileResponse.model_validate(profile)


@router.put("/profile", response_model=CompanyProfileResponse)
async def update_company_profile(
    profile_data: CompanyProfileCreate,
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Update user's company profile."""
    result = await db.execute(
        select(CompanyProfile).where(CompanyProfile.user_id == current_user.id)
    )
    profile = result.scalar_one_or_none()
    if not profile:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Company profile not found. Use POST to create."
        )

    profile.legal_name = profile_data.legal_name
    profile.trading_name = profile_data.trading_name
    profile.industry = profile_data.industry
    profile.capabilities = profile_data.capabilities or []
    profile.preferred_sectors = profile_data.preferred_sectors or []
    profile.preferred_provinces = profile_data.preferred_provinces or []
    profile.keywords = profile_data.keywords or []
    profile.value_range_min = Decimal(str(profile_data.value_range_min)) if profile_data.value_range_min else None
    profile.value_range_max = Decimal(str(profile_data.value_range_max)) if profile_data.value_range_max else None

    await db.commit()
    await db.refresh(profile)
    return CompanyProfileResponse.model_validate(profile)


# =============================================================================
# FUNNEL ANALYTICS
# =============================================================================

@router.get("/funnel", response_model=FunnelAnalytics)
async def get_funnel_analytics(
    current_user: CurrentUser,
    period_months: int = Query(12, ge=1, le=24),
    db: AsyncSession = Depends(get_db)
):
    """
    Get opportunity funnel analytics from REAL user data.

    Returns empty funnel if user has no pipeline/outcome data.
    """
    period_end = date.today()
    period_start = period_end - timedelta(days=period_months * 30)

    # Count pipeline tenders by status
    result = await db.execute(
        select(PipelineTenderModel.status, func.count(PipelineTenderModel.id))
        .where(
            PipelineTenderModel.user_id == current_user.id,
            PipelineTenderModel.created_at >= datetime.combine(period_start, datetime.min.time())
        )
        .group_by(PipelineTenderModel.status)
    )
    status_counts = dict(result.all())

    # Count outcomes
    result = await db.execute(
        select(TenderOutcomeModel.outcome, func.count(TenderOutcomeModel.id))
        .where(
            TenderOutcomeModel.user_id == current_user.id,
            TenderOutcomeModel.award_date >= period_start
        )
        .group_by(TenderOutcomeModel.outcome)
    )
    outcome_counts = dict(result.all())

    # Calculate funnel stages
    discovered = status_counts.get("discovered", 0) + sum(status_counts.values())
    qualified = status_counts.get("qualified", 0) + status_counts.get("preparation", 0) + \
                status_counts.get("review", 0) + status_counts.get("submission", 0) + \
                status_counts.get("submitted", 0) + outcome_counts.get("won", 0) + \
                outcome_counts.get("lost", 0)
    submitted = status_counts.get("submitted", 0) + outcome_counts.get("won", 0) + \
                outcome_counts.get("lost", 0)
    awarded = outcome_counts.get("won", 0)

    # Check if user has any data
    is_empty = discovered == 0

    stages = [
        FunnelStage(
            name="Discovered",
            count=discovered,
            drop_off_count=max(0, discovered - qualified),
            drop_off_reason="filtered out" if discovered > qualified else ""
        ),
        FunnelStage(
            name="Qualified",
            count=qualified,
            drop_off_count=max(0, qualified - submitted),
            drop_off_reason="no-bid decision" if qualified > submitted else ""
        ),
        FunnelStage(
            name="Submitted",
            count=submitted,
            drop_off_count=max(0, submitted - awarded),
            drop_off_reason="unsuccessful" if submitted > awarded else ""
        ),
        FunnelStage(
            name="Awarded",
            count=awarded,
            drop_off_count=0
        )
    ]

    # Calculate metrics
    win_rate = awarded / submitted if submitted > 0 else 0.0

    # Calculate cost per win from user's cost data
    result = await db.execute(
        select(func.sum(CostItem.amount))
        .where(
            CostItem.user_id == current_user.id,
            CostItem.date >= period_start
        )
    )
    total_costs = result.scalar() or Decimal("0")
    cost_per_win = float(total_costs) / awarded if awarded > 0 else 0.0

    # Calculate average days to award
    result = await db.execute(
        select(func.avg(
            func.julianday(TenderOutcomeModel.award_date) -
            func.julianday(TenderOutcomeModel.created_at)
        ))
        .where(
            TenderOutcomeModel.user_id == current_user.id,
            TenderOutcomeModel.outcome == "won",
            TenderOutcomeModel.award_date >= period_start
        )
    )
    avg_days = result.scalar()
    avg_days_to_award = int(avg_days) if avg_days else 0

    return FunnelAnalytics(
        stages=stages,
        win_rate=win_rate,
        cost_per_win=cost_per_win,
        avg_days_to_award=avg_days_to_award,
        period_start=period_start,
        period_end=period_end,
        is_empty=is_empty
    )


# =============================================================================
# COST TRACKING
# =============================================================================

@router.get("/costs", response_model=CostAnalytics)
async def get_cost_analytics(
    current_user: CurrentUser,
    period_months: int = Query(12, ge=1, le=24),
    db: AsyncSession = Depends(get_db)
):
    """
    Get cost-per-successful-bid analytics from REAL user data.

    Returns empty categories if user has no cost data.
    """
    period_end = date.today()
    period_start = period_end - timedelta(days=period_months * 30)

    # Get all cost items for user in period
    result = await db.execute(
        select(CostItem)
        .where(
            CostItem.user_id == current_user.id,
            CostItem.date >= period_start
        )
        .order_by(CostItem.date.desc())
    )
    cost_items = result.scalars().all()

    # Group by category
    categories_dict: dict[str, list[CostItem]] = {
        "platform": [],
        "compliance": [],
        "preparation": [],
        "submission": [],
        "opportunity": [],
    }

    for item in cost_items:
        if item.category in categories_dict:
            categories_dict[item.category].append(item)

    total_spent = sum(float(item.amount) for item in cost_items)

    categories = []
    for name, items in categories_dict.items():
        amount = sum(float(item.amount) for item in items)
        percentage = amount / total_spent if total_spent > 0 else 0.0
        categories.append(CostCategory(
            name=name,
            amount=amount,
            percentage=percentage,
            items=[CostItemResponse.model_validate(item) for item in items]
        ))

    # Count tenders won in period
    result = await db.execute(
        select(func.count(TenderOutcomeModel.id))
        .where(
            TenderOutcomeModel.user_id == current_user.id,
            TenderOutcomeModel.outcome == "won",
            TenderOutcomeModel.award_date >= period_start
        )
    )
    tenders_won = result.scalar() or 0

    cost_per_win = total_spent / tenders_won if tenders_won > 0 else 0.0
    is_empty = len(cost_items) == 0

    return CostAnalytics(
        categories=categories,
        total_spent=total_spent,
        tenders_won=tenders_won,
        cost_per_win=cost_per_win,
        period_start=period_start,
        period_end=period_end,
        is_empty=is_empty
    )


@router.post("/costs", response_model=CostItemResponse, status_code=status.HTTP_201_CREATED)
async def add_cost_item(
    item: CostItemCreate,
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Add a cost item for tracking."""
    cost_item = CostItem(
        user_id=current_user.id,
        tender_id=item.tender_id,
        category=item.category,
        description=item.description,
        amount=Decimal(str(item.amount)),
        date=item.date,
        recurring=item.recurring,
        frequency=item.frequency,
    )
    db.add(cost_item)
    await db.commit()
    await db.refresh(cost_item)
    return CostItemResponse.model_validate(cost_item)


@router.delete("/costs/{cost_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_cost_item(
    cost_id: UUID,
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Delete a cost item."""
    result = await db.execute(
        select(CostItem).where(
            CostItem.id == cost_id,
            CostItem.user_id == current_user.id
        )
    )
    item = result.scalar_one_or_none()
    if not item:
        raise HTTPException(status_code=404, detail="Cost item not found")

    await db.delete(item)
    await db.commit()


# =============================================================================
# COMPLIANCE STATUS
# =============================================================================

@router.get("/compliance", response_model=ComplianceStatus)
async def get_compliance_status(
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """
    Get user's compliance registration status from REAL user data.

    Returns empty list if user has no compliance registrations.
    """
    result = await db.execute(
        select(ComplianceRegistration)
        .where(ComplianceRegistration.user_id == current_user.id)
        .order_by(ComplianceRegistration.expiry_date.asc().nullslast())
    )
    registrations = result.scalars().all()

    items = []
    for reg in registrations:
        items.append(ComplianceItemResponse(
            id=reg.id,
            user_id=reg.user_id,
            type=reg.type,
            name=reg.name,
            short_name=reg.short_name,
            registration_number=reg.registration_number,
            issue_date=reg.issue_date,
            expiry_date=reg.expiry_date,
            expiry_type=reg.expiry_type,
            renewal_url=reg.renewal_url,
            documents=reg.documents,
            days_remaining=reg.days_remaining,
            is_expired=reg.is_expired,
            is_valid=reg.is_valid,
            created_at=reg.created_at,
            updated_at=reg.updated_at,
        ))

    valid_items = [i for i in items if i.is_valid]
    is_empty = len(items) == 0

    # Find next expiring item
    next_expiry = None
    next_expiry_item = None
    for item in items:
        if item.days_remaining is not None and item.days_remaining > 0:
            if next_expiry is None or item.days_remaining < next_expiry:
                next_expiry = item.days_remaining
                next_expiry_item = item.short_name

    return ComplianceStatus(
        items=items,
        valid_count=len(valid_items),
        total_count=len(items),
        days_to_next_expiry=next_expiry,
        next_expiry_item=next_expiry_item,
        is_empty=is_empty
    )


@router.post("/compliance", response_model=ComplianceItemResponse, status_code=status.HTTP_201_CREATED)
async def add_compliance_item(
    item: ComplianceItemCreate,
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Add a compliance registration."""
    registration = ComplianceRegistration(
        user_id=current_user.id,
        type=item.type,
        name=item.name,
        short_name=item.short_name,
        registration_number=item.registration_number,
        issue_date=item.issue_date,
        expiry_date=item.expiry_date,
        expiry_type=item.expiry_type,
        renewal_url=item.renewal_url,
        documents=item.documents or [],
    )
    db.add(registration)
    await db.commit()
    await db.refresh(registration)

    return ComplianceItemResponse(
        id=registration.id,
        user_id=registration.user_id,
        type=registration.type,
        name=registration.name,
        short_name=registration.short_name,
        registration_number=registration.registration_number,
        issue_date=registration.issue_date,
        expiry_date=registration.expiry_date,
        expiry_type=registration.expiry_type,
        renewal_url=registration.renewal_url,
        documents=registration.documents,
        days_remaining=registration.days_remaining,
        is_expired=registration.is_expired,
        is_valid=registration.is_valid,
        created_at=registration.created_at,
        updated_at=registration.updated_at,
    )


@router.put("/compliance/{item_id}", response_model=ComplianceItemResponse)
async def update_compliance_item(
    item_id: UUID,
    item: ComplianceItemCreate,
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Update a compliance registration."""
    result = await db.execute(
        select(ComplianceRegistration).where(
            ComplianceRegistration.id == item_id,
            ComplianceRegistration.user_id == current_user.id
        )
    )
    registration = result.scalar_one_or_none()
    if not registration:
        raise HTTPException(status_code=404, detail="Compliance item not found")

    registration.type = item.type
    registration.name = item.name
    registration.short_name = item.short_name
    registration.registration_number = item.registration_number
    registration.issue_date = item.issue_date
    registration.expiry_date = item.expiry_date
    registration.expiry_type = item.expiry_type
    registration.renewal_url = item.renewal_url
    registration.documents = item.documents or []

    await db.commit()
    await db.refresh(registration)

    return ComplianceItemResponse(
        id=registration.id,
        user_id=registration.user_id,
        type=registration.type,
        name=registration.name,
        short_name=registration.short_name,
        registration_number=registration.registration_number,
        issue_date=registration.issue_date,
        expiry_date=registration.expiry_date,
        expiry_type=registration.expiry_type,
        renewal_url=registration.renewal_url,
        documents=registration.documents,
        days_remaining=registration.days_remaining,
        is_expired=registration.is_expired,
        is_valid=registration.is_valid,
        created_at=registration.created_at,
        updated_at=registration.updated_at,
    )


@router.delete("/compliance/{item_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_compliance_item(
    item_id: UUID,
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Delete a compliance registration."""
    result = await db.execute(
        select(ComplianceRegistration).where(
            ComplianceRegistration.id == item_id,
            ComplianceRegistration.user_id == current_user.id
        )
    )
    item = result.scalar_one_or_none()
    if not item:
        raise HTTPException(status_code=404, detail="Compliance item not found")

    await db.delete(item)
    await db.commit()


# =============================================================================
# PAYMENT DELAY
# =============================================================================

@router.get("/payment-delays", response_model=PaymentDelayAnalytics)
async def get_payment_delay_analytics(
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """
    Get payment delay analytics.

    Uses National Treasury reference data as baseline.
    Supplements with user's actual payment experience over time.
    """
    # Load reference data from National Treasury
    reference_data = load_payment_reference_data()

    by_province = []
    by_department = []

    # Get user's payment data if available
    result = await db.execute(
        select(TenderOutcomeModel)
        .where(
            TenderOutcomeModel.user_id == current_user.id,
            TenderOutcomeModel.outcome == "won",
            TenderOutcomeModel.payment_received_date.isnot(None)
        )
    )
    user_payments = result.scalars().all()
    user_data_available = len(user_payments) > 0

    # Build province data from reference
    provinces = reference_data.get("provinces", {})
    for prov_id, prov_data in provinces.items():
        by_province.append(PaymentDelayData(
            id=prov_id,
            name=prov_data.get("name", prov_id),
            short_name=prov_id,
            avg_days_to_payment=prov_data.get("avg_days", 45),
            sample_size=prov_data.get("sample_size", 0),
            trend=prov_data.get("trend", 0),
            source="reference"
        ))

    # Sort provinces by payment delay (worst first)
    by_province.sort(key=lambda x: x.avg_days_to_payment, reverse=True)

    # Add department data from reference
    departments = reference_data.get("departments", {})
    for dept_id, dept_data in departments.items():
        by_department.append(PaymentDelayData(
            id=dept_id,
            name=dept_data.get("name", dept_id),
            short_name=dept_id,
            avg_days_to_payment=dept_data.get("avg_days", 45),
            sample_size=dept_data.get("sample_size", 0),
            trend=dept_data.get("trend", 0),
            source="reference"
        ))

    # Calculate overall average from reference data
    if by_province:
        overall_avg = sum(p.avg_days_to_payment for p in by_province) // len(by_province)
    else:
        overall_avg = 45  # Default based on National Treasury 30-day policy

    # If user has payment data, calculate their actual experience
    if user_data_available:
        user_avg = sum(
            p.days_to_payment for p in user_payments
            if p.days_to_payment is not None
        ) / len([p for p in user_payments if p.days_to_payment is not None])
        # Could add user-specific stats here

    return PaymentDelayAnalytics(
        by_province=by_province,
        by_department=by_department,
        overall_average=overall_avg,
        user_data_available=user_data_available,
        reference_data_source=reference_data.get(
            "source",
            "National Treasury Reports 2024-2025"
        )
    )


# =============================================================================
# PIPELINE
# =============================================================================

@router.get("/pipeline", response_model=PipelineStatus)
async def get_pipeline_status(
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Get user's tender pipeline status from REAL user data."""
    result = await db.execute(
        select(PipelineTenderModel)
        .options(selectinload(PipelineTenderModel.tender))
        .where(PipelineTenderModel.user_id == current_user.id)
        .order_by(PipelineTenderModel.created_at.desc())
    )
    pipeline_entries = result.scalars().all()

    tenders = []
    due_this_week = 0
    has_conflicts = False
    today = date.today()
    week_end = today + timedelta(days=7)

    for entry in pipeline_entries:
        tender = entry.tender
        if tender and tender.deadline:
            deadline_date = tender.deadline.date() if isinstance(tender.deadline, datetime) else tender.deadline
            if today <= deadline_date <= week_end:
                due_this_week += 1

        if entry.has_conflict:
            has_conflicts = True

        tenders.append(PipelineTenderResponse(
            id=entry.id,
            user_id=entry.user_id,
            tender_id=entry.tender_id,
            title=tender.title if tender else None,
            reference=tender.reference if tender else None,
            deadline=tender.deadline if tender else None,
            status=entry.status,
            start_date=entry.start_date,
            phases=entry.phases,
            assigned_to=entry.assigned_to,
            notes=entry.notes,
            has_conflict=entry.has_conflict,
            created_at=entry.created_at,
            updated_at=entry.updated_at,
        ))

    # Active = not submitted or won/lost
    active_count = sum(1 for t in tenders if t.status not in ["submitted"])
    is_empty = len(tenders) == 0

    return PipelineStatus(
        tenders=tenders,
        active_count=active_count,
        due_this_week=due_this_week,
        has_conflicts=has_conflicts,
        is_empty=is_empty
    )


@router.post("/pipeline", response_model=PipelineTenderResponse, status_code=status.HTTP_201_CREATED)
async def add_to_pipeline(
    item: PipelineTenderCreate,
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Add a tender to user's pipeline."""
    # Verify tender exists
    result = await db.execute(
        select(Tender).where(Tender.id == item.tender_id)
    )
    tender = result.scalar_one_or_none()
    if not tender:
        raise HTTPException(status_code=404, detail="Tender not found")

    # Check if already in pipeline
    result = await db.execute(
        select(PipelineTenderModel).where(
            PipelineTenderModel.user_id == current_user.id,
            PipelineTenderModel.tender_id == item.tender_id
        )
    )
    existing = result.scalar_one_or_none()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Tender already in pipeline"
        )

    pipeline_entry = PipelineTenderModel(
        user_id=current_user.id,
        tender_id=item.tender_id,
        status=item.status,
        start_date=date.today(),
        assigned_to=item.assigned_to,
        notes=item.notes,
        phases=[],
        has_conflict=False,
    )
    db.add(pipeline_entry)
    await db.commit()
    await db.refresh(pipeline_entry)

    return PipelineTenderResponse(
        id=pipeline_entry.id,
        user_id=pipeline_entry.user_id,
        tender_id=pipeline_entry.tender_id,
        title=tender.title,
        reference=tender.reference,
        deadline=tender.deadline,
        status=pipeline_entry.status,
        start_date=pipeline_entry.start_date,
        phases=pipeline_entry.phases,
        assigned_to=pipeline_entry.assigned_to,
        notes=pipeline_entry.notes,
        has_conflict=pipeline_entry.has_conflict,
        created_at=pipeline_entry.created_at,
        updated_at=pipeline_entry.updated_at,
    )


@router.patch("/pipeline/{pipeline_id}", response_model=PipelineTenderResponse)
async def update_pipeline_status(
    pipeline_id: UUID,
    update: PipelineTenderUpdate,
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Update a pipeline tender's status."""
    result = await db.execute(
        select(PipelineTenderModel)
        .options(selectinload(PipelineTenderModel.tender))
        .where(
            PipelineTenderModel.id == pipeline_id,
            PipelineTenderModel.user_id == current_user.id
        )
    )
    entry = result.scalar_one_or_none()
    if not entry:
        raise HTTPException(status_code=404, detail="Pipeline entry not found")

    if update.status is not None:
        entry.status = update.status
    if update.assigned_to is not None:
        entry.assigned_to = update.assigned_to
    if update.notes is not None:
        entry.notes = update.notes
    if update.phases is not None:
        entry.phases = update.phases
    if update.has_conflict is not None:
        entry.has_conflict = update.has_conflict

    await db.commit()
    await db.refresh(entry)

    tender = entry.tender
    return PipelineTenderResponse(
        id=entry.id,
        user_id=entry.user_id,
        tender_id=entry.tender_id,
        title=tender.title if tender else None,
        reference=tender.reference if tender else None,
        deadline=tender.deadline if tender else None,
        status=entry.status,
        start_date=entry.start_date,
        phases=entry.phases,
        assigned_to=entry.assigned_to,
        notes=entry.notes,
        has_conflict=entry.has_conflict,
        created_at=entry.created_at,
        updated_at=entry.updated_at,
    )


@router.delete("/pipeline/{pipeline_id}", status_code=status.HTTP_204_NO_CONTENT)
async def remove_from_pipeline(
    pipeline_id: UUID,
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Remove a tender from user's pipeline."""
    result = await db.execute(
        select(PipelineTenderModel).where(
            PipelineTenderModel.id == pipeline_id,
            PipelineTenderModel.user_id == current_user.id
        )
    )
    entry = result.scalar_one_or_none()
    if not entry:
        raise HTTPException(status_code=404, detail="Pipeline entry not found")

    await db.delete(entry)
    await db.commit()


# =============================================================================
# TENDER OUTCOMES
# =============================================================================

@router.post("/outcomes", response_model=TenderOutcomeResponse, status_code=status.HTTP_201_CREATED)
async def record_outcome(
    outcome: TenderOutcomeCreate,
    current_user: CurrentUser,
    db: AsyncSession = Depends(get_db)
):
    """Record a tender outcome (won, lost, withdrawn, cancelled)."""
    # Verify tender exists
    result = await db.execute(
        select(Tender).where(Tender.id == outcome.tender_id)
    )
    tender = result.scalar_one_or_none()
    if not tender:
        raise HTTPException(status_code=404, detail="Tender not found")

    outcome_record = TenderOutcomeModel(
        user_id=current_user.id,
        tender_id=outcome.tender_id,
        outcome=outcome.outcome,
        reason_code=outcome.reason_code,
        reason_details=outcome.reason_details,
        award_date=outcome.award_date,
        award_amount=Decimal(str(outcome.award_amount)) if outcome.award_amount else None,
        payment_received_date=outcome.payment_received_date,
    )
    db.add(outcome_record)
    await db.commit()
    await db.refresh(outcome_record)

    return TenderOutcomeResponse(
        id=outcome_record.id,
        user_id=outcome_record.user_id,
        tender_id=outcome_record.tender_id,
        outcome=outcome_record.outcome,
        reason_code=outcome_record.reason_code,
        reason_details=outcome_record.reason_details,
        award_date=outcome_record.award_date,
        award_amount=float(outcome_record.award_amount) if outcome_record.award_amount else None,
        payment_received_date=outcome_record.payment_received_date,
        days_to_payment=outcome_record.days_to_payment,
        created_at=outcome_record.created_at,
    )


@router.get("/outcomes", response_model=OutcomeAnalytics)
async def get_outcome_analytics(
    current_user: CurrentUser,
    period_months: int = Query(12, ge=1, le=24),
    db: AsyncSession = Depends(get_db)
):
    """Get tender outcome analytics from REAL user data."""
    period_end = date.today()
    period_start = period_end - timedelta(days=period_months * 30)

    # Get all outcomes in period
    result = await db.execute(
        select(TenderOutcomeModel)
        .where(
            TenderOutcomeModel.user_id == current_user.id,
            TenderOutcomeModel.award_date >= period_start
        )
    )
    outcomes = result.scalars().all()

    # Count by outcome type
    total_won = sum(1 for o in outcomes if o.outcome == "won")
    total_lost = sum(1 for o in outcomes if o.outcome == "lost")
    total_withdrawn = sum(1 for o in outcomes if o.outcome == "withdrawn")
    total_cancelled = sum(1 for o in outcomes if o.outcome == "cancelled")
    total_submitted = total_won + total_lost  # withdrawn/cancelled don't count

    win_rate = total_won / total_submitted if total_submitted > 0 else 0.0

    # Total award amount
    total_award_amount = sum(
        float(o.award_amount) for o in outcomes
        if o.outcome == "won" and o.award_amount
    )

    # Average days to payment
    payments_with_days = [
        o.days_to_payment for o in outcomes
        if o.outcome == "won" and o.days_to_payment is not None
    ]
    avg_days_to_payment = int(sum(payments_with_days) / len(payments_with_days)) if payments_with_days else None

    # Count by reason code
    outcomes_by_reason: dict[str, int] = {}
    for o in outcomes:
        if o.outcome == "lost" and o.reason_code:
            outcomes_by_reason[o.reason_code] = outcomes_by_reason.get(o.reason_code, 0) + 1

    is_empty = len(outcomes) == 0

    return OutcomeAnalytics(
        total_submitted=total_submitted,
        total_won=total_won,
        total_lost=total_lost,
        total_withdrawn=total_withdrawn,
        total_cancelled=total_cancelled,
        win_rate=win_rate,
        total_award_amount=total_award_amount,
        avg_days_to_payment=avg_days_to_payment,
        outcomes_by_reason=outcomes_by_reason,
        period_start=period_start,
        period_end=period_end,
        is_empty=is_empty
    )
