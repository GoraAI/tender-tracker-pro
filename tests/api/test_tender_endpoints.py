"""Integration tests for tender endpoints."""

import asyncio
from uuid import uuid4

import pytest
import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models import Source, Tender
from src.api.models.score import Score


@pytest_asyncio.fixture
async def test_tenders(db: AsyncSession, test_source: Source) -> list[Tender]:
    """Create test tenders."""
    tenders = [
        Tender(
            id=uuid4(),
            source_id=test_source.id,
            pdf_url=f"https://example.com/tender{i}.pdf",
            title=f"Test Tender {i}",
            reference=f"REF-{i:03d}",
        )
        for i in range(5)
    ]
    for tender in tenders:
        db.add(tender)
    await db.commit()
    return tenders


@pytest.mark.asyncio
class TestTenderEndpoints:
    """Tests for /api/v1/tenders/* endpoints."""

    async def test_list_tenders_authenticated(
        self, client: AsyncClient, auth_headers, test_tenders
    ):
        """GET /tenders should return paginated list."""
        response = await client.get("/api/v1/tenders", headers=auth_headers)

        assert response.status_code == 200
        data = response.json()
        assert "items" in data
        assert "total" in data
        assert "page" in data
        assert "per_page" in data
        assert data["total"] == 5

    async def test_list_tenders_unauthenticated(self, client: AsyncClient):
        """GET /tenders should fail without auth."""
        response = await client.get("/api/v1/tenders")

        assert response.status_code == 401

    async def test_list_tenders_pagination(
        self, client: AsyncClient, auth_headers, test_tenders
    ):
        """GET /tenders should support pagination."""
        response = await client.get(
            "/api/v1/tenders?page=1&per_page=2", headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 2
        assert data["page"] == 1
        assert data["per_page"] == 2
        assert data["pages"] == 3  # 5 items / 2 per page = 3 pages

    async def test_list_tenders_search(
        self, client: AsyncClient, auth_headers, test_tenders
    ):
        """GET /tenders should filter by search."""
        response = await client.get(
            "/api/v1/tenders?search=Tender 1", headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 1
        assert "Tender 1" in data["items"][0]["title"]

    async def test_get_tender_detail(
        self, client: AsyncClient, auth_headers, test_tenders
    ):
        """GET /tenders/{id} should return tender details."""
        tender = test_tenders[0]
        response = await client.get(
            f"/api/v1/tenders/{tender.id}", headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert data["id"] == str(tender.id)
        assert data["title"] == tender.title
        assert data["reference"] == tender.reference

    async def test_get_tender_not_found(self, client: AsyncClient, auth_headers):
        """GET /tenders/{id} should return 404 for unknown tender."""
        fake_id = uuid4()
        response = await client.get(
            f"/api/v1/tenders/{fake_id}", headers=auth_headers
        )

        assert response.status_code == 404


@pytest.mark.asyncio
class TestTenderScoring:
    """Tests for /api/v1/tenders/{id}/score endpoint."""

    async def test_score_tender_with_raw_text_accepted(
        self, client: AsyncClient, auth_headers, db: AsyncSession, test_source: Source
    ):
        """POST /tenders/{id}/score should accept a tender with raw_text."""
        tender = Tender(
            id=uuid4(),
            source_id=test_source.id,
            pdf_url="https://example.com/tender.pdf",
            title="Water Infrastructure Tender",
            reference="SCORE-001",
            raw_text="water supply pipe treatment infrastructure maintenance civil",
        )
        db.add(tender)
        await db.commit()

        response = await client.post(
            f"/api/v1/tenders/{tender.id}/score", headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["score_explanation"] == "Scoring queued. Check back in a few moments."

    async def test_score_tender_with_only_requirements_summary_accepted(
        self, client: AsyncClient, auth_headers, db: AsyncSession, test_source: Source
    ):
        """POST /tenders/{id}/score should accept a tender with only requirements_summary."""
        tender = Tender(
            id=uuid4(),
            source_id=test_source.id,
            pdf_url="https://example.com/tender.pdf",
            title="Construction Tender",
            reference="SCORE-002",
            raw_text=None,
            requirements_summary="water supply pipe treatment infrastructure",
        )
        db.add(tender)
        await db.commit()

        # Should NOT return 400 — requirements_summary is sufficient
        response = await client.post(
            f"/api/v1/tenders/{tender.id}/score", headers=auth_headers
        )
        assert response.status_code == 200

    async def test_score_tender_no_text_returns_400(
        self, client: AsyncClient, auth_headers, db: AsyncSession, test_source: Source
    ):
        """POST /tenders/{id}/score should return 400 when no text available."""
        tender = Tender(
            id=uuid4(),
            source_id=test_source.id,
            pdf_url="https://example.com/tender.pdf",
            title="Empty Tender",
            reference="SCORE-004",
            raw_text=None,
            requirements_summary=None,
        )
        db.add(tender)
        await db.commit()

        response = await client.post(
            f"/api/v1/tenders/{tender.id}/score", headers=auth_headers
        )
        assert response.status_code == 400

    async def test_score_tender_not_found(
        self, client: AsyncClient, auth_headers
    ):
        """POST /tenders/{id}/score should return 404 for unknown tender."""
        fake_id = uuid4()
        response = await client.post(
            f"/api/v1/tenders/{fake_id}/score", headers=auth_headers
        )
        assert response.status_code == 404

    async def test_score_upsert_logic_direct(
        self, db: AsyncSession, test_source: Source
    ):
        """Score upsert: inserting same (tender_id, user_id, criteria_id) should update."""
        from src.api.models.user import User
        from src.api.services.auth_service import hash_password

        user = User(
            id=uuid4(),
            email="scorer@test.com",
            password_hash=hash_password("testpass"),
            full_name="Scorer",
            is_active=True,
        )
        db.add(user)

        tender = Tender(
            id=uuid4(),
            source_id=test_source.id,
            pdf_url="https://example.com/t.pdf",
            title="Upsert Test",
            reference="UPSERT-001",
            raw_text="test content",
        )
        db.add(tender)
        await db.commit()

        # Insert first score
        score1 = Score(
            tender_id=tender.id,
            user_id=user.id,
            criteria_id=None,
            score=50,
            explanation="First score",
            relevance_category="Moderate",
        )
        db.add(score1)
        await db.commit()

        # Simulate upsert: check for existing, then update
        existing_result = await db.execute(
            select(Score).where(
                Score.tender_id == tender.id,
                Score.user_id == user.id,
                Score.criteria_id.is_(None),
            )
        )
        existing = existing_result.scalar_one_or_none()
        assert existing is not None

        # Update existing
        existing.score = 85
        existing.explanation = "Updated score"
        existing.relevance_category = "Good"
        await db.commit()

        # Verify only 1 record and it's updated
        all_result = await db.execute(
            select(Score).where(Score.tender_id == tender.id)
        )
        all_scores = list(all_result.scalars().all())
        assert len(all_scores) == 1
        assert all_scores[0].score == 85
        assert all_scores[0].explanation == "Updated score"
