"""Integration tests for tender endpoints."""

from uuid import uuid4

import pytest
import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models import Source, Tender


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
