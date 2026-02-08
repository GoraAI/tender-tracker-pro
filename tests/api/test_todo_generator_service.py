"""
Unit tests for ToDoGeneratorService.

Tests the intelligent to-do generation logic:
- Missing field detection
- Auto-fill review items
- Pricing requirements
- Technical questions
- Document requirements
- Final review
- Priority assignment (High/Medium/Low)
- Progress tracking
"""

import pytest
import pytest_asyncio
from uuid import uuid4

from src.api.services.todo_generator_service import ToDoGeneratorService


@pytest_asyncio.fixture
async def todo_service():
    """Create ToDoGeneratorService instance."""
    return ToDoGeneratorService()


class TestToDoGeneratorService:
    """Test cases for ToDoGeneratorService."""

    @pytest.mark.asyncio
    async def test_generate_missing_field_todos(self, todo_service: ToDoGeneratorService):
        """Test that missing fields create high-priority to-dos."""
        auto_filled_fields = {
            "company_name": {"value": "Acme Ltd", "auto_filled": True, "source": "Company Profile"},
            "pricing": {"value": None, "auto_filled": False, "source": None},
            "contact_email": {"value": None, "auto_filled": False, "source": None}
        }

        todos = todo_service.generate_todos(
            auto_filled_fields=auto_filled_fields,
            tender_requirements="Standard tender"
        )

        # Should have to-dos for missing pricing and contact_email
        missing_todos = [t for t in todos if t.category == "missing_field"]
        assert len(missing_todos) >= 2

        # Missing fields should be high priority
        pricing_todo = next((t for t in missing_todos if "pricing" in t.title.lower()), None)
        assert pricing_todo is not None
        assert pricing_todo.priority == "high"
        assert pricing_todo.completed is False

    @pytest.mark.asyncio
    async def test_generate_review_todos_for_auto_filled(
        self,
        todo_service: ToDoGeneratorService
    ):
        """Test that auto-filled fields create review to-dos."""
        auto_filled_fields = {
            "company_overview": {
                "value": "Auto-generated company overview",
                "auto_filled": True,
                "source": "Answer Library"
            },
            "methodology": {
                "value": "Agile methodology",
                "auto_filled": True,
                "source": "Answer Library"
            }
        }

        todos = todo_service.generate_todos(
            auto_filled_fields=auto_filled_fields,
            tender_requirements="Standard tender"
        )

        # Should have review to-dos for auto-filled content
        review_todos = [t for t in todos if t.category == "review"]
        assert len(review_todos) >= 2

        # Review items should be medium priority
        overview_review = next(
            (t for t in review_todos if "overview" in t.title.lower()),
            None
        )
        assert overview_review is not None
        assert overview_review.priority == "medium"

    @pytest.mark.asyncio
    async def test_generate_pricing_todos(self, todo_service: ToDoGeneratorService):
        """Test that pricing requirements create high-priority to-dos."""
        auto_filled_fields = {
            "item_1_price": {"value": None, "auto_filled": False, "source": None},
            "item_2_price": {"value": None, "auto_filled": False, "source": None}
        }

        tender_requirements = "Provide pricing for all items in Schedule A"

        todos = todo_service.generate_todos(
            auto_filled_fields=auto_filled_fields,
            tender_requirements=tender_requirements
        )

        # Should have pricing to-dos
        pricing_todos = [t for t in todos if t.category == "pricing"]
        assert len(pricing_todos) >= 1

        # Pricing should be high priority
        for todo in pricing_todos:
            assert todo.priority == "high"

    @pytest.mark.asyncio
    async def test_generate_technical_question_todos(
        self,
        todo_service: ToDoGeneratorService
    ):
        """Test that technical questions create medium-priority to-dos."""
        auto_filled_fields = {
            "tech_question_1": {"value": None, "auto_filled": False, "source": None}
        }

        tender_requirements = "Describe your development methodology and quality assurance process"

        todos = todo_service.generate_todos(
            auto_filled_fields=auto_filled_fields,
            tender_requirements=tender_requirements
        )

        # Should have technical question to-dos
        tech_todos = [t for t in todos if t.category == "technical_response"]
        assert len(tech_todos) >= 1

        # Technical questions should be medium priority
        for todo in tech_todos:
            assert todo.priority == "medium"

    @pytest.mark.asyncio
    async def test_generate_document_todos(self, todo_service: ToDoGeneratorService):
        """Test that document requirements create high-priority to-dos."""
        auto_filled_fields = {
            "bee_certificate": {"value": None, "auto_filled": False, "source": None},
            "tax_clearance": {"value": None, "auto_filled": False, "source": None}
        }

        tender_requirements = "Submit B-BBEE certificate and tax clearance"

        todos = todo_service.generate_todos(
            auto_filled_fields=auto_filled_fields,
            tender_requirements=tender_requirements
        )

        # Should have document to-dos
        doc_todos = [t for t in todos if t.category == "document"]
        assert len(doc_todos) >= 2

        # Documents should be high priority
        for todo in doc_todos:
            assert todo.priority == "high"

    @pytest.mark.asyncio
    async def test_generate_final_review_todo(self, todo_service: ToDoGeneratorService):
        """Test that final review to-do is always created."""
        auto_filled_fields = {
            "company_name": {"value": "Acme Ltd", "auto_filled": True, "source": "Company Profile"}
        }

        todos = todo_service.generate_todos(
            auto_filled_fields=auto_filled_fields,
            tender_requirements="Standard tender"
        )

        # Should have final review to-do
        final_todos = [t for t in todos if t.category == "final_check"]
        assert len(final_todos) >= 1

        # Final review should be low priority
        final_todo = final_todos[0]
        assert final_todo.priority == "low"
        assert "final review" in final_todo.title.lower()

    @pytest.mark.asyncio
    async def test_priority_ordering(self, todo_service: ToDoGeneratorService):
        """Test that to-dos are ordered by priority (High → Medium → Low)."""
        auto_filled_fields = {
            "pricing": {"value": None, "auto_filled": False, "source": None},  # High
            "company_overview": {
                "value": "Auto-generated",
                "auto_filled": True,
                "source": "Answer Library"
            },  # Medium (review)
            "technical_question": {"value": None, "auto_filled": False, "source": None}  # Medium
        }

        todos = todo_service.generate_todos(
            auto_filled_fields=auto_filled_fields,
            tender_requirements="Provide pricing and technical details"
        )

        # Extract priorities in order
        priorities = [t.priority for t in todos]

        # High should come before Medium, Medium before Low
        high_indices = [i for i, p in enumerate(priorities) if p == "high"]
        medium_indices = [i for i, p in enumerate(priorities) if p == "medium"]
        low_indices = [i for i, p in enumerate(priorities) if p == "low"]

        if high_indices and medium_indices:
            assert max(high_indices) < min(medium_indices)
        if medium_indices and low_indices:
            assert max(medium_indices) < min(low_indices)

    @pytest.mark.asyncio
    async def test_calculate_progress_all_incomplete(
        self,
        todo_service: ToDoGeneratorService
    ):
        """Test progress calculation when all to-dos are incomplete."""
        todo_items = [
            {"id": "1", "completed": False},
            {"id": "2", "completed": False},
            {"id": "3", "completed": False},
            {"id": "4", "completed": False}
        ]

        progress = todo_service.calculate_progress(todo_items)

        assert progress["total"] == 4
        assert progress["completed"] == 0
        assert progress["percentage"] == 0
        assert progress["ready_to_submit"] is False

    @pytest.mark.asyncio
    async def test_calculate_progress_partially_complete(
        self,
        todo_service: ToDoGeneratorService
    ):
        """Test progress calculation with some completed to-dos."""
        todo_items = [
            {"id": "1", "completed": True},
            {"id": "2", "completed": True},
            {"id": "3", "completed": False},
            {"id": "4", "completed": False}
        ]

        progress = todo_service.calculate_progress(todo_items)

        assert progress["total"] == 4
        assert progress["completed"] == 2
        assert progress["percentage"] == 50
        assert progress["ready_to_submit"] is False

    @pytest.mark.asyncio
    async def test_calculate_progress_all_complete(
        self,
        todo_service: ToDoGeneratorService
    ):
        """Test progress calculation when all to-dos are complete."""
        todo_items = [
            {"id": "1", "completed": True},
            {"id": "2", "completed": True},
            {"id": "3", "completed": True}
        ]

        progress = todo_service.calculate_progress(todo_items)

        assert progress["total"] == 3
        assert progress["completed"] == 3
        assert progress["percentage"] == 100
        assert progress["ready_to_submit"] is True

    @pytest.mark.asyncio
    async def test_calculate_progress_empty_list(
        self,
        todo_service: ToDoGeneratorService
    ):
        """Test progress calculation with empty to-do list."""
        progress = todo_service.calculate_progress([])

        assert progress["total"] == 0
        assert progress["completed"] == 0
        assert progress["percentage"] == 0
        # Empty to-do list means ready to submit
        assert progress["ready_to_submit"] is True

    @pytest.mark.asyncio
    async def test_todo_has_section_reference(self, todo_service: ToDoGeneratorService):
        """Test that to-dos include section references where applicable."""
        auto_filled_fields = {
            "section_a_pricing": {"value": None, "auto_filled": False, "source": None}
        }

        tender_requirements = "Complete Section A: Pricing Schedule"

        todos = todo_service.generate_todos(
            auto_filled_fields=auto_filled_fields,
            tender_requirements=tender_requirements
        )

        # Find pricing to-do
        pricing_todo = next(
            (t for t in todos if t.category == "pricing"),
            None
        )

        assert pricing_todo is not None
        # Should have section reference if detected
        if hasattr(pricing_todo, 'section_reference') and pricing_todo.section_reference:
            assert "section" in pricing_todo.section_reference.lower()

    @pytest.mark.asyncio
    async def test_generate_comprehensive_todo_list(
        self,
        todo_service: ToDoGeneratorService
    ):
        """Test generation of a comprehensive to-do list with all categories."""
        auto_filled_fields = {
            # Auto-filled fields needing review (medium)
            "company_overview": {
                "value": "Auto-generated overview",
                "auto_filled": True,
                "source": "Answer Library"
            },
            # Missing critical fields (high)
            "pricing_item_1": {"value": None, "auto_filled": False, "source": None},
            "bee_certificate": {"value": None, "auto_filled": False, "source": None},
            # Missing technical response (medium)
            "methodology_question": {"value": None, "auto_filled": False, "source": None}
        }

        tender_requirements = """
        Provide detailed pricing for all items.
        Describe your software development methodology.
        Submit B-BBEE certificate and tax clearance.
        """

        todos = todo_service.generate_todos(
            auto_filled_fields=auto_filled_fields,
            tender_requirements=tender_requirements
        )

        # Should have multiple categories
        categories = {t.category for t in todos}
        assert "review" in categories
        assert "missing_field" in categories or "pricing" in categories
        assert "final_check" in categories

        # Should have mix of priorities
        priorities = {t.priority for t in todos}
        assert "high" in priorities
        assert "low" in priorities  # final_check

        # All should start as incomplete
        assert all(t.completed is False for t in todos)

        # Should have unique IDs
        ids = [t.id for t in todos]
        assert len(ids) == len(set(ids))
