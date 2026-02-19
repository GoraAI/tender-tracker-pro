"""
Integration tests for Qualification API endpoint.

Tests:
- GET /api/v1/qualification/tenders/{id} - Check qualification
"""

import pytest
import pytest_asyncio
from datetime import datetime, timedelta, timezone
from uuid import uuid4
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models import Tender, CompanyProfile, Score, UserCriteria


@pytest_asyncio.fixture
async def test_company_profile(db: AsyncSession, test_user) -> CompanyProfile:
    """Create a test company profile."""
    profile = CompanyProfile(
        user_id=test_user.id,
        legal_name="Test Company Ltd",
        industry="Information Technology",
        capabilities=["Software Development", "Cloud Infrastructure", "Data Analytics"],
        preferred_sectors=["IT", "Technology"],
        value_range_min=100000.0,
        value_range_max=5000000.0,
    )
    db.add(profile)
    await db.commit()
    await db.refresh(profile)
    return profile


@pytest_asyncio.fixture
async def test_user_criteria(db: AsyncSession, test_user) -> UserCriteria:
    """Create test user criteria for qualification checks."""
    criteria = UserCriteria(
        user_id=test_user.id,
        name="Default Criteria",
        capabilities=["Software Development", "Cloud Infrastructure", "Data Analytics"],
        value_range={"min": 100000.0, "max": 5000000.0},
        keywords=["software", "cloud", "infrastructure"],
        is_default=True,
    )
    db.add(criteria)
    await db.commit()
    await db.refresh(criteria)
    return criteria


@pytest_asyncio.fixture
async def qualified_tender(db: AsyncSession, test_source, test_user) -> Tender:
    """Create a tender that qualifies (with a separate Score record)."""
    future_date = datetime.now(timezone.utc) + timedelta(days=14)
    tender = Tender(
        id=uuid4(),
        source_id=test_source.id,
        pdf_url="https://test.example.com/qualified.pdf",
        title="Qualified Tender",
        reference="QUAL-001",
        estimated_value=500000.0,
        deadline=future_date,
        requirements_summary="We need software development and cloud infrastructure expertise"
    )
    db.add(tender)
    await db.commit()
    await db.refresh(tender)

    # Create separate Score record
    score = Score(
        id=uuid4(),
        tender_id=tender.id,
        user_id=test_user.id,
        score=85,
        explanation="High relevance to user criteria",
    )
    db.add(score)
    await db.commit()

    return tender


@pytest_asyncio.fixture
async def unqualified_tender(db: AsyncSession, test_source, test_user) -> Tender:
    """Create a tender that does not qualify."""
    short_date = datetime.now(timezone.utc) + timedelta(days=3)
    tender = Tender(
        id=uuid4(),
        source_id=test_source.id,
        pdf_url="https://test.example.com/unqualified.pdf",
        title="Unqualified Tender",
        reference="UNQUAL-001",
        estimated_value=500000.0,
        deadline=short_date,  # Too soon
        requirements_summary="We need construction services"  # No capability match
    )
    db.add(tender)
    await db.commit()
    await db.refresh(tender)

    # Create Score below threshold
    score = Score(
        id=uuid4(),
        tender_id=tender.id,
        user_id=test_user.id,
        score=65,
        explanation="Below threshold",
    )
    db.add(score)
    await db.commit()

    return tender


