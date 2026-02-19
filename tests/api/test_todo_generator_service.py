"""
Unit tests for ToDoGeneratorService.

Tests the intelligent to-do generation logic:
- Missing field detection
- Auto-fill review items
- Pricing requirements
- Document requirements
- Final review
- Priority assignment (High/Medium/Low)
- Progress tracking
"""

import pytest
import pytest_asyncio
from datetime import datetime, timedelta, timezone
from uuid import uuid4
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models import Tender, TenderApplication, CompanyProfile
from src.api.services.todo_generator_service import ToDoGeneratorService


@pytest_asyncio.fixture
async def todo_service(db: AsyncSession):
    """Create ToDoGeneratorService instance."""
    return ToDoGeneratorService(db)


@pytest_asyncio.fixture
async def test_company_profile(db: AsyncSession, test_user) -> CompanyProfile:
    """Create a test company profile."""
    profile = CompanyProfile(
        user_id=test_user.id,
        legal_name="Acme Software Solutions Ltd",
        industry="Information Technology",
        capabilities=["Software Development", "Cloud Infrastructure"],
        preferred_sectors=["IT"],
    )
    db.add(profile)
    await db.commit()
    await db.refresh(profile)
    return profile


@pytest_asyncio.fixture
async def test_tender(db: AsyncSession, test_source) -> Tender:
    """Create a test tender with requirements."""
    future_date = datetime.now(timezone.utc) + timedelta(days=14)
    tender = Tender(
        id=uuid4(),
        source_id=test_source.id,
        pdf_url="https://test.example.com/tender.pdf",
        title="Software Development Tender",
        reference="TODO-001",
        estimated_value=500000.0,
        deadline=future_date,
        requirements_summary="Provide pricing for all items. Describe your methodology and implementation approach.",
    )
    db.add(tender)
    await db.commit()
    await db.refresh(tender)
    return tender


@pytest_asyncio.fixture
async def test_application(
    db: AsyncSession, test_tender: Tender, test_user
) -> TenderApplication:
    """Create a test application with auto-filled fields."""
    application = TenderApplication(
        id=uuid4(),
        tender_id=test_tender.id,
        user_id=test_user.id,
        status="draft",
        auto_filled_fields={
            "company_name": {"value": "Acme Ltd", "auto_filled": True, "source": "CompanyProfile"},
            "industry": {"value": "IT", "auto_filled": True, "source": "CompanyProfile"},
        },
        to_do_items=[],
    )
    db.add(application)
    await db.commit()
    await db.refresh(application)
    return application


