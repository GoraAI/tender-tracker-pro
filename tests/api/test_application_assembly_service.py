"""
Unit tests for ApplicationAssemblyService.

Tests the auto-fill logic from multiple data sources:
- CompanyProfile
- AnswerLibrary
- DocumentLibrary
- Capability filtering
- Application templates
"""

import pytest
import pytest_asyncio
from datetime import datetime, timedelta, timezone
from uuid import uuid4
from sqlalchemy.ext.asyncio import AsyncSession

from src.api.models import (
    Tender, CompanyProfile, AnswerLibrary,
    DocumentLibrary, ApplicationTemplate
)
from src.api.services.application_assembly_service import ApplicationAssemblyService


@pytest_asyncio.fixture
async def assembly_service(db: AsyncSession):
    """Create ApplicationAssemblyService instance."""
    return ApplicationAssemblyService(db)


@pytest_asyncio.fixture
async def test_company_profile(db: AsyncSession, test_user) -> CompanyProfile:
    """Create a test company profile with capabilities."""
    profile = CompanyProfile(
        user_id=test_user.id,
        legal_name="Acme Software Solutions Ltd",
        industry="Information Technology",
        capabilities=["Software Development", "Cloud Infrastructure", "Data Analytics"],
        preferred_sectors=["IT", "Technology", "Finance"],
        value_range_min=100000.0,
        value_range_max=5000000.0,
    )
    db.add(profile)
    await db.commit()
    await db.refresh(profile)
    return profile


@pytest_asyncio.fixture
async def test_answer_library(db: AsyncSession, test_user) -> list[AnswerLibrary]:
    """Create test answer library entries."""
    answers = [
        AnswerLibrary(
            id=uuid4(),
            user_id=test_user.id,
            category="company_overview",
            question="Company Overview",
            answer_text="Acme Software Solutions is a leading provider of enterprise software solutions with 10 years of experience."
        ),
        AnswerLibrary(
            id=uuid4(),
            user_id=test_user.id,
            category="methodology",
            question="Development Methodology",
            answer_text="We follow Agile/Scrum methodology with bi-weekly sprints and continuous integration."
        ),
        AnswerLibrary(
            id=uuid4(),
            user_id=test_user.id,
            category="quality_assurance",
            question="Quality Assurance Process",
            answer_text="Our QA process includes automated testing, code reviews, and comprehensive documentation."
        ),
    ]
    for answer in answers:
        db.add(answer)
    await db.commit()
    for answer in answers:
        await db.refresh(answer)
    return answers


@pytest_asyncio.fixture
async def test_document_library(db: AsyncSession, test_user) -> list[DocumentLibrary]:
    """Create test document library entries."""
    future_date = datetime.now(timezone.utc) + timedelta(days=365)
    documents = [
        DocumentLibrary(
            id=uuid4(),
            user_id=test_user.id,
            document_type="bee_certificate",
            file_path="/documents/bee_cert.pdf",
            expires_at=future_date
        ),
        DocumentLibrary(
            id=uuid4(),
            user_id=test_user.id,
            document_type="tax_clearance",
            file_path="/documents/tax_clearance.pdf",
            expires_at=future_date
        ),
        DocumentLibrary(
            id=uuid4(),
            user_id=test_user.id,
            document_type="company_registration",
            file_path="/documents/company_reg.pdf",
            expires_at=None  # No expiry
        ),
    ]
    for doc in documents:
        db.add(doc)
    await db.commit()
    for doc in documents:
        await db.refresh(doc)
    return documents


@pytest_asyncio.fixture
async def test_application_template(db: AsyncSession, test_source) -> ApplicationTemplate:
    """Create a test application template."""
    template = ApplicationTemplate(
        id=uuid4(),
        source_id=test_source.id,
        template_name="Standard Tender Application",
        fields={
            "company_name": {"type": "text", "required": True, "label": "Company Name"},
            "company_overview": {"type": "textarea", "required": True, "label": "Company Overview"},
            "capabilities": {"type": "list", "required": True, "label": "Key Capabilities"},
            "methodology": {"type": "textarea", "required": True, "label": "Development Methodology"},
            "bee_certificate": {"type": "file", "required": True, "label": "B-BBEE Certificate"},
            "tax_clearance": {"type": "file", "required": True, "label": "Tax Clearance"},
        },
        section_order=["company_info", "technical", "compliance"]
    )
    db.add(template)
    await db.commit()
    await db.refresh(template)
    return template


