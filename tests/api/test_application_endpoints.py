"""
Integration tests for Applications API endpoints.

Tests the full HTTP API for application management:
- POST /api/v1/applications - Create application
- GET /api/v1/applications/{id} - Get application
- PUT /api/v1/applications/{id}/fields - Update fields
- PUT /api/v1/applications/{id}/pricing - Update pricing
- PUT /api/v1/applications/{id}/todos/{todo_id} - Update to-do status
- POST /api/v1/applications/{id}/submit - Submit application
- DELETE /api/v1/applications/{id} - Delete application
"""

import pytest
import pytest_asyncio
from datetime import datetime, timedelta, timezone
from uuid import uuid4
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models import (
    Tender, CompanyProfile, AnswerLibrary,
    DocumentLibrary, TenderApplication
)


@pytest_asyncio.fixture
async def test_company_profile(db: AsyncSession, test_user) -> CompanyProfile:
    """Create a test company profile."""
    profile = CompanyProfile(
        user_id=test_user.id,
        legal_name="Test Company Ltd",
        industry="Information Technology",
        capabilities=["Software Development", "Cloud Infrastructure"],
        preferred_sectors=["IT"],
        value_range_min=100000.0,
        value_range_max=5000000.0,
    )
    db.add(profile)
    await db.commit()
    await db.refresh(profile)
    return profile


@pytest_asyncio.fixture
async def qualified_tender(db: AsyncSession, test_source) -> Tender:
    """Create a qualified tender."""
    future_date = datetime.now(timezone.utc) + timedelta(days=14)
    tender = Tender(
        id=uuid4(),
        source_id=test_source.id,
        pdf_url="https://test.example.com/qualified.pdf",
        title="Qualified Tender",
        reference="QUAL-001",
        estimated_value=500000.0,
        deadline=future_date,
        requirements_summary="Software development and cloud infrastructure required"
    )
    db.add(tender)
    await db.commit()
    await db.refresh(tender)
    return tender


@pytest_asyncio.fixture
async def test_application(
    db: AsyncSession,
    qualified_tender: Tender,
    test_company_profile: CompanyProfile
) -> TenderApplication:
    """Create a test application."""
    application = TenderApplication(
        id=uuid4(),
        tender_id=qualified_tender.id,
        user_id=test_company_profile.user_id,
        status="draft",
        auto_filled_fields={
            "company_name": {
                "value": "Test Company Ltd",
                "auto_filled": True,
                "source": "Company Profile"
            }
        },
        pricing_data=None,
        to_do_items=[
            {
                "id": "1",
                "category": "pricing",
                "priority": "high",
                "title": "Provide pricing",
                "description": "Add pricing details",
                "completed": False
            }
        ],
    )
    db.add(application)
    await db.commit()
    await db.refresh(application)
    return application


