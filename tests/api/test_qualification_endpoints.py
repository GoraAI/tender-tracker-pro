"""
Integration tests for Qualification API endpoint.

Tests:
- GET /api/v1/qualification/tenders/{id} - Check qualification
"""

import pytest
import pytest_asyncio
from datetime import datetime, timedelta
from uuid import uuid4
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models import Tender, CompanyProfile


@pytest_asyncio.fixture
async def test_company_profile(db: AsyncSession, test_user) -> CompanyProfile:
    """Create a test company profile."""
    profile = CompanyProfile(
        user_id=test_user.id,
        legal_name="Test Company Ltd",
        capabilities=["Software Development", "Cloud Infrastructure", "Data Analytics"],
        preferred_sectors=["IT", "Technology"],
        min_value=100000.0,
        max_value=5000000.0
    )
    db.add(profile)
    await db.commit()
    await db.refresh(profile)
    return profile


@pytest_asyncio.fixture
async def qualified_tender(db: AsyncSession, test_source) -> Tender:
    """Create a tender that qualifies."""
    future_date = datetime.utcnow() + timedelta(days=14)
    tender = Tender(
        id=uuid4(),
        source_id=test_source.id,
        pdf_url="https://test.example.com/qualified.pdf",
        title="Qualified Tender",
        reference="QUAL-001",
        score=85,
        estimated_value=500000.0,
        deadline=future_date.isoformat(),
        requirements_summary="We need software development and cloud infrastructure expertise"
    )
    db.add(tender)
    await db.commit()
    await db.refresh(tender)
    return tender


@pytest_asyncio.fixture
async def unqualified_tender(db: AsyncSession, test_source) -> Tender:
    """Create a tender that does not qualify."""
    short_date = datetime.utcnow() + timedelta(days=3)
    tender = Tender(
        id=uuid4(),
        source_id=test_source.id,
        pdf_url="https://test.example.com/unqualified.pdf",
        title="Unqualified Tender",
        reference="UNQUAL-001",
        score=65,  # Below threshold
        estimated_value=500000.0,
        deadline=short_date.isoformat(),  # Too soon
        requirements_summary="We need construction services"  # No capability match
    )
    db.add(tender)
    await db.commit()
    await db.refresh(tender)
    return tender


class TestQualificationEndpoints:
    """Integration tests for qualification endpoints."""

    @pytest.mark.asyncio
    async def test_check_qualification_qualified_tender(
        self,
        client: AsyncClient,
        auth_headers: dict,
        qualified_tender: Tender,
        test_company_profile: CompanyProfile
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
        assert len(data["reasons"]) == 4  # All 4 factors

        # Check for positive reasons
        reasons_text = " ".join(data["reasons"]).lower()
        assert "score" in reasons_text
        assert "capabilities" in reasons_text or "capability" in reasons_text
        assert "value" in reasons_text
        assert "deadline" in reasons_text

    @pytest.mark.asyncio
    async def test_check_qualification_unqualified_tender(
        self,
        client: AsyncClient,
        auth_headers: dict,
        unqualified_tender: Tender,
        test_company_profile: CompanyProfile
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

        # Should mention why it doesn't qualify
        reasons_text = " ".join(data["reasons"]).lower()
        assert "below threshold" in reasons_text or "threshold" in reasons_text

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
        """Test that checking qualification with invalid tender ID fails."""
        fake_tender_id = str(uuid4())

        response = await client.get(
            f"/api/v1/qualification/tenders/{fake_tender_id}",
            headers=auth_headers
        )

        assert response.status_code == 404

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

        # Should return unqualified with reason
        assert response.status_code == 200
        data = response.json()
        assert data["qualifies"] is False
        reasons_text = " ".join(data["reasons"]).lower()
        assert "company profile" in reasons_text or "profile" in reasons_text

    @pytest.mark.asyncio
    async def test_check_qualification_with_custom_thresholds(
        self,
        client: AsyncClient,
        auth_headers: dict,
        qualified_tender: Tender,
        test_company_profile: CompanyProfile
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

        # Should mention score threshold
        reasons_text = " ".join(data["reasons"]).lower()
        assert "85" in reasons_text
        assert "90" in reasons_text or "threshold" in reasons_text

    @pytest.mark.asyncio
    async def test_check_qualification_response_structure(
        self,
        client: AsyncClient,
        auth_headers: dict,
        qualified_tender: Tender,
        test_company_profile: CompanyProfile
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
        assert isinstance(data["score"], (int, float)) or data["score"] is None
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
        test_company_profile: CompanyProfile
    ):
        """Test qualification check for tender without score."""
        future_date = datetime.utcnow() + timedelta(days=14)
        tender_no_score = Tender(
            id=uuid4(),
            source_id=test_source.id,
            pdf_url="https://test.example.com/noscore.pdf",
            title="No Score Tender",
            reference="NOSCORE-001",
            score=None,  # No score
            estimated_value=500000.0,
            deadline=future_date.isoformat(),
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
        assert data["score"] is None

        # Should mention lack of score
        reasons_text = " ".join(data["reasons"]).lower()
        assert "no score" in reasons_text or "score" in reasons_text

    @pytest.mark.asyncio
    async def test_qualification_reasons_are_descriptive(
        self,
        client: AsyncClient,
        auth_headers: dict,
        qualified_tender: Tender,
        test_company_profile: CompanyProfile
    ):
        """Test that qualification reasons are descriptive and helpful."""
        response = await client.get(
            f"/api/v1/qualification/tenders/{qualified_tender.id}",
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()

        # Each reason should be descriptive (>20 characters)
        for reason in data["reasons"]:
            assert len(reason) > 20, f"Reason too short: {reason}"

            # Should contain numbers or specific details
            has_details = any(char.isdigit() for char in reason)
            assert has_details, f"Reason lacks specific details: {reason}"

    @pytest.mark.asyncio
    async def test_multiple_qualification_checks_consistent(
        self,
        client: AsyncClient,
        auth_headers: dict,
        qualified_tender: Tender,
        test_company_profile: CompanyProfile
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
