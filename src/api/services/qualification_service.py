"""Qualification service for determining tender auto-fill eligibility."""

from datetime import datetime, timedelta, timezone
from typing import Optional
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models.tender import Tender
from src.api.models.score import Score
from src.api.models.criteria import UserCriteria


class QualificationResult:
    """Result of tender qualification evaluation."""

    def __init__(
        self,
        qualifies: bool,
        score: int,
        reasons: list[str],
        confidence: float,
        tender_id: UUID,
        user_id: UUID,
    ):
        self.qualifies = qualifies
        self.score = score
        self.reasons = reasons
        self.confidence = confidence
        self.tender_id = tender_id
        self.user_id = user_id
        self.checked_at = datetime.now(timezone.utc)


class QualificationService:
    """Service for evaluating tender qualification for auto-fill."""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def evaluate_tender(
        self,
        tender_id: UUID,
        user_id: UUID,
        min_score: int = 70,
        min_capabilities: int = 2,
        min_deadline_days: int = 7,
    ) -> QualificationResult:
        """
        Evaluate if a tender qualifies for auto-fill.

        Args:
            tender_id: Tender to evaluate
            user_id: User making the request
            min_score: Minimum score threshold (default 70)
            min_capabilities: Minimum capability matches (default 2)
            min_deadline_days: Minimum days before deadline (default 7)

        Returns:
            QualificationResult with decision and reasons
        """
        # Get tender
        tender_result = await self.db.execute(
            select(Tender).where(Tender.id == tender_id)
        )
        tender = tender_result.scalar_one_or_none()
        if not tender:
            return QualificationResult(
                qualifies=False,
                score=0,
                reasons=["Tender not found"],
                confidence=1.0,
                tender_id=tender_id,
                user_id=user_id,
            )

        # Get score
        score_result = await self.db.execute(
            select(Score).where(
                Score.tender_id == tender_id, Score.user_id == user_id
            )
        )
        score_record = score_result.scalar_one_or_none()

        # Get user criteria
        criteria_result = await self.db.execute(
            select(UserCriteria).where(
                UserCriteria.user_id == user_id, UserCriteria.is_default == True
            )
        )
        criteria = criteria_result.scalar_one_or_none()

        # Evaluate qualification criteria
        reasons = []
        qualifies = True
        confidence_factors = []

        # 1. Score threshold
        if score_record:
            score_value = score_record.score
            if score_value >= min_score:
                reasons.append(f"✓ Score: {score_value}/100 (High relevance)")
                confidence_factors.append(0.9)
            else:
                reasons.append(f"✗ Score: {score_value}/100 (Below threshold of {min_score})")
                qualifies = False
                confidence_factors.append(0.7)
        else:
            reasons.append("✗ No score available")
            qualifies = False
            confidence_factors.append(0.5)
            score_value = 0

        # 2. Capability match
        if criteria and tender.requirements_summary:
            user_capabilities = criteria.capabilities or []
            requirements_lower = tender.requirements_summary.lower()

            matching_caps = [
                cap for cap in user_capabilities if cap.lower() in requirements_lower
            ]

            if len(matching_caps) >= min_capabilities:
                cap_list = ", ".join(matching_caps[:5])  # Show first 5
                reasons.append(
                    f"✓ Capabilities: {len(matching_caps)}/{len(user_capabilities)} match ({cap_list})"
                )
                confidence_factors.append(0.85)
            else:
                if matching_caps:
                    reasons.append(
                        f"✓ Capabilities: {len(matching_caps)}/{len(user_capabilities)} match"
                    )
                else:
                    reasons.append(
                        f"✗ Capabilities: No matches (need {min_capabilities}+)"
                    )

                if len(user_capabilities) > 0:
                    missing_caps = [
                        cap
                        for cap in user_capabilities
                        if cap.lower() not in requirements_lower
                    ][:3]
                    if missing_caps:
                        reasons.append(
                            f"✗ Missing capabilities: {', '.join(missing_caps)}"
                        )

                qualifies = False
                confidence_factors.append(0.6)
        elif criteria:
            reasons.append("✗ No tender requirements available to match capabilities")
            qualifies = False
            confidence_factors.append(0.4)
        else:
            reasons.append("✗ No user criteria configured")
            qualifies = False
            confidence_factors.append(0.3)

        # 3. Value range
        if criteria and tender.estimated_value:
            value_min = criteria.value_range.get("min", 0) if criteria.value_range else 0
            value_max = criteria.value_range.get("max") if criteria.value_range else None

            tender_value = float(tender.estimated_value)

            if value_min <= tender_value:
                if value_max is None or tender_value <= value_max:
                    value_str = f"R{int(tender_value):,}"
                    range_str = (
                        f"R{int(value_min):,}-R{int(value_max):,}"
                        if value_max
                        else f"R{int(value_min):,}+"
                    )
                    reasons.append(f"✓ Value: {value_str} (Within range {range_str})")
                    confidence_factors.append(0.9)
                else:
                    reasons.append(
                        f"✗ Value: R{int(tender_value):,} (Above max R{int(value_max):,})"
                    )
                    qualifies = False
                    confidence_factors.append(0.7)
            else:
                reasons.append(
                    f"✗ Value: R{int(tender_value):,} (Below min R{int(value_min):,})"
                )
                qualifies = False
                confidence_factors.append(0.7)
        elif tender.estimated_value:
            reasons.append("✓ Value: Tender has estimated value (no range check)")
            confidence_factors.append(0.5)
        else:
            reasons.append("~ Value: Unknown (tender has no estimated value)")
            # Don't disqualify, but note uncertainty
            confidence_factors.append(0.6)

        # 4. Deadline feasibility
        if tender.deadline:
            now = datetime.now(timezone.utc)
            deadline_aware = tender.deadline.replace(tzinfo=timezone.utc) if tender.deadline.tzinfo is None else tender.deadline
            days_remaining = (deadline_aware - now).days

            if days_remaining >= min_deadline_days:
                reasons.append(f"✓ Deadline: {days_remaining} days (Feasible)")
                confidence_factors.append(0.95)
            else:
                reasons.append(
                    f"✗ Deadline: {days_remaining} days (Too tight - need {min_deadline_days}+ days)"
                )
                qualifies = False
                confidence_factors.append(0.8)
        else:
            reasons.append("~ Deadline: Unknown (tender has no deadline)")
            # Don't disqualify, but note uncertainty
            confidence_factors.append(0.5)

        # Calculate overall confidence
        avg_confidence = (
            sum(confidence_factors) / len(confidence_factors)
            if confidence_factors
            else 0.5
        )

        return QualificationResult(
            qualifies=qualifies,
            score=score_value if score_record else 0,
            reasons=reasons,
            confidence=avg_confidence,
            tender_id=tender_id,
            user_id=user_id,
        )

    async def get_qualifying_tenders(
        self,
        user_id: UUID,
        source_id: Optional[str] = None,
        limit: int = 50,
    ) -> list[Tender]:
        """
        Get list of tenders that qualify for auto-fill.

        Args:
            user_id: User making the request
            source_id: Optional filter by source
            limit: Maximum results to return

        Returns:
            List of qualifying tenders
        """
        # Build query
        query = select(Tender).join(Score).where(
            Score.user_id == user_id, Score.score >= 70  # Min score threshold
        )

        if source_id:
            query = query.where(Tender.source_id == source_id)

        query = query.order_by(Score.score.desc()).limit(limit)

        result = await self.db.execute(query)
        tenders = list(result.scalars().all())

        # Filter by additional criteria (capabilities, deadline)
        qualifying_tenders = []
        for tender in tenders:
            qualification = await self.evaluate_tender(tender.id, user_id)
            if qualification.qualifies:
                qualifying_tenders.append(tender)

        return qualifying_tenders