class TestApplicationEndpoints:
    """Integration tests for application endpoints."""

    @pytest.mark.asyncio
    async def test_create_application(
        self,
        client: AsyncClient,
        auth_headers: dict,
        qualified_tender: Tender,
        test_company_profile: CompanyProfile
    ):
        """Test POST /api/v1/applications creates auto-filled application."""
        response = await client.post(
            "/api/v1/applications",
            json={"tender_id": str(qualified_tender.id)},
            headers=auth_headers
        )

        assert response.status_code == 201
        data = response.json()

        assert data["tender_id"] == str(qualified_tender.id)
        assert data["status"] == "draft"
        assert "auto_filled_fields" in data
        assert "to_do_items" in data
        assert "progress" in data

        # Should have auto-filled company name
        assert "company_name" in data["auto_filled_fields"]
        assert data["auto_filled_fields"]["company_name"]["auto_filled"] is True

    @pytest.mark.asyncio
    async def test_create_application_without_auth_fails(
        self,
        client: AsyncClient,
        qualified_tender: Tender
    ):
        """Test that creating application without auth fails."""
        response = await client.post(
            "/api/v1/applications",
            json={"tender_id": str(qualified_tender.id)}
        )

        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_create_application_invalid_tender_fails(
        self,
        client: AsyncClient,
        auth_headers: dict
    ):
        """Test that creating application with invalid tender fails."""
        fake_tender_id = str(uuid4())

        response = await client.post(
            "/api/v1/applications",
            json={"tender_id": fake_tender_id},
            headers=auth_headers
        )

        assert response.status_code == 404

    @pytest.mark.asyncio
    async def test_get_application(
        self,
        client: AsyncClient,
        auth_headers: dict,
        test_application: TenderApplication
    ):
        """Test GET /api/v1/applications/{id} returns application details."""
        response = await client.get(
            f"/api/v1/applications/{test_application.id}",
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()

        assert data["id"] == str(test_application.id)
        assert data["status"] == "draft"
        assert "auto_filled_fields" in data
        assert "to_do_items" in data

    @pytest.mark.asyncio
    async def test_get_application_not_found(
        self,
        client: AsyncClient,
        auth_headers: dict
    ):
        """Test that getting non-existent application returns 404."""
        fake_id = str(uuid4())

        response = await client.get(
            f"/api/v1/applications/{fake_id}",
            headers=auth_headers
        )

        assert response.status_code == 404

    @pytest.mark.asyncio
    async def test_update_fields(
        self,
        client: AsyncClient,
        auth_headers: dict,
        test_application: TenderApplication
    ):
        """Test PUT /api/v1/applications/{id}/fields updates fields."""
        response = await client.put(
            f"/api/v1/applications/{test_application.id}/fields",
            json={
                "field_updates": {
                    "contact_email": "test@example.com",
                    "contact_phone": "+27123456789"
                }
            },
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()

        # Should have updated fields
        assert "contact_email" in data["auto_filled_fields"]
        assert data["auto_filled_fields"]["contact_email"]["value"] == "test@example.com"

    @pytest.mark.asyncio
    async def test_update_pricing(
        self,
        client: AsyncClient,
        auth_headers: dict,
        test_application: TenderApplication
    ):
        """Test PUT /api/v1/applications/{id}/pricing updates pricing data."""
        pricing_data = {
            "items": [
                {"name": "Item 1", "unit_price": 1000.0, "quantity": 5},
                {"name": "Item 2", "unit_price": 2000.0, "quantity": 3}
            ],
            "subtotal": 11000.0,
            "vat": 1650.0,
            "total": 12650.0
        }

        response = await client.put(
            f"/api/v1/applications/{test_application.id}/pricing",
            json={"pricing_data": pricing_data},
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()

        # Should have pricing data
        assert data["pricing_data"] is not None
        assert data["pricing_data"]["total"] == 12650.0

    @pytest.mark.asyncio
    async def test_update_todo_status(
        self,
        client: AsyncClient,
        auth_headers: dict,
        test_application: TenderApplication
    ):
        """Test PUT /api/v1/applications/{id}/todos/{todo_id} marks to-do complete."""
        todo_id = test_application.to_do_items[0]["id"]

        response = await client.put(
            f"/api/v1/applications/{test_application.id}/todos/{todo_id}",
            json={"todo_id": todo_id, "completed": True},
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()

        # Should have updated to-do
        todo = next(t for t in data["to_do_items"] if t["id"] == todo_id)
        assert todo["completed"] is True

    @pytest.mark.asyncio
    async def test_submit_application(
        self,
        client: AsyncClient,
        auth_headers: dict,
        db: AsyncSession,
        qualified_tender: Tender,
        test_company_profile: CompanyProfile
    ):
        """Test POST /api/v1/applications/{id}/submit submits application."""
        # Create application
        application = TenderApplication(
            id=uuid4(),
            tender_id=qualified_tender.id,
            user_id=test_company_profile.user_id,
            status="draft",
            auto_filled_fields={
                "company_name": {
                    "value": "Test Company Ltd",
                    "auto_filled": True,
                    "source": "Company Profile"
                }
            },
            to_do_items=[],
        )
        db.add(application)
        await db.commit()
        await db.refresh(application)

        response = await client.post(
            f"/api/v1/applications/{application.id}/submit",
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()

        # Status should change to submitted
        assert data["status"] == "submitted"
        assert data["submitted_at"] is not None

    @pytest.mark.asyncio
    async def test_delete_application(
        self,
        client: AsyncClient,
        auth_headers: dict,
        test_application: TenderApplication
    ):
        """Test DELETE /api/v1/applications/{id} deletes application."""
        response = await client.delete(
            f"/api/v1/applications/{test_application.id}",
            headers=auth_headers
        )

        assert response.status_code == 204

        # Verify it's deleted
        get_response = await client.get(
            f"/api/v1/applications/{test_application.id}",
            headers=auth_headers
        )
        assert get_response.status_code == 404

    @pytest.mark.asyncio
    async def test_delete_nonexistent_application_fails(
        self,
        client: AsyncClient,
        auth_headers: dict,
    ):
        """Test that deleting non-existent application returns 404."""
        fake_id = str(uuid4())

        response = await client.delete(
            f"/api/v1/applications/{fake_id}",
            headers=auth_headers
        )

        assert response.status_code == 404