class TestQualificationEndpoints:
    """Integration tests for qualification endpoints."""

    @pytest.mark.asyncio
    async def test_check_qualification_qualified_tender(
        self,
        client: AsyncClient,
        auth_headers: dict,
        qualified_tender: Tender,
        test_company_profile: CompanyProfile,
        test_user_criteria: UserCriteria,
    ):
        """Test qualification check returns qualified=true for qualifying tender."""
        response = await client.get(
            f"/api/v1/qualification/tenders/{qualified_tender.id}",
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()

        assert data["qualifies"] is True
        assert data["score"] == 85
        assert isinstance(data["reasons"], list)
        assert len(data["reasons"]) >= 4  # All 4 factors

        # Check for positive reasons
        reasons_text = " ".join(data["reasons"]).lower()
        assert "score" in reasons_text

    @pytest.mark.asyncio
    async def test_check_qualification_unqualified_tender(
        self,
        client: AsyncClient,
        auth_headers: dict,
        unqualified_tender: Tender,
        test_company_profile: CompanyProfile,
        test_user_criteria: UserCriteria,
    ):
        """Test qualification check returns qualified=false for non-qualifying tender."""
        response = await client.get(
            f"/api/v1/qualification/tenders/{unqualified_tender.id}",
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()

        assert data["qualifies"] is False
        assert data["score"] == 65
        assert isinstance(data["reasons"], list)
        assert len(data["reasons"]) > 0

    @pytest.mark.asyncio
    async def test_check_qualification_without_auth_fails(
        self,
        client: AsyncClient,
        qualified_tender: Tender
    ):
        """Test that checking qualification without auth fails."""
        response = await client.get(
            f"/api/v1/qualification/tenders/{qualified_tender.id}"
        )

        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_check_qualification_invalid_tender_fails(
        self,
        client: AsyncClient,
        auth_headers: dict
    ):
        """Test that checking qualification with invalid tender ID returns not qualified."""
        fake_tender_id = str(uuid4())

        response = await client.get(
            f"/api/v1/qualification/tenders/{fake_tender_id}",
            headers=auth_headers
        )

        # Service returns qualifies=False with "Tender not found" reason (not 404)
        assert response.status_code == 200
        data = response.json()
        assert data["qualifies"] is False
        assert any("not found" in r.lower() for r in data["reasons"])

    @pytest.mark.asyncio
    async def test_check_qualification_without_company_profile_fails(
        self,
        client: AsyncClient,
        db: AsyncSession,
        qualified_tender: Tender
    ):
        """Test that checking qualification without company profile fails."""
        # Create user without company profile
        from src.api.models import User
        from src.api.services.auth_service import hash_password, create_access_token

        user_no_profile = User(
            id=uuid4(),
            email="noprofile@example.com",
            password_hash=hash_password("password"),
            full_name="No Profile User",
            is_active=True
        )
        db.add(user_no_profile)
        await db.commit()

        # Get auth headers for this user
        token = create_access_token(user_no_profile.id, user_no_profile.email)
        headers = {"Authorization": f"Bearer {token}"}

        response = await client.get(
            f"/api/v1/qualification/tenders/{qualified_tender.id}",
            headers=headers
        )

        # Should return unqualified with reason (no criteria configured)
        assert response.status_code == 200
        data = response.json()
        assert data["qualifies"] is False

    @pytest.mark.asyncio
    async def test_check_qualification_with_custom_thresholds(
        self,
        client: AsyncClient,
        auth_headers: dict,
        qualified_tender: Tender,
        test_company_profile: CompanyProfile,
        test_user_criteria: UserCriteria,
    ):
        """Test qualification check with custom threshold parameters."""
        # Use higher thresholds
        response = await client.get(
            f"/api/v1/qualification/tenders/{qualified_tender.id}",
            params={
                "min_score": 90,  # Score is 85
                "min_capabilities": 2,
                "min_deadline_days": 7
            },
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()

        # Should not qualify with higher score threshold
        assert data["qualifies"] is False
        assert data["score"] == 85

    @pytest.mark.asyncio
    async def test_check_qualification_response_structure(
        self,
        client: AsyncClient,
        auth_headers: dict,
        qualified_tender: Tender,
        test_company_profile: CompanyProfile,
        test_user_criteria: UserCriteria,
    ):
        """Test that qualification response has correct structure."""
        response = await client.get(
            f"/api/v1/qualification/tenders/{qualified_tender.id}",
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()

        # Required fields
        assert "qualifies" in data
        assert "score" in data
        assert "reasons" in data

        # Types
        assert isinstance(data["qualifies"], bool)
        assert isinstance(data["score"], (int, float))
        assert isinstance(data["reasons"], list)

        # Reasons should be non-empty strings
        for reason in data["reasons"]:
            assert isinstance(reason, str)
            assert len(reason) > 0

    @pytest.mark.asyncio
    async def test_check_qualification_tender_without_score(
        self,
        client: AsyncClient,
        auth_headers: dict,
        db: AsyncSession,
        test_source,
        test_company_profile: CompanyProfile,
        test_user_criteria: UserCriteria,
    ):
        """Test qualification check for tender without score."""
        future_date = datetime.now(timezone.utc) + timedelta(days=14)
        tender_no_score = Tender(
            id=uuid4(),
            source_id=test_source.id,
            pdf_url="https://test.example.com/noscore.pdf",
            title="No Score Tender",
            reference="NOSCORE-001",
            estimated_value=500000.0,
            deadline=future_date,
            requirements_summary="Software development required"
        )
        db.add(tender_no_score)
        await db.commit()

        response = await client.get(
            f"/api/v1/qualification/tenders/{tender_no_score.id}",
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()

        # Should not qualify without score
        assert data["qualifies"] is False
        assert data["score"] == 0  # Service returns 0 when no score

        # Should mention lack of score
        reasons_text = " ".join(data["reasons"]).lower()
        assert "no score" in reasons_text or "score" in reasons_text

    @pytest.mark.asyncio
    async def test_qualification_reasons_are_descriptive(
        self,
        client: AsyncClient,
        auth_headers: dict,
        qualified_tender: Tender,
        test_company_profile: CompanyProfile,
        test_user_criteria: UserCriteria,
    ):
        """Test that qualification reasons are descriptive and helpful."""
        response = await client.get(
            f"/api/v1/qualification/tenders/{qualified_tender.id}",
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()

        # Each reason should be descriptive (>10 characters)
        for reason in data["reasons"]:
            assert len(reason) > 10, f"Reason too short: {reason}"

    @pytest.mark.asyncio
    async def test_multiple_qualification_checks_consistent(
        self,
        client: AsyncClient,
        auth_headers: dict,
        qualified_tender: Tender,
        test_company_profile: CompanyProfile,
        test_user_criteria: UserCriteria,
    ):
        """Test that multiple qualification checks return consistent results."""
        # First check
        response1 = await client.get(
            f"/api/v1/qualification/tenders/{qualified_tender.id}",
            headers=auth_headers
        )

        # Second check (immediately after)
        response2 = await client.get(
            f"/api/v1/qualification/tenders/{qualified_tender.id}",
            headers=auth_headers
        )

        assert response1.status_code == 200
        assert response2.status_code == 200

        data1 = response1.json()
        data2 = response2.json()

        # Should be identical
        assert data1["qualifies"] == data2["qualifies"]
        assert data1["score"] == data2["score"]
        # Reasons might vary in order, but should have same count
        assert len(data1["reasons"]) == len(data2["reasons"])
