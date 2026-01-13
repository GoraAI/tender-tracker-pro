"""Integration tests for authentication endpoints."""

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
class TestAuthEndpoints:
    """Tests for /api/v1/auth/* endpoints."""

    async def test_register_success(self, client: AsyncClient):
        """POST /auth/register should create user and return token."""
        response = await client.post(
            "/api/v1/auth/register",
            json={
                "email": "newuser@example.com",
                "password": "SecurePass123",
                "full_name": "New User",
                "company_name": "New Company",
            },
        )

        assert response.status_code == 201
        data = response.json()
        assert "access_token" in data
        assert data["token_type"] == "bearer"

    async def test_register_duplicate_email(self, client: AsyncClient, test_user):
        """POST /auth/register should fail for duplicate email."""
        response = await client.post(
            "/api/v1/auth/register",
            json={
                "email": test_user.email,
                "password": "SecurePass123",
            },
        )

        assert response.status_code == 400
        assert "already registered" in response.json()["detail"]

    async def test_register_weak_password(self, client: AsyncClient):
        """POST /auth/register should validate password strength."""
        response = await client.post(
            "/api/v1/auth/register",
            json={
                "email": "weak@example.com",
                "password": "weak",  # Too short, no uppercase/digit
            },
        )

        assert response.status_code == 422  # Validation error

    async def test_login_success(self, client: AsyncClient, test_user):
        """POST /auth/login should return token for valid credentials."""
        response = await client.post(
            "/api/v1/auth/login",
            json={
                "email": test_user.email,
                "password": "TestPass123",
            },
        )

        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
        assert data["token_type"] == "bearer"

    async def test_login_invalid_password(self, client: AsyncClient, test_user):
        """POST /auth/login should fail for wrong password."""
        response = await client.post(
            "/api/v1/auth/login",
            json={
                "email": test_user.email,
                "password": "WrongPassword123",
            },
        )

        assert response.status_code == 401

    async def test_login_unknown_email(self, client: AsyncClient):
        """POST /auth/login should fail for unknown email."""
        response = await client.post(
            "/api/v1/auth/login",
            json={
                "email": "unknown@example.com",
                "password": "SomePass123",
            },
        )

        assert response.status_code == 401

    async def test_me_authenticated(self, client: AsyncClient, test_user, auth_headers):
        """GET /auth/me should return current user info."""
        response = await client.get("/api/v1/auth/me", headers=auth_headers)

        assert response.status_code == 200
        data = response.json()
        assert data["email"] == test_user.email
        assert data["full_name"] == test_user.full_name
        assert "password_hash" not in data  # Should be excluded

    async def test_me_unauthenticated(self, client: AsyncClient):
        """GET /auth/me should fail without token."""
        response = await client.get("/api/v1/auth/me")

        assert response.status_code == 403  # No auth header

    async def test_refresh_token(self, client: AsyncClient, auth_headers):
        """POST /auth/refresh should return new token."""
        response = await client.post("/api/v1/auth/refresh", headers=auth_headers)

        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
