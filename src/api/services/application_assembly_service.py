"""Application assembly service for auto-filling tender applications."""

import json
from datetime import datetime, timezone
from typing import Optional
from uuid import UUID, uuid4

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models.tender import Tender
from src.api.models.tender_application import TenderApplication
from src.api.models.company_profile import CompanyProfile
from src.api.models.answer_library import AnswerLibrary
from src.api.models.document_library import DocumentLibrary
from src.api.models.application_template import ApplicationTemplate


class ApplicationAssemblyService:
    """Service for assembling auto-filled tender applications."""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def generate_draft(
        self, tender_id: UUID, user_id: UUID
    ) -> TenderApplication:
        """
        Generate a draft application with auto-filled fields.

        Args:
            tender_id: Tender to apply for
            user_id: User creating application

        Returns:
            TenderApplication with auto-filled data
        """
        # Get tender
        tender_result = await self.db.execute(
            select(Tender).where(Tender.id == tender_id)
        )
        tender = tender_result.scalar_one_or_none()
        if not tender:
            raise ValueError(f"Tender {tender_id} not found")

        # Get company profile
        profile_result = await self.db.execute(
            select(CompanyProfile).where(CompanyProfile.user_id == user_id)
        )
        profile = profile_result.scalar_one_or_none()

        # Get answer library entries
        answers_result = await self.db.execute(
            select(AnswerLibrary).where(AnswerLibrary.user_id == user_id)
        )
        answers = list(answers_result.scalars().all())

        # Get document library entries
        docs_result = await self.db.execute(
            select(DocumentLibrary).where(DocumentLibrary.user_id == user_id)
        )
        documents = list(docs_result.scalars().all())

        # Get application template (default to joburg if not found)
        template_result = await self.db.execute(
            select(ApplicationTemplate).where(
                ApplicationTemplate.source_id == tender.source_id
            )
        )
        template = template_result.scalar_one_or_none()

        # Build auto-filled fields
        auto_filled_fields = {}

        # Company details
        if profile:
            auto_filled_fields["company_name"] = {
                "value": profile.legal_name,
                "auto_filled": True,
                "source": "CompanyProfile",
            }
            auto_filled_fields["registration_number"] = {
                "value": profile.registration_number or "",
                "auto_filled": True,
                "source": "CompanyProfile",
            }
            auto_filled_fields["contact_details"] = {
                "value": f"{profile.contact_name or ''} - {profile.contact_email or ''} - {profile.contact_phone or ''}",
                "auto_filled": True,
                "source": "CompanyProfile",
            }

        # Capabilities (filtered by tender requirements)
        if profile and tender.requirements_summary:
            requirements_lower = tender.requirements_summary.lower()
            matching_caps = [
                cap
                for cap in (profile.capabilities or [])
                if cap.lower() in requirements_lower
            ]
            auto_filled_fields["capabilities"] = {
                "value": matching_caps,
                "auto_filled": True,
                "source": "CompanyProfile (filtered)",
            }

        # Standard answers from answer library
        for answer in answers:
            if answer.category == "company_overview":
                auto_filled_fields["company_overview"] = {
                    "value": answer.answer_text,
                    "auto_filled": True,
                    "source": f"AnswerLibrary ({answer.category})",
                }
            elif answer.category == "past_projects":
                auto_filled_fields["past_projects"] = {
                    "value": answer.answer_text,
                    "auto_filled": True,
                    "source": f"AnswerLibrary ({answer.category})",
                }

        # Compliance documents
        doc_mapping = {}
        for doc in documents:
            doc_mapping[doc.document_type] = {
                "file_path": doc.file_path,
                "expires_at": doc.expires_at.isoformat() if doc.expires_at else None,
            }

        if doc_mapping:
            auto_filled_fields["compliance_documents"] = {
                "value": doc_mapping,
                "auto_filled": True,
                "source": "DocumentLibrary",
            }

        # Pricing data (empty, to be filled by user)
        pricing_data = None
        if tender.requirements_summary and "pricing" in tender.requirements_summary.lower():
            # Placeholder for pricing
            pricing_data = {
                "line_items": [],
                "grand_total": None,
            }

        # Create application record
        application = TenderApplication(
            id=uuid4(),
            tender_id=tender_id,
            user_id=user_id,
            status="draft",
            auto_filled_fields=auto_filled_fields,
            pricing_data=pricing_data,
            to_do_items=[],  # Will be generated by ToDoGeneratorService
        )

        self.db.add(application)
        await self.db.commit()
        await self.db.refresh(application)

        return application

    async def update_application(
        self,
        application_id: UUID,
        user_id: UUID,
        field_updates: dict,
    ) -> TenderApplication:
        """
        Update application fields.

        Args:
            application_id: Application to update
            user_id: User making update
            field_updates: Dict of field updates {field_name: new_value}

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

        # Update fields
        auto_filled_fields = application.auto_filled_fields.copy()
        for field_name, new_value in field_updates.items():
            if field_name in auto_filled_fields:
                # Mark as manually edited
                auto_filled_fields[field_name] = {
                    "value": new_value,
                    "auto_filled": False,
                    "source": "Manual edit",
                }
            else:
                # Add new field
                auto_filled_fields[field_name] = {
                    "value": new_value,
                    "auto_filled": False,
                    "source": "User input",
                }

        application.auto_filled_fields = auto_filled_fields
        await self.db.commit()
        await self.db.refresh(application)

        return application

    async def update_pricing(
        self,
        application_id: UUID,
        user_id: UUID,
        pricing_data: dict,
    ) -> TenderApplication:
        """
        Update application pricing data.

        Args:
            application_id: Application to update
            user_id: User making update
            pricing_data: Pricing data dict

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

        # Update pricing
        application.pricing_data = pricing_data
        await self.db.commit()
        await self.db.refresh(application)

        return application

    async def submit_application(
        self, application_id: UUID, user_id: UUID
    ) -> TenderApplication:
        """
        Mark application as submitted.

        Args:
            application_id: Application to submit
            user_id: User submitting

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

        # Update status
        application.status = "submitted"
        application.submitted_at = datetime.now(timezone.utc)
        await self.db.commit()
        await self.db.refresh(application)

        return application
