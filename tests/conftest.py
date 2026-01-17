"""
Pytest configuration for Tender Tracker Pro tests.

Adds project root to sys.path for imports.
Provides fixtures for database, users, and HTTP client.
"""

import asyncio
import sys
from pathlib import Path
from typing import AsyncGenerator
from uuid import uuid4

import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker

# Add project root to sys.path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from src.api.main import app
from src.api.database import Base, get_db
from src.api.models import User, Tender, Source
from src.api.services.auth_service import create_access_token, hash_password


# Test database URL (in-memory SQLite)
TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"


@pytest.fixture(scope="session")
def event_loop():
    """Create an event loop for the test session."""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest_asyncio.fixture(scope="function")
async def engine():
    """Create a test database engine."""
    engine = create_async_engine(TEST_DATABASE_URL, echo=False)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield engine
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await engine.dispose()


@pytest_asyncio.fixture(scope="function")
async def db(engine) -> AsyncGenerator[AsyncSession, None]:
    """Create a test database session."""
    async_session = async_sessionmaker(engine, expire_on_commit=False)
    async with async_session() as session:
        yield session


@pytest_asyncio.fixture(scope="function")
async def test_user(db: AsyncSession) -> User:
    """Create a test user."""
    user = User(
        id=uuid4(),
        email="test@example.com",
        password_hash=hash_password("testpassword"),
        full_name="Test User",
        company_name="Test Company",
        is_active=True,
        is_admin=False,
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return user


@pytest_asyncio.fixture(scope="function")
async def test_source(db: AsyncSession) -> Source:
    """Create a test source."""
    source = Source(
        id="test-source",
        name="Test Source",
        url="https://test.example.com",
        is_active=True,
    )
    db.add(source)
    await db.commit()
    await db.refresh(source)
    return source


@pytest_asyncio.fixture(scope="function")
async def test_tender(db: AsyncSession, test_source: Source) -> Tender:
    """Create a test tender."""
    tender = Tender(
        id=uuid4(),
        source_id=test_source.id,
        pdf_url="https://test.example.com/tender.pdf",
        title="Test Tender",
        reference="TEST-001",
    )
    db.add(tender)
    await db.commit()
    await db.refresh(tender)
    return tender


@pytest_asyncio.fixture(scope="function")
async def auth_headers(test_user: User) -> dict:
    """Create auth headers for test user."""
    token = create_access_token(test_user.id, test_user.email)
    return {"Authorization": f"Bearer {token}"}


@pytest_asyncio.fixture(scope="function")
async def client(db: AsyncSession, test_user: User) -> AsyncGenerator[AsyncClient, None]:
    """Create a test HTTP client."""
    # Override the database dependency
    async def override_get_db():
        yield db

    app.dependency_overrides[get_db] = override_get_db

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client

    app.dependency_overrides.clear()
