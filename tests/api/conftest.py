"""Test fixtures for API tests.

Note: Common fixtures (db, test_user, auth_headers, client, test_source)
are provided by the root tests/conftest.py. This file provides additional
fixtures specific to API integration tests.
"""

import pytest_asyncio
from typing import AsyncGenerator
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from src.api.database import Base

# Legacy db_session fixture for test_tender_endpoints.py
# New tests should use the `db` fixture from root conftest.

TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"

test_engine = create_async_engine(TEST_DATABASE_URL, echo=False)

test_async_session = async_sessionmaker(
    test_engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autocommit=False,
    autoflush=False,
)


@pytest_asyncio.fixture
async def db_session() -> AsyncGenerator[AsyncSession, None]:
    """Create a test database session (legacy - prefer `db` from root conftest)."""
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    async with test_async_session() as session:
        yield session

    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
