"""Analytics service for aggregating user data."""

from datetime import date, timedelta
from decimal import Decimal
from typing import Any, List, Optional
from uuid import UUID

from sqlalchemy import and_, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models import (
    CompanyProfile,
    ComplianceRegistration,
    CostItem,
    PipelineTender,
    Tender,
    TenderOutcome,
)


class AnalyticsService:
    """Service for analytics operations."""

    def __init__(self, db: AsyncSession, user_id: UUID):
        self.db = db
        self.user_id = user_id

    # ==================== COMPANY PROFILE ====================

    async def get_company_profile(self) -> Optional[CompanyProfile]:
        """Get user's company profile."""
        result = await self.db.execute(
            select(CompanyProfile).where(CompanyProfile.user_id == self.user_id)
        )
        return result.scalar_one_or_none()

    async def create_or_update_company_profile(
        self, data: dict[str, Any]
    ) -> CompanyProfile:
        """Create or update user's company profile."""
        profile = await self.get_company_profile()
        if profile:
            for key, value in data.items():
                if hasattr(profile, key):
                    setattr(profile, key, value)
        else:
            profile = CompanyProfile(user_id=self.user_id, **data)
            self.db.add(profile)
        await self.db.flush()
        return profile

    # ==================== COMPLIANCE ====================

    async def get_compliance_registrations(self) -> List[ComplianceRegistration]:
        """Get all compliance registrations for user."""
        result = await self.db.execute(
            select(ComplianceRegistration)
            .where(ComplianceRegistration.user_id == self.user_id)
            .order_by(ComplianceRegistration.expiry_date.asc().nulls_last())
        )
        return list(result.scalars().all())

    async def get_compliance_by_id(
        self, registration_id: UUID
    ) -> Optional[ComplianceRegistration]:
        """Get a specific compliance registration."""
        result = await self.db.execute(
            select(ComplianceRegistration).where(
                and_(
                    ComplianceRegistration.id == registration_id,
                    ComplianceRegistration.user_id == self.user_id,
                )
            )
        )
        return result.scalar_one_or_none()

    async def create_compliance_registration(
        self, data: dict[str, Any]
    ) -> ComplianceRegistration:
        """Create a new compliance registration."""
        registration = ComplianceRegistration(user_id=self.user_id, **data)
        self.db.add(registration)
        await self.db.flush()
        return registration

    async def update_compliance_registration(
        self, registration_id: UUID, data: dict[str, Any]
    ) -> Optional[ComplianceRegistration]:
        """Update a compliance registration."""
        registration = await self.get_compliance_by_id(registration_id)
        if registration:
            for key, value in data.items():
                if hasattr(registration, key) and key != "id" and key != "user_id":
                    setattr(registration, key, value)
            await self.db.flush()
        return registration

    async def delete_compliance_registration(self, registration_id: UUID) -> bool:
        """Delete a compliance registration."""
        registration = await self.get_compliance_by_id(registration_id)
        if registration:
            await self.db.delete(registration)
            await self.db.flush()
            return True
        return False

    async def get_compliance_status(self) -> dict[str, Any]:
        """Get compliance status summary."""
        registrations = await self.get_compliance_registrations()

        if not registrations:
            return {
                "is_empty": True,
                "total": 0,
                "valid": 0,
                "expiring_soon": 0,
                "expired": 0,
                "items": [],
            }

        today = date.today()
        warning_threshold = today + timedelta(days=90)

        valid_count = 0
        expiring_soon_count = 0
        expired_count = 0

        items = []
        for reg in registrations:
            status = "valid"
            days_remaining = None

            if reg.expiry_type == "perpetual":
                status = "valid"
            elif reg.expiry_date:
                days_remaining = (reg.expiry_date - today).days
                if reg.expiry_date < today:
                    status = "expired"
                    expired_count += 1
                elif reg.expiry_date <= warning_threshold:
                    status = "expiring_soon"
                    expiring_soon_count += 1
                else:
                    status = "valid"
                    valid_count += 1
            else:
                valid_count += 1

            items.append({
                "id": str(reg.id),
                "type": reg.type,
                "name": reg.name,
                "short_name": reg.short_name,
                "registration_number": reg.registration_number,
                "issue_date": reg.issue_date.isoformat() if reg.issue_date else None,
                "expiry_date": reg.expiry_date.isoformat() if reg.expiry_date else None,
                "expiry_type": reg.expiry_type,
                "status": status,
                "days_remaining": days_remaining,
                "renewal_url": reg.renewal_url,
            })

        return {
            "is_empty": False,
            "total": len(registrations),
            "valid": valid_count,
            "expiring_soon": expiring_soon_count,
            "expired": expired_count,
            "items": items,
        }

    # ==================== COSTS ====================

    async def get_cost_items(
        self,
        category: Optional[str] = None,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None,
    ) -> List[CostItem]:
        """Get cost items with optional filters."""
        query = select(CostItem).where(CostItem.user_id == self.user_id)

        if category:
            query = query.where(CostItem.category == category)
        if start_date:
            query = query.where(CostItem.date >= start_date)
        if end_date:
            query = query.where(CostItem.date <= end_date)

        query = query.order_by(CostItem.date.desc())
        result = await self.db.execute(query)
        return list(result.scalars().all())

    async def create_cost_item(self, data: dict[str, Any]) -> CostItem:
        """Create a new cost item."""
        cost_item = CostItem(user_id=self.user_id, **data)
        self.db.add(cost_item)
        await self.db.flush()
        return cost_item

    async def delete_cost_item(self, cost_id: UUID) -> bool:
        """Delete a cost item."""
        result = await self.db.execute(
            select(CostItem).where(
                and_(CostItem.id == cost_id, CostItem.user_id == self.user_id)
            )
        )
        cost_item = result.scalar_one_or_none()
        if cost_item:
            await self.db.delete(cost_item)
            await self.db.flush()
            return True
        return False

    async def get_cost_analytics(
        self, period_months: int = 12
    ) -> dict[str, Any]:
        """Get cost analytics aggregated by category."""
        start_date = date.today() - timedelta(days=period_months * 30)

        # Get total by category
        result = await self.db.execute(
            select(
                CostItem.category,
                func.sum(CostItem.amount).label("total"),
                func.count(CostItem.id).label("count"),
            )
            .where(
                and_(
                    CostItem.user_id == self.user_id,
                    CostItem.date >= start_date,
                )
            )
            .group_by(CostItem.category)
        )

        categories_raw = result.all()

        if not categories_raw:
            return {
                "is_empty": True,
                "total_spent": 0,
                "period_label": f"Last {period_months} months",
                "categories": [],
            }

        total_spent = sum(row.total or 0 for row in categories_raw)

        categories = []
        for row in categories_raw:
            categories.append({
                "category": row.category,
                "amount": float(row.total or 0),
                "count": row.count,
                "percentage": float(row.total / total_spent * 100) if total_spent > 0 else 0,
            })

        # Sort by amount descending
        categories.sort(key=lambda x: x["amount"], reverse=True)

        return {
            "is_empty": False,
            "total_spent": float(total_spent),
            "period_label": f"Last {period_months} months",
            "categories": categories,
        }

    # ==================== FUNNEL / OUTCOMES ====================

    async def get_tender_outcomes(self) -> List[TenderOutcome]:
        """Get all tender outcomes for user."""
        result = await self.db.execute(
            select(TenderOutcome)
            .where(TenderOutcome.user_id == self.user_id)
            .order_by(TenderOutcome.award_date.desc())
        )
        return list(result.scalars().all())

    async def create_tender_outcome(self, data: dict[str, Any]) -> TenderOutcome:
        """Record a tender outcome."""
        outcome = TenderOutcome(user_id=self.user_id, **data)
        self.db.add(outcome)
        await self.db.flush()
        return outcome

    async def get_funnel_analytics(self) -> dict[str, Any]:
        """Get opportunity funnel analytics."""
        # Count outcomes by status
        result = await self.db.execute(
            select(
                TenderOutcome.outcome,
                func.count(TenderOutcome.id).label("count"),
                func.sum(TenderOutcome.award_amount).label("total_value"),
            )
            .where(TenderOutcome.user_id == self.user_id)
            .group_by(TenderOutcome.outcome)
        )
        outcomes_raw = result.all()

        if not outcomes_raw:
            return {
                "is_empty": True,
                "stages": [
                    {"name": "Discovered", "count": 0, "value": 0},
                    {"name": "Qualified", "count": 0, "value": 0},
                    {"name": "Submitted", "count": 0, "value": 0},
                    {"name": "Won", "count": 0, "value": 0},
                ],
                "win_rate": 0.0,
                "cost_per_win": 0.0,
                "avg_days_to_award": 0,
            }

        outcome_counts = {row.outcome: row.count for row in outcomes_raw}
        outcome_values = {row.outcome: float(row.total_value or 0) for row in outcomes_raw}

        total_submitted = sum(outcome_counts.get(o, 0) for o in ["won", "lost", "withdrawn"])
        won_count = outcome_counts.get("won", 0)

        # Calculate win rate
        win_rate = (won_count / total_submitted * 100) if total_submitted > 0 else 0

        # Calculate average days to award
        avg_result = await self.db.execute(
            select(func.avg(
                func.extract('epoch', TenderOutcome.created_at) -
                func.extract('epoch', Tender.discovered_at)
            ) / 86400)
            .select_from(TenderOutcome)
            .join(Tender, TenderOutcome.tender_id == Tender.id)
            .where(
                and_(
                    TenderOutcome.user_id == self.user_id,
                    TenderOutcome.outcome == "won",
                )
            )
        )
        avg_days = avg_result.scalar() or 0

        # Get cost per win
        cost_analytics = await self.get_cost_analytics(12)
        total_costs = cost_analytics.get("total_spent", 0)
        cost_per_win = total_costs / won_count if won_count > 0 else 0

        # Build stages (simplified funnel)
        discovered = outcome_counts.get("discovered", 0) + total_submitted
        qualified = outcome_counts.get("qualified", 0) + total_submitted
        submitted = total_submitted
        won = won_count

        return {
            "is_empty": False,
            "stages": [
                {"name": "Discovered", "count": discovered, "value": 0},
                {"name": "Qualified", "count": qualified, "value": 0},
                {"name": "Submitted", "count": submitted, "value": 0},
                {"name": "Won", "count": won, "value": outcome_values.get("won", 0)},
            ],
            "win_rate": round(win_rate, 1),
            "cost_per_win": round(cost_per_win, 2),
            "avg_days_to_award": int(avg_days),
        }

    # ==================== PIPELINE ====================

    async def get_pipeline_tenders(self) -> List[PipelineTender]:
        """Get all pipeline tenders for user."""
        result = await self.db.execute(
            select(PipelineTender)
            .where(PipelineTender.user_id == self.user_id)
            .order_by(PipelineTender.created_at.desc())
        )
        return list(result.scalars().all())

    async def get_pipeline_tender_by_id(
        self, pipeline_id: UUID
    ) -> Optional[PipelineTender]:
        """Get a specific pipeline tender."""
        result = await self.db.execute(
            select(PipelineTender).where(
                and_(
                    PipelineTender.id == pipeline_id,
                    PipelineTender.user_id == self.user_id,
                )
            )
        )
        return result.scalar_one_or_none()

    async def add_to_pipeline(self, data: dict[str, Any]) -> PipelineTender:
        """Add a tender to user's pipeline."""
        pipeline_tender = PipelineTender(user_id=self.user_id, **data)
        self.db.add(pipeline_tender)
        await self.db.flush()
        return pipeline_tender

    async def update_pipeline_tender(
        self, pipeline_id: UUID, data: dict[str, Any]
    ) -> Optional[PipelineTender]:
        """Update a pipeline tender."""
        pipeline_tender = await self.get_pipeline_tender_by_id(pipeline_id)
        if pipeline_tender:
            for key, value in data.items():
                if hasattr(pipeline_tender, key) and key != "id" and key != "user_id":
                    setattr(pipeline_tender, key, value)
            await self.db.flush()
        return pipeline_tender

    async def remove_from_pipeline(self, pipeline_id: UUID) -> bool:
        """Remove a tender from pipeline."""
        pipeline_tender = await self.get_pipeline_tender_by_id(pipeline_id)
        if pipeline_tender:
            await self.db.delete(pipeline_tender)
            await self.db.flush()
            return True
        return False

    async def get_pipeline_status(self) -> dict[str, Any]:
        """Get pipeline status summary."""
        pipeline_tenders = await self.get_pipeline_tenders()

        if not pipeline_tenders:
            return {
                "is_empty": True,
                "active_count": 0,
                "due_this_week": 0,
                "has_conflicts": False,
                "tenders": [],
            }

        today = date.today()
        week_end = today + timedelta(days=7)

        due_this_week = 0
        has_conflicts = False
        tenders_data = []

        for pt in pipeline_tenders:
            # Load associated tender for deadline
            tender_result = await self.db.execute(
                select(Tender).where(Tender.id == pt.tender_id)
            )
            tender = tender_result.scalar_one_or_none()

            deadline = tender.deadline.date() if tender and tender.deadline else None
            if deadline and deadline <= week_end:
                due_this_week += 1

            if pt.has_conflict:
                has_conflicts = True

            tenders_data.append({
                "id": str(pt.id),
                "tender_id": str(pt.tender_id),
                "title": tender.title if tender else "Unknown",
                "reference": tender.reference if tender else None,
                "status": pt.status,
                "deadline": deadline.isoformat() if deadline else None,
                "phases": pt.phases or [],
                "assigned_to": pt.assigned_to,
                "has_conflict": pt.has_conflict,
            })

        return {
            "is_empty": False,
            "active_count": len(pipeline_tenders),
            "due_this_week": due_this_week,
            "has_conflicts": has_conflicts,
            "tenders": tenders_data,
        }

    # ==================== PAYMENT DELAYS ====================

    async def get_payment_delay_analytics(self) -> dict[str, Any]:
        """Get payment delay analytics from user's outcomes."""
        result = await self.db.execute(
            select(TenderOutcome)
            .where(
                and_(
                    TenderOutcome.user_id == self.user_id,
                    TenderOutcome.outcome == "won",
                    TenderOutcome.payment_received_date.isnot(None),
                )
            )
        )
        outcomes = result.scalars().all()

        if not outcomes:
            return {
                "is_empty": True,
                "average_days": 0,
                "by_department": [],
            }

        total_days = 0
        count = 0
        for outcome in outcomes:
            if outcome.days_to_payment:
                total_days += outcome.days_to_payment
                count += 1

        avg_days = total_days / count if count > 0 else 0

        return {
            "is_empty": False,
            "average_days": int(avg_days),
            "total_payments": count,
            "by_department": [],  # Would need department data
        }
