"""Unit tests for authentication service."""

import pytest

from src.api.services.auth_service import (
    create_access_token,
    decode_token,
    hash_password,
    verify_password,
)


class TestPasswordHashing:
    """Tests for password hashing functions."""

    def test_hash_password_returns_hash(self):
        """hash_password should return a bcrypt hash."""
        password = "TestPassword123"
        hashed = hash_password(password)

        assert hashed != password
        assert hashed.startswith("$2b$")  # bcrypt prefix
        assert len(hashed) == 60  # bcrypt hash length

    def test_hash_password_different_each_time(self):
        """hash_password should generate different hashes for same password."""
        password = "TestPassword123"
        hash1 = hash_password(password)
        hash2 = hash_password(password)

        assert hash1 != hash2  # Different salts

    def test_verify_password_correct(self):
        """verify_password should return True for correct password."""
        password = "TestPassword123"
        hashed = hash_password(password)

        assert verify_password(password, hashed) is True

    def test_verify_password_incorrect(self):
        """verify_password should return False for incorrect password."""
        password = "TestPassword123"
        hashed = hash_password(password)

        assert verify_password("WrongPassword", hashed) is False


class TestJWTTokens:
    """Tests for JWT token functions."""

    def test_create_access_token_returns_string(self):
        """create_access_token should return a JWT string."""
        from uuid import uuid4

        user_id = uuid4()
        email = "test@example.com"

        token = create_access_token(user_id, email)

        assert isinstance(token, str)
        assert len(token) > 0
        # JWT has 3 parts separated by dots
        assert len(token.split(".")) == 3

    def test_decode_token_valid(self):
        """decode_token should decode valid token."""
        from uuid import uuid4

        user_id = uuid4()
        email = "test@example.com"

        token = create_access_token(user_id, email)
        token_data = decode_token(token)

        assert token_data is not None
        assert token_data.user_id == str(user_id)
        assert token_data.email == email

    def test_decode_token_invalid(self):
        """decode_token should return None for invalid token."""
        token_data = decode_token("invalid.token.here")

        assert token_data is None

    def test_decode_token_expired(self):
        """decode_token should return None for expired token."""
        from datetime import timedelta
        from uuid import uuid4

        user_id = uuid4()
        email = "test@example.com"

        # Create token with negative expiration (already expired)
        token = create_access_token(user_id, email, expires_delta=timedelta(seconds=-10))
        token_data = decode_token(token)

        assert token_data is None
