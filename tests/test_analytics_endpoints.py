"""TDD Tests for Analytics API Endpoints - GREEN Phase.

These tests verify:
1. Endpoints return EMPTY data when user has no records (NOT mock data)
2. CRUD operations persist to database
3. Analytics are calculated from real user data

NO MOCK DATA - all data comes from user input.
"""

import pytest
from datetime import date, timedelta
from uuid import uuid4
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models import Tender


class TestComplianceEndpoints:
    """Tests for /analytics/compliance endpoints."""

    @pytest.mark.asyncio
    async def test_get_compliance_empty_for_new_user(self, client: AsyncClient, auth_headers: dict):
        """New user should get empty compliance list, NOT mock data."""
        response = await client.get("/api/v1/analytics/compliance", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()

        # Should be empty, not 6 hardcoded items
        assert data["items"] == []
        assert data["valid_count"] == 0
        assert data["total_count"] == 0
        assert data["is_empty"] is True

    @pytest.mark.asyncio
    async def test_create_compliance_registration(self, client: AsyncClient, auth_headers: dict):
        """Should create and persist compliance registration."""
        payload = {
            "type": "bbbee",
            "name": "B-BBEE Certificate",
            "short_name": "B-BBEE",
            "registration_number": "BBBEE-2025-78901",
            "issue_date": "2025-01-29",
            "expiry_date": "2026-01-29",
            "expiry_type": "annual",
            "renewal_url": None
        }
        response = await client.post(
            "/api/v1/analytics/compliance",
            json=payload,
            headers=auth_headers
        )
        assert response.status_code == 201
        data = response.json()

        # Should have real UUID ID, not "bbbee" or hardcoded
        assert data["id"] is not None
        assert data["id"] != "bbbee"
        assert data["type"] == "bbbee"
        assert data["registration_number"] == "BBBEE-2025-78901"

    @pytest.mark.asyncio
    async def test_compliance_persists_to_database(self, client: AsyncClient, auth_headers: dict):
        """Created compliance should be retrievable."""
        # Create
        payload = {
            "type": "cidb",
            "name": "CIDB Registration",
            "short_name": "CIDB",
            "registration_number": "CIDB-123456",
            "issue_date": "2025-03-01",
            "expiry_date": "2026-03-01",
            "expiry_type": "annual"
        }
        create_response = await client.post(
            "/api/v1/analytics/compliance",
            json=payload,
            headers=auth_headers
        )
        created_id = create_response.json()["id"]

        # Retrieve
        get_response = await client.get("/api/v1/analytics/compliance", headers=auth_headers)
        data = get_response.json()

        assert len(data["items"]) >= 1
        item_ids = [item["id"] for item in data["items"]]
        assert created_id in item_ids

    @pytest.mark.asyncio
    async def test_update_compliance_registration(self, client: AsyncClient, auth_headers: dict):
        """Should update compliance registration."""
        # First create
        create_payload = {
            "type": "bbbee",
            "name": "B-BBEE Certificate",
            "short_name": "B-BBEE",
            "registration_number": "OLD-NUMBER",
            "issue_date": "2025-01-29",
            "expiry_date": "2026-01-29",
            "expiry_type": "annual"
        }
        create_response = await client.post(
            "/api/v1/analytics/compliance",
            json=create_payload,
            headers=auth_headers
        )
        item_id = create_response.json()["id"]

        # Update - must include all required fields
        update_payload = {
            "type": "bbbee",
            "name": "B-BBEE Certificate",
            "short_name": "B-BBEE",
            "registration_number": "NEW-NUMBER",
            "issue_date": "2025-01-29",
            "expiry_date": "2027-01-29",
            "expiry_type": "annual"
        }
        update_response = await client.put(
            f"/api/v1/analytics/compliance/{item_id}",
            json=update_payload,
            headers=auth_headers
        )
        assert update_response.status_code == 200
        data = update_response.json()
        assert data["registration_number"] == "NEW-NUMBER"

    @pytest.mark.asyncio
    async def test_delete_compliance_registration(self, client: AsyncClient, auth_headers: dict):
        """Should delete compliance registration."""
        # Create
        payload = {
            "type": "uif",
            "name": "UIF Letter",
            "short_name": "UIF",
            "registration_number": "UIF-123",
            "issue_date": "2025-01-01",
            "expiry_date": "2026-01-01",
            "expiry_type": "annual"
        }
        create_response = await client.post(
            "/api/v1/analytics/compliance",
            json=payload,
            headers=auth_headers
        )
        item_id = create_response.json()["id"]

        # Delete
        delete_response = await client.delete(
            f"/api/v1/analytics/compliance/{item_id}",
            headers=auth_headers
        )
        assert delete_response.status_code == 204


class TestCostEndpoints:
    """Tests for /analytics/costs endpoints."""

    @pytest.mark.asyncio
    async def test_get_costs_empty_for_new_user(self, client: AsyncClient, auth_headers: dict):
        """New user should get empty costs, NOT mock data."""
        response = await client.get("/api/v1/analytics/costs", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()

        # Should be empty categories with zero amounts
        assert data["total_spent"] == 0
        assert data["is_empty"] is True
        # Categories should exist but be empty
        for category in data["categories"]:
            assert category["amount"] == 0
            assert category["items"] == []

    @pytest.mark.asyncio
    async def test_create_cost_item(self, client: AsyncClient, auth_headers: dict):
        """Should create and persist cost item."""
        payload = {
            "category": "preparation",
            "description": "Staff time - Test Tender",
            "amount": 36000.00,
            "date": "2025-11-15",
            "tender_id": None,
            "recurring": False
        }
        response = await client.post(
            "/api/v1/analytics/costs",
            json=payload,
            headers=auth_headers
        )
        assert response.status_code == 201
        data = response.json()

        # Should have real UUID ID, not "cost_001"
        assert data["id"] is not None
        assert data["id"] != "cost_001"
        # Should have real user_id, not "user_001"
        assert data.get("user_id") is not None
        assert data.get("user_id") != "user_001"

    @pytest.mark.asyncio
    async def test_cost_aggregation(self, client: AsyncClient, auth_headers: dict):
        """Costs should be aggregated by category."""
        # Use recent dates within the 12-month period
        today = date.today()
        # Create multiple costs
        costs = [
            {"category": "preparation", "description": "Item 1", "amount": 1000.00, "date": str(today - timedelta(days=10))},
            {"category": "preparation", "description": "Item 2", "amount": 2000.00, "date": str(today - timedelta(days=5))},
            {"category": "compliance", "description": "Item 3", "amount": 500.00, "date": str(today - timedelta(days=2))},
        ]
        for cost in costs:
            await client.post("/api/v1/analytics/costs", json=cost, headers=auth_headers)

        # Get aggregated
        response = await client.get("/api/v1/analytics/costs", headers=auth_headers)
        data = response.json()

        assert data["total_spent"] == 3500.00

        prep_category = next(c for c in data["categories"] if c["name"] == "preparation")
        assert prep_category["amount"] == 3000.00
        assert len(prep_category["items"]) == 2

    @pytest.mark.asyncio
    async def test_delete_cost_item(self, client: AsyncClient, auth_headers: dict):
        """Should delete cost item."""
        # Create
        payload = {
            "category": "platform",
            "description": "Test",
            "amount": 100.00,
            "date": "2025-01-01"
        }
        create_response = await client.post(
            "/api/v1/analytics/costs",
            json=payload,
            headers=auth_headers
        )
        item_id = create_response.json()["id"]

        # Delete
        delete_response = await client.delete(
            f"/api/v1/analytics/costs/{item_id}",
            headers=auth_headers
        )
        assert delete_response.status_code == 204


class TestFunnelEndpoints:
    """Tests for /analytics/funnel endpoints."""

    @pytest.mark.asyncio
    async def test_get_funnel_empty_for_new_user(self, client: AsyncClient, auth_headers: dict):
        """New user should get zero funnel, NOT hardcoded 45/28/18/6."""
        response = await client.get("/api/v1/analytics/funnel", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()

        # Should be zeros, not hardcoded mock data
        assert data["stages"][0]["name"] == "Discovered"
        assert data["stages"][0]["count"] == 0  # NOT 45
        assert data["stages"][1]["name"] == "Qualified"
        assert data["stages"][1]["count"] == 0  # NOT 28
        assert data["stages"][2]["name"] == "Submitted"
        assert data["stages"][2]["count"] == 0  # NOT 18
        assert data["stages"][3]["name"] == "Awarded"
        assert data["stages"][3]["count"] == 0  # NOT 6
        assert data["is_empty"] is True


class TestOutcomeEndpoints:
    """Tests for /analytics/outcomes endpoints."""

    @pytest.mark.asyncio
    async def test_get_outcome_analytics_empty(self, client: AsyncClient, auth_headers: dict):
        """New user should get zero outcomes, NOT hardcoded stats."""
        response = await client.get("/api/v1/analytics/outcomes", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()

        # Should be zeros, not 18/6/12
        assert data["total_submitted"] == 0  # NOT 18
        assert data["total_won"] == 0  # NOT 6
        assert data["total_lost"] == 0  # NOT 12
        assert data["is_empty"] is True

    @pytest.mark.asyncio
    async def test_create_outcome_with_tender(
        self, client: AsyncClient, auth_headers: dict, test_tender: Tender
    ):
        """Should create and persist tender outcome."""
        payload = {
            "tender_id": str(test_tender.id),
            "outcome": "won",
            "reason_code": None,
            "reason_details": None,
            "award_date": "2025-05-20",
            "award_amount": 2750000.00
        }
        response = await client.post(
            "/api/v1/analytics/outcomes",
            json=payload,
            headers=auth_headers
        )
        assert response.status_code == 201
        data = response.json()

        # Should have real UUID ID, not "outcome_001"
        assert data["id"] is not None
        assert data["id"] != "outcome_001"
        # Should have real user_id
        assert data.get("user_id") is not None
        assert data.get("user_id") != "user_001"


class TestPipelineEndpoints:
    """Tests for /analytics/pipeline endpoints."""

    @pytest.mark.asyncio
    async def test_get_pipeline_empty_for_new_user(self, client: AsyncClient, auth_headers: dict):
        """New user should get empty pipeline, NOT mock tenders."""
        response = await client.get("/api/v1/analytics/pipeline", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()

        assert data["tenders"] == []
        assert data["active_count"] == 0
        assert data["is_empty"] is True

    @pytest.mark.asyncio
    async def test_add_to_pipeline_with_tender(
        self, client: AsyncClient, auth_headers: dict, test_tender: Tender
    ):
        """Should add tender to pipeline and persist."""
        payload = {
            "tender_id": str(test_tender.id),
            "status": "preparation",
            "assigned_to": "Test User"
        }
        response = await client.post(
            "/api/v1/analytics/pipeline",
            json=payload,
            headers=auth_headers
        )
        assert response.status_code == 201
        data = response.json()

        # Should have real UUID ID, not "pipeline_001"
        assert data["id"] is not None
        assert data["id"] != "pipeline_001"
        # Should have real tender info
        assert data["title"] == test_tender.title
        assert data["reference"] == test_tender.reference

    @pytest.mark.asyncio
    async def test_pipeline_persists_and_retrieves(
        self, client: AsyncClient, auth_headers: dict, test_tender: Tender
    ):
        """Added pipeline tender should be retrievable."""
        # Add
        payload = {
            "tender_id": str(test_tender.id),
            "status": "qualified"
        }
        await client.post("/api/v1/analytics/pipeline", json=payload, headers=auth_headers)

        # Get
        response = await client.get("/api/v1/analytics/pipeline", headers=auth_headers)
        data = response.json()

        assert len(data["tenders"]) >= 1
        assert data["active_count"] >= 1


class TestPaymentDelayEndpoints:
    """Tests for /analytics/payment-delays endpoint."""

    @pytest.mark.asyncio
    async def test_get_payment_delays_returns_reference_data(self, client: AsyncClient, auth_headers: dict):
        """Should return National Treasury reference data."""
        response = await client.get("/api/v1/analytics/payment-delays", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()

        # Should have province data (from reference file)
        assert "by_province" in data
        assert "by_department" in data
        assert "overall_average" in data
        assert "reference_data_source" in data

        # Should indicate user has no payment data yet
        assert data["user_data_available"] is False


class TestCompanyProfileEndpoints:
    """Tests for /analytics/profile endpoints."""

    @pytest.mark.asyncio
    async def test_get_profile_empty_for_new_user(self, client: AsyncClient, auth_headers: dict):
        """New user should get null profile, NOT mock data."""
        response = await client.get("/api/v1/analytics/profile", headers=auth_headers)
        assert response.status_code == 200
        # Should be null/None for new user
        data = response.json()
        assert data is None

    @pytest.mark.asyncio
    async def test_create_company_profile(self, client: AsyncClient, auth_headers: dict):
        """Should create company profile."""
        payload = {
            "legal_name": "Test Company (Pty) Ltd",
            "trading_name": "Test Co",
            "industry": "Information Technology",
            "capabilities": ["IT Infrastructure", "Software Development"],
            "preferred_sectors": ["Government - National"],
            "preferred_provinces": ["Gauteng"],
            "keywords": ["IT", "software"],
            "value_range_min": 100000,
            "value_range_max": 5000000
        }
        response = await client.post(
            "/api/v1/analytics/profile",
            json=payload,
            headers=auth_headers
        )
        assert response.status_code == 201
        data = response.json()

        assert data["id"] is not None
        assert data["legal_name"] == "Test Company (Pty) Ltd"
        assert data["industry"] == "Information Technology"

    @pytest.mark.asyncio
    async def test_update_company_profile(self, client: AsyncClient, auth_headers: dict):
        """Should update company profile."""
        # Create first
        create_payload = {
            "legal_name": "Original Name",
            "industry": "IT"
        }
        await client.post("/api/v1/analytics/profile", json=create_payload, headers=auth_headers)

        # Update
        update_payload = {
            "legal_name": "Updated Name",
            "industry": "Technology"
        }
        response = await client.put(
            "/api/v1/analytics/profile",
            json=update_payload,
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["legal_name"] == "Updated Name"


class TestNoMockDataVerification:
    """Tests to verify NO hardcoded mock data exists."""

    @pytest.mark.asyncio
    async def test_no_user_001_in_responses(self, client: AsyncClient, auth_headers: dict):
        """No endpoint should return 'user_001'."""
        endpoints = [
            "/api/v1/analytics/compliance",
            "/api/v1/analytics/costs",
            "/api/v1/analytics/funnel",
            "/api/v1/analytics/outcomes",
            "/api/v1/analytics/pipeline",
            "/api/v1/analytics/payment-delays"
        ]
        for endpoint in endpoints:
            response = await client.get(endpoint, headers=auth_headers)
            response_text = response.text
            assert "user_001" not in response_text, f"Found 'user_001' in {endpoint}"

    @pytest.mark.asyncio
    async def test_no_hardcoded_ids_in_post_responses(self, client: AsyncClient, auth_headers: dict):
        """POST endpoints should not return hardcoded IDs."""
        # Test compliance
        response = await client.post(
            "/api/v1/analytics/compliance",
            json={
                "type": "test",
                "name": "Test",
                "short_name": "T",
                "registration_number": "123",
                "issue_date": "2025-01-01",
                "expiry_type": "annual"
            },
            headers=auth_headers
        )
        if response.status_code == 201:
            assert response.json()["id"] not in ["csd", "cidb", "bbbee", "sars", "uif", "coid"]

        # Test costs
        response = await client.post(
            "/api/v1/analytics/costs",
            json={"category": "platform", "description": "Test", "amount": 100, "date": "2025-01-01"},
            headers=auth_headers
        )
        if response.status_code == 201:
            assert response.json()["id"] != "cost_001"
