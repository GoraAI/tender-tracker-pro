"""To-do generator service for tender applications."""

import copy
from datetime import datetime, timezone
from typing import Optional
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models.tender import Tender
from src.api.models.tender_application import TenderApplication


class ToDoGeneratorService:
    """Service for generating to-do items for applications."""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def generate_todos(
        self, application_id: UUID, user_id: UUID
    ) -> list[dict]:
        """
        Generate to-do items for an application.

        Args:
            application_id: Application to generate to-dos for
            user_id: User owning the application

        Returns:
            List of to-do item dicts
        """
        # Get application
        app_result = await self.db.execute(
            select(TenderApplication).where(
                TenderApplication.id == application_id,
                TenderApplication.user_id == user_id,
            )
        )
        application = app_result.scalar_one_or_none()
        if not application:
            raise ValueError(f"Application {application_id} not found")

        # Get tender
        tender_result = await self.db.execute(
            select(Tender).where(Tender.id == application.tender_id)
        )
        tender = tender_result.scalar_one_or_none()
        if not tender:
            raise ValueError(f"Tender {application.tender_id} not found")

        todos = []
        todo_id = 1

        # 1. Missing required fields (High priority)
        required_fields = [
            "company_name",
            "registration_number",
            "capabilities",
            "compliance_documents",
        ]

        for field in required_fields:
            if field not in application.auto_filled_fields:
                todos.append({
                    "id": str(todo_id),
                    "category": "missing_field",
                    "priority": "high",
                    "title": f"Provide {field.replace('_', ' ').title()}",
                    "description": f"Required field: {field.replace('_', ' ')}",
                    "section_reference": f"Section 1",
                    "completed": False,
                    "completed_at": None,
                })
                todo_id += 1

        # 2. Review auto-filled content (Medium priority)
        for field_name, field_data in application.auto_filled_fields.items():
            if field_data.get("auto_filled", False):
                todos.append({
                    "id": str(todo_id),
                    "category": "review",
                    "priority": "medium",
                    "title": f"Review Auto-Filled: {field_name.replace('_', ' ').title()}",
                    "description": f"Verify accuracy of {field_name.replace('_', ' ')} (source: {field_data.get('source', 'unknown')})",
                    "auto_filled_field": field_name,
                    "completed": False,
                    "completed_at": None,
                })
                todo_id += 1

        # 3. Pricing (High priority if tender has estimated value)
        if tender.estimated_value or (tender.requirements_summary and "pricing" in tender.requirements_summary.lower()):
            if not application.pricing_data or not application.pricing_data.get("grand_total"):
                todos.append({
                    "id": str(todo_id),
                    "category": "pricing",
                    "priority": "high",
                    "title": "Prepare Pricing Schedule",
                    "description": f"Enter unit costs and calculate totals for tender requirements",
                    "section_reference": "Section 5 - Pricing",
                    "estimated_time": "1-2 hours",
                    "completed": False,
                    "completed_at": None,
                })
                todo_id += 1

        # 4. Technical responses (High priority)
        if tender.requirements_summary:
            # Simple keyword detection for technical questions
            technical_keywords = [
                "methodology",
                "technical approach",
                "project plan",
                "implementation",
                "strategy",
            ]

            for keyword in technical_keywords:
                if keyword.lower() in tender.requirements_summary.lower():
                    todos.append({
                        "id": str(todo_id),
                        "category": "technical_response",
                        "priority": "high",
                        "title": f"Answer: {keyword.title()}",
                        "description": f"Provide detailed response for {keyword}",
                        "section_reference": "Technical Section",
                        "can_use_ai": True,
                        "estimated_time": "30-60 minutes",
                        "completed": False,
                        "completed_at": None,
                    })
                    todo_id += 1
                    break  # Only add one technical response item for MVP

        # 5. Document check (High priority)
        compliance_docs = application.auto_filled_fields.get("compliance_documents", {}).get("value", {})
        required_doc_types = [
            "company_registration",
            "tax_clearance",
            "bbee_certificate",
            "insurance",
        ]

        for doc_type in required_doc_types:
            if doc_type not in compliance_docs:
                display_name = doc_type.replace("_", " ").title()
                todos.append({
                    "id": str(todo_id),
                    "category": "document",
                    "priority": "high",
                    "title": f"Upload {display_name}",
                    "description": f"Required document: {display_name}",
                    "section_reference": "Section 4 - Documents",
                    "completed": False,
                    "completed_at": None,
                })
                todo_id += 1

        # 6. Final checklist (Low priority)
        todos.append({
            "id": str(todo_id),
            "category": "final_check",
            "priority": "low",
            "title": "Final Review",
            "description": "Review entire application before submission",
            "completed": False,
            "completed_at": None,
        })

        # Sort by priority: high → medium → low
        priority_order = {"high": 0, "medium": 1, "low": 2}
        todos.sort(key=lambda t: priority_order.get(t["priority"], 3))

        return todos

    async def update_todo_status(
        self,
        application_id: UUID,
        user_id: UUID,
        todo_id: str,
        completed: bool,
    ) -> TenderApplication:
        """
        Update a to-do item's completion status.

        Args:
            application_id: Application ID
            user_id: User ID
            todo_id: To-do item ID
            completed: New completion status

        Returns:
            Updated TenderApplication
        """
        # Get application
        result = await self.db.execute(
            select(TenderApplication).where(
                TenderApplication.id == application_id,
                TenderApplication.user_id == user_id,
            )
        )
        application = result.scalar_one_or_none()
        if not application:
            raise ValueError(f"Application {application_id} not found")

        # Update to-do item (deep copy to ensure SQLAlchemy detects mutation)
        todos = copy.deepcopy(application.to_do_items)
        for todo in todos:
            if todo["id"] == todo_id:
                todo["completed"] = completed
                todo["completed_at"] = (
                    datetime.now(timezone.utc).isoformat() if completed else None
                )
                break

        application.to_do_items = todos
        await self.db.commit()
        await self.db.refresh(application)

        return application

    def calculate_progress(self, todos: list[dict]) -> dict:
        """
        Calculate application progress from to-do items.

        Args:
            todos: List of to-do items

        Returns:
            Progress dict with completed, total, percentage, ready_to_submit
        """
        total = len(todos)
        completed = len([t for t in todos if t.get("completed", False)])
        percentage = (completed / total * 100) if total > 0 else 0

        return {
            "completed": completed,
            "total": total,
            "percentage": round(percentage, 1),
            "ready_to_submit": completed == total,
        }