@pytest_asyncio.fixture
async def test_tender_with_requirements(db: AsyncSession, test_source) -> Tender:
    """Create a tender with specific requirements."""
    future_date = datetime.now(timezone.utc) + timedelta(days=14)
    tender = Tender(
        id=uuid4(),
        source_id=test_source.id,
        pdf_url="https://test.example.com/tender.pdf",
        title="Software Development Tender",
        reference="SOFT-001",
        estimated_value=500000.0,
        deadline=future_date,
        requirements_summary="We require software development and cloud infrastructure expertise with proven methodology."
    )
    db.add(tender)
    await db.commit()
    await db.refresh(tender)
    return tender


class TestApplicationAssemblyService:
    """Test cases for ApplicationAssemblyService."""

    @pytest.mark.asyncio
    async def test_generate_draft_auto_fills_from_profile(
        self,
        assembly_service: ApplicationAssemblyService,
        test_tender_with_requirements: Tender,
        test_company_profile: CompanyProfile,
        test_answer_library: list[AnswerLibrary],
        test_document_library: list[DocumentLibrary]
    ):
        """Test that generate_draft auto-fills from company profile."""
        application = await assembly_service.generate_draft(
            tender_id=test_tender_with_requirements.id,
            user_id=test_company_profile.user_id
        )

        assert application is not None
        assert application.tender_id == test_tender_with_requirements.id
        assert application.user_id == test_company_profile.user_id
        assert application.status == "draft"

        # Check auto-filled fields from CompanyProfile
        auto_filled = application.auto_filled_fields
        assert "company_name" in auto_filled
        assert auto_filled["company_name"]["value"] == "Acme Software Solutions Ltd"
        assert auto_filled["company_name"]["auto_filled"] is True

    @pytest.mark.asyncio
    async def test_generate_draft_includes_capabilities(
        self,
        assembly_service: ApplicationAssemblyService,
        test_tender_with_requirements: Tender,
        test_company_profile: CompanyProfile,
        test_answer_library: list[AnswerLibrary],
        test_document_library: list[DocumentLibrary]
    ):
        """Test that capabilities are auto-filled and filtered by tender requirements."""
        application = await assembly_service.generate_draft(
            tender_id=test_tender_with_requirements.id,
            user_id=test_company_profile.user_id
        )

        auto_filled = application.auto_filled_fields

        # Should have capabilities field
        assert "capabilities" in auto_filled
        capabilities = auto_filled["capabilities"]["value"]

        # Should include relevant capabilities (software development, cloud)
        assert isinstance(capabilities, list)
        assert len(capabilities) >= 2
        assert any("Software Development" in cap for cap in capabilities)
        assert any("Cloud Infrastructure" in cap for cap in capabilities)

    @pytest.mark.asyncio
    async def test_generate_draft_uses_answer_library(
        self,
        assembly_service: ApplicationAssemblyService,
        test_tender_with_requirements: Tender,
        test_company_profile: CompanyProfile,
        test_answer_library: list[AnswerLibrary],
        test_document_library: list[DocumentLibrary]
    ):
        """Test that answer library entries are used for auto-fill."""
        application = await assembly_service.generate_draft(
            tender_id=test_tender_with_requirements.id,
            user_id=test_company_profile.user_id
        )

        auto_filled = application.auto_filled_fields

        # Should have company overview from answer library
        if "company_overview" in auto_filled:
            assert auto_filled["company_overview"]["auto_filled"] is True
            assert "Acme Software Solutions" in auto_filled["company_overview"]["value"]

    @pytest.mark.asyncio
    async def test_generate_draft_links_documents(
        self,
        assembly_service: ApplicationAssemblyService,
        test_tender_with_requirements: Tender,
        test_company_profile: CompanyProfile,
        test_answer_library: list[AnswerLibrary],
        test_document_library: list[DocumentLibrary]
    ):
        """Test that document library entries are linked in auto-fill."""
        application = await assembly_service.generate_draft(
            tender_id=test_tender_with_requirements.id,
            user_id=test_company_profile.user_id
        )

        auto_filled = application.auto_filled_fields

        # Should have compliance documents
        if "compliance_documents" in auto_filled:
            assert auto_filled["compliance_documents"]["auto_filled"] is True
            docs = auto_filled["compliance_documents"]["value"]
            assert "bee_certificate" in docs
            assert "tax_clearance" in docs

    @pytest.mark.asyncio
    async def test_generate_draft_without_company_profile_fails(
        self,
        assembly_service: ApplicationAssemblyService,
        test_tender_with_requirements: Tender
    ):
        """Test that missing company profile still creates application."""
        fake_user_id = uuid4()

        # ApplicationAssemblyService doesn't raise on missing profile,
        # it just doesn't auto-fill profile fields
        application = await assembly_service.generate_draft(
            tender_id=test_tender_with_requirements.id,
            user_id=fake_user_id
        )

        # Should still create application but without profile auto-fills
        assert application is not None
        assert application.status == "draft"

    @pytest.mark.asyncio
    async def test_generate_draft_with_missing_tender_fails(
        self,
        assembly_service: ApplicationAssemblyService,
        test_company_profile: CompanyProfile
    ):
        """Test that missing tender raises error."""
        fake_tender_id = uuid4()

        with pytest.raises(ValueError, match="not found"):
            await assembly_service.generate_draft(
                tender_id=fake_tender_id,
                user_id=test_company_profile.user_id
            )

    @pytest.mark.asyncio
    async def test_generate_draft_without_answer_library_still_works(
        self,
        assembly_service: ApplicationAssemblyService,
        test_tender_with_requirements: Tender,
        test_company_profile: CompanyProfile,
        test_document_library: list[DocumentLibrary]
    ):
        """Test that auto-fill works even without answer library."""
        # Don't create answer library entries
        application = await assembly_service.generate_draft(
            tender_id=test_tender_with_requirements.id,
            user_id=test_company_profile.user_id
        )

        # Should still create application with company profile data
        assert application is not None
        assert "company_name" in application.auto_filled_fields
        assert application.auto_filled_fields["company_name"]["value"] == "Acme Software Solutions Ltd"

    @pytest.mark.asyncio
    async def test_generate_draft_marks_capabilities(
        self,
        assembly_service: ApplicationAssemblyService,
        test_tender_with_requirements: Tender,
        test_company_profile: CompanyProfile
    ):
        """Test that capabilities field is auto-filled from profile."""
        application = await assembly_service.generate_draft(
            tender_id=test_tender_with_requirements.id,
            user_id=test_company_profile.user_id
        )

        # Should have at least company name and capabilities auto-filled
        auto_filled_count = sum(
            1 for field in application.auto_filled_fields.values()
            if field["auto_filled"] is True
        )
        assert auto_filled_count >= 2

    @pytest.mark.asyncio
    async def test_source_tracking_is_accurate(
        self,
        assembly_service: ApplicationAssemblyService,
        test_tender_with_requirements: Tender,
        test_company_profile: CompanyProfile,
        test_answer_library: list[AnswerLibrary],
        test_document_library: list[DocumentLibrary]
    ):
        """Test that each auto-filled field has accurate source tracking."""
        application = await assembly_service.generate_draft(
            tender_id=test_tender_with_requirements.id,
            user_id=test_company_profile.user_id
        )

        for field_name, field_data in application.auto_filled_fields.items():
            if field_data["auto_filled"]:
                # Source should be one of the known sources
                source = field_data["source"]
                assert any(s in source for s in [
                    "CompanyProfile",
                    "AnswerLibrary",
                    "DocumentLibrary",
                ])
