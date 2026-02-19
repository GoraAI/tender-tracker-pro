"""Integration tests for dashboard endpoints."""

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
class TestDashboardEndpoints:
    """Tests for /api/v1/dashboard/* endpoints."""

    async def test_get_stats_authenticated(self, client: AsyncClient, auth_headers):
        """GET /dashboard/stats should return statistics."""
        response = await client.get("/api/v1/dashboard/stats", headers=auth_headers)

        assert response.status_code == 200
        data = response.json()
        assert "total_tenders" in data
        assert "high_priority_count" in data
        assert "avg_score" in data
        assert "tenders_this_week" in data
        assert "sources_active" in data

    async def test_get_stats_unauthenticated(self, client: AsyncClient):
        """GET /dashboard/stats should fail without auth."""
        response = await client.get("/api/v1/dashboard/stats")

        assert response.status_code == 401

    async def test_get_recent_tenders(self, client: AsyncClient, auth_headers):
        """GET /dashboard/recent should return recent high-priority tenders."""
        response = await client.get("/api/v1/dashboard/recent", headers=auth_headers)

        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        # Empty list is valid if no high-priority tenders


@pytest.mark.asyncio
class TestHealthEndpoint:
    """Tests for health check endpoint."""

    async def test_health_check(self, client: AsyncClient):
        """GET /health should return healthy status."""
        response = await client.get("/health")

        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert "version" in data


@pytest.mark.asyncio
class TestRootEndpoint:
    """Tests for root endpoint."""

    async def test_root(self, client: AsyncClient):
        """GET / should return API info."""
        response = await client.get("/")

        assert response.status_code == 200
        data = response.json()
        assert "name" in data
        assert "version" in data
        assert data["docs"] == "/docs"