class TestToDoGeneratorService:
    """Test cases for ToDoGeneratorService."""

    @pytest.mark.asyncio
    async def test_generate_todos_creates_items(
        self,
        todo_service: ToDoGeneratorService,
        test_application: TenderApplication,
        test_user,
    ):
        """Test that generate_todos creates a non-empty list of to-do items."""
        todos = await todo_service.generate_todos(
            application_id=test_application.id,
            user_id=test_user.id,
        )

        assert isinstance(todos, list)
        assert len(todos) > 0

    @pytest.mark.asyncio
    async def test_generate_missing_field_todos(
        self,
        todo_service: ToDoGeneratorService,
        test_application: TenderApplication,
        test_user,
    ):
        """Test that missing required fields create high-priority to-dos."""
        todos = await todo_service.generate_todos(
            application_id=test_application.id,
            user_id=test_user.id,
        )

        # Should have to-dos for missing required fields (registration_number, capabilities, compliance_documents)
        missing_todos = [t for t in todos if t["category"] == "missing_field"]
        assert len(missing_todos) >= 1

        # Missing fields should be high priority
        for todo in missing_todos:
            assert todo["priority"] == "high"
            assert todo["completed"] is False

    @pytest.mark.asyncio
    async def test_generate_review_todos_for_auto_filled(
        self,
        todo_service: ToDoGeneratorService,
        test_application: TenderApplication,
        test_user,
    ):
        """Test that auto-filled fields create review to-dos."""
        todos = await todo_service.generate_todos(
            application_id=test_application.id,
            user_id=test_user.id,
        )

        # Should have review to-dos for auto-filled content
        review_todos = [t for t in todos if t["category"] == "review"]
        assert len(review_todos) >= 2  # company_name and industry

        # Review items should be medium priority
        for todo in review_todos:
            assert todo["priority"] == "medium"

    @pytest.mark.asyncio
    async def test_generate_pricing_todos(
        self,
        todo_service: ToDoGeneratorService,
        test_application: TenderApplication,
        test_user,
    ):
        """Test that pricing requirements create to-dos when estimated_value is set."""
        todos = await todo_service.generate_todos(
            application_id=test_application.id,
            user_id=test_user.id,
        )

        # Should have pricing to-do (tender has estimated_value and "pricing" in requirements)
        pricing_todos = [t for t in todos if t["category"] == "pricing"]
        assert len(pricing_todos) >= 1
        assert pricing_todos[0]["priority"] == "high"

    @pytest.mark.asyncio
    async def test_generate_final_review_todo(
        self,
        todo_service: ToDoGeneratorService,
        test_application: TenderApplication,
        test_user,
    ):
        """Test that final review to-do is always created."""
        todos = await todo_service.generate_todos(
            application_id=test_application.id,
            user_id=test_user.id,
        )

        # Should have final review to-do
        final_todos = [t for t in todos if t["category"] == "final_check"]
        assert len(final_todos) == 1
        assert final_todos[0]["priority"] == "low"
        assert "final review" in final_todos[0]["title"].lower()

    @pytest.mark.asyncio
    async def test_priority_ordering(
        self,
        todo_service: ToDoGeneratorService,
        test_application: TenderApplication,
        test_user,
    ):
        """Test that to-dos are ordered by priority (High -> Medium -> Low)."""
        todos = await todo_service.generate_todos(
            application_id=test_application.id,
            user_id=test_user.id,
        )

        priorities = [t["priority"] for t in todos]
        priority_order = {"high": 0, "medium": 1, "low": 2}

        high_indices = [i for i, p in enumerate(priorities) if p == "high"]
        medium_indices = [i for i, p in enumerate(priorities) if p == "medium"]
        low_indices = [i for i, p in enumerate(priorities) if p == "low"]

        if high_indices and medium_indices:
            assert max(high_indices) < min(medium_indices)
        if medium_indices and low_indices:
            assert max(medium_indices) < min(low_indices)

    @pytest.mark.asyncio
    async def test_generate_document_todos(
        self,
        todo_service: ToDoGeneratorService,
        test_application: TenderApplication,
        test_user,
    ):
        """Test that document requirements create to-dos."""
        todos = await todo_service.generate_todos(
            application_id=test_application.id,
            user_id=test_user.id,
        )

        # Should have document to-dos (no compliance docs in auto_filled_fields)
        doc_todos = [t for t in todos if t["category"] == "document"]
        assert len(doc_todos) >= 1
        for todo in doc_todos:
            assert todo["priority"] == "high"

    @pytest.mark.asyncio
    async def test_todos_have_unique_ids(
        self,
        todo_service: ToDoGeneratorService,
        test_application: TenderApplication,
        test_user,
    ):
        """Test that all generated to-dos have unique IDs."""
        todos = await todo_service.generate_todos(
            application_id=test_application.id,
            user_id=test_user.id,
        )

        ids = [t["id"] for t in todos]
        assert len(ids) == len(set(ids))

    @pytest.mark.asyncio
    async def test_all_todos_start_incomplete(
        self,
        todo_service: ToDoGeneratorService,
        test_application: TenderApplication,
        test_user,
    ):
        """Test that all generated to-dos start as incomplete."""
        todos = await todo_service.generate_todos(
            application_id=test_application.id,
            user_id=test_user.id,
        )

        assert all(t["completed"] is False for t in todos)

    # Progress calculation tests (no DB needed - tests the method directly)

    @pytest.mark.asyncio
    async def test_calculate_progress_all_incomplete(
        self, todo_service: ToDoGeneratorService
    ):
        """Test progress calculation when all to-dos are incomplete."""
        todo_items = [
            {"id": "1", "completed": False},
            {"id": "2", "completed": False},
            {"id": "3", "completed": False},
            {"id": "4", "completed": False},
        ]

        progress = todo_service.calculate_progress(todo_items)

        assert progress["total"] == 4
        assert progress["completed"] == 0
        assert progress["percentage"] == 0
        assert progress["ready_to_submit"] is False

    @pytest.mark.asyncio
    async def test_calculate_progress_partially_complete(
        self, todo_service: ToDoGeneratorService
    ):
        """Test progress calculation with some completed to-dos."""
        todo_items = [
            {"id": "1", "completed": True},
            {"id": "2", "completed": True},
            {"id": "3", "completed": False},
            {"id": "4", "completed": False},
        ]

        progress = todo_service.calculate_progress(todo_items)

        assert progress["total"] == 4
        assert progress["completed"] == 2
        assert progress["percentage"] == 50
        assert progress["ready_to_submit"] is False

    @pytest.mark.asyncio
    async def test_calculate_progress_all_complete(
        self, todo_service: ToDoGeneratorService
    ):
        """Test progress calculation when all to-dos are complete."""
        todo_items = [
            {"id": "1", "completed": True},
            {"id": "2", "completed": True},
            {"id": "3", "completed": True},
        ]

        progress = todo_service.calculate_progress(todo_items)

        assert progress["total"] == 3
        assert progress["completed"] == 3
        assert progress["percentage"] == 100
        assert progress["ready_to_submit"] is True

    @pytest.mark.asyncio
    async def test_calculate_progress_empty_list(
        self, todo_service: ToDoGeneratorService
    ):
        """Test progress calculation with empty to-do list."""
        progress = todo_service.calculate_progress([])

        assert progress["total"] == 0
        assert progress["completed"] == 0
        assert progress["percentage"] == 0
        # Empty to-do list means ready to submit
        assert progress["ready_to_submit"] is True
