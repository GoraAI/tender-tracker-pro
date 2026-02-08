"""
Unit tests for QualificationService.

Tests the multi-factor qualification logic:
- Score threshold (≥70)
- Capability matching (≥2 matches)
- Value range validation
- Deadline feasibility (≥7 days)
"""

import pytest
import pytest_asyncio
from datetime import datetime, timedelta, timezone
from uuid import uuid4
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models import Tender, Score, UserCriteria
from src.api.services.qualification_service import QualificationService


@pytest_asyncio.fixture
async def qualification_service(db: AsyncSession):
    """Create QualificationService instance."""
    return QualificationService(db)


@pytest_asyncio.fixture
async def test_user_criteria(db: AsyncSession, test_user) -> UserCriteria:
    """Create test user criteria."""
    criteria = UserCriteria(
        user_id=test_user.id,
        name="Default Criteria",
        capabilities=["Software Development", "Cloud Infrastructure", "Data Analytics"],
        value_range={"min": 100000.0, "max": 5000000.0},
        is_default=True
    )
    db.add(criteria)
    await db.commit()
    await db.refresh(criteria)
    return criteria


@pytest_asyncio.fixture
async def qualified_tender(db: AsyncSession, test_source) -> Tender:
    """Create a tender that should qualify."""
    future_date = datetime.now(timezone.utc) + timedelta(days=14)
    tender = Tender(
        id=uuid4(),
        source_id=test_source.id,
        pdf_url="https://test.example.com/qualified.pdf",
        title="Qualified Tender",
        reference="QUAL-001",
        estimated_value=500000.0,
        deadline=future_date,
        requirements_summary="We need software development and cloud infrastructure services"
    )
    db.add(tender)
    await db.commit()
    await db.refresh(tender)
    return tender


@pytest_asyncio.fixture
async def qualified_score(db: AsyncSession, qualified_tender: Tender, test_user) -> Score:
    """Create a qualifying score."""
    score = Score(
        id=uuid4(),
        tender_id=qualified_tender.id,
        user_id=test_user.id,
        score=85,
        criteria_id=None
    )
    db.add(score)
    await db.commit()
    await db.refresh(score)
    return score


@pytest_asyncio.fixture
async def low_score_tender(db: AsyncSession, test_source, test_user) -> tuple[Tender, Score]:
    """Create a tender with low score."""
    future_date = datetime.now(timezone.utc) + timedelta(days=14)
    tender = Tender(
        id=uuid4(),
        source_id=test_source.id,
        pdf_url="https://test.example.com/lowscore.pdf",
        title="Low Score Tender",
        reference="LOW-001",
        estimated_value=500000.0,
        deadline=future_date,
        requirements_summary="We need software development services"
    )
    db.add(tender)
    await db.commit()
    await db.refresh(tender)

    score = Score(
        id=uuid4(),
        tender_id=tender.id,
        user_id=test_user.id,
        score=65,
        criteria_id=None
    )
    db.add(score)
    await db.commit()
    await db.refresh(score)

    return tender, score


@pytest_asyncio.fixture
async def short_deadline_tender(db: AsyncSession, test_source, test_user) -> tuple[Tender, Score]:
    """Create a tender with short deadline."""
    short_date = datetime.now(timezone.utc) + timedelta(days=5)
    tender = Tender(
        id=uuid4(),
        source_id=test_source.id,
        pdf_url="https://test.example.com/short.pdf",
        title="Short Deadline Tender",
        reference="SHORT-001",
        estimated_value=500000.0,
        deadline=short_date,
        requirements_summary="We need software development and cloud infrastructure services"
    )
    db.add(tender)
    await db.commit()
    await db.refresh(tender)

    score = Score(
        id=uuid4(),
        tender_id=tender.id,
        user_id=test_user.id,
        score=85,
        criteria_id=None
    )
    db.add(score)
    await db.commit()
    await db.refresh(score)

    return tender, score


class TestQualificationService:
    """Test cases for QualificationService."""

    @pytest.mark.asyncio
    async def test_qualified_tender_all_criteria_met(
        self,
        qualification_service: QualificationService,
        qualified_tender: Tender,
        qualified_score: Score,
        test_user_criteria: UserCriteria,
        test_user
    ):
        """Test that a tender meeting all criteria qualifies."""
        result = await qualification_service.evaluate_tender(
            tender_id=qualified_tender.id,
            user_id=test_user.id
        )

        assert result.qualifies is True
        assert result.score == 85
        assert len(result.reasons) >= 4  # All 4 factors should pass

    @pytest.mark.asyncio
    async def test_low_score_disqualifies(
        self,
        qualification_service: QualificationService,
        low_score_tender: tuple[Tender, Score],
        test_user_criteria: UserCriteria,
        test_user
    ):
        """Test that score below 70 disqualifies tender."""
        tender, score = low_score_tender

        result = await qualification_service.evaluate_tender(
            tender_id=tender.id,
            user_id=test_user.id
        )

        assert result.qualifies is False
        assert result.score == 65

    @pytest.mark.asyncio
    async def test_short_deadline_disqualifies(
        self,
        qualification_service: QualificationService,
        short_deadline_tender: tuple[Tender, Score],
        test_user_criteria: UserCriteria,
        test_user
    ):
        """Test that deadline < 7 days disqualifies tender."""
        tender, score = short_deadline_tender

        result = await qualification_service.evaluate_tender(
            tender_id=tender.id,
            user_id=test_user.id,
            min_deadline_days=7
        )

        assert result.qualifies is False

    @pytest.mark.asyncio
    async def test_missing_score_disqualifies(
        self,
        qualification_service: QualificationService,
        qualified_tender: Tender,
        test_user_criteria: UserCriteria,
        test_user
    ):
        """Test that missing score disqualifies tender."""
        # Don't create score for this test
        result = await qualification_service.evaluate_tender(
            tender_id=qualified_tender.id,
            user_id=test_user.id
        )

        assert result.qualifies is False
        assert result.score == 0

    @pytest.mark.asyncio
    async def test_missing_criteria_disqualifies(
        self,
        qualification_service: QualificationService,
        qualified_tender: Tender,
        qualified_score: Score,
        test_user
    ):
        """Test that missing user criteria disqualifies tender."""
        # Don't create criteria for this test
        result = await qualification_service.evaluate_tender(
            tender_id=qualified_tender.id,
            user_id=test_user.id
        )

        assert result.qualifies is False

    @pytest.mark.asyncio
    async def test_tender_not_found(
        self,
        qualification_service: QualificationService,
        test_user
    ):
        """Test that non-existent tender returns not qualified."""
        fake_tender_id = uuid4()

        result = await qualification_service.evaluate_tender(
            tender_id=fake_tender_id,
            user_id=test_user.id
        )

        assert result.qualifies is False
        assert "not found" in result.reasons[0].lower()
