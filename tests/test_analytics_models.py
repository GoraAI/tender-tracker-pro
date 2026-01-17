"""TDD Tests for Analytics Models - RED Phase.

These tests are written FIRST before implementation.
They should FAIL initially, then pass after implementation.
"""

import pytest
from datetime import date, datetime, timedelta
from decimal import Decimal
from uuid import uuid4

# These imports will fail until we implement the models
from src.api.models.company_profile import CompanyProfile
from src.api.models.compliance_registration import ComplianceRegistration
from src.api.models.cost_item import CostItem
from src.api.models.tender_outcome import TenderOutcome
from src.api.models.pipeline_tender import PipelineTender


class TestCompanyProfileModel:
    """Tests for CompanyProfile model."""

    def test_create_company_profile(self):
        """Should create a company profile with all required fields."""
        profile = CompanyProfile(
            user_id=uuid4(),
            legal_name="TechFlow Solutions (Pty) Ltd",
            trading_name="TechFlow Solutions",
            industry="Information Technology",
            capabilities=["IT Infrastructure", "Cybersecurity"],
            preferred_sectors=["Government - National", "Municipalities"],
            preferred_provinces=["Gauteng", "Western Cape"],
            value_range_min=Decimal("250000"),
            value_range_max=Decimal("15000000"),
            keywords=["ICT", "network", "server"]
        )
        assert profile.legal_name == "TechFlow Solutions (Pty) Ltd"
        assert profile.industry == "Information Technology"
        assert len(profile.capabilities) == 2
        assert profile.value_range_min == Decimal("250000")

    def test_company_profile_has_uuid_id_column(self):
        """Company profile should have UUID id column defined."""
        from sqlalchemy import inspect
        mapper = inspect(CompanyProfile)
        id_column = mapper.columns.get('id')
        assert id_column is not None
        assert 'UUID' in str(id_column.type)


class TestComplianceRegistrationModel:
    """Tests for ComplianceRegistration model."""

    def test_create_compliance_registration(self):
        """Should create a compliance registration."""
        reg = ComplianceRegistration(
            user_id=uuid4(),
            type="bbbee",
            name="B-BBEE Certificate",
            short_name="B-BBEE",
            registration_number="BBBEE-2025-78901",
            issue_date=date(2025, 1, 29),
            expiry_date=date(2026, 1, 29),
            expiry_type="annual",
            renewal_url=None
        )
        assert reg.type == "bbbee"
        assert reg.short_name == "B-BBEE"
        assert reg.expiry_type == "annual"

    def test_compliance_days_remaining_calculation(self):
        """Should calculate days remaining until expiry."""
        reg = ComplianceRegistration(
            user_id=uuid4(),
            type="cidb",
            name="CIDB Registration",
            short_name="CIDB",
            registration_number="CIDB-123456",
            issue_date=date.today() - timedelta(days=30),
            expiry_date=date.today() + timedelta(days=45),
            expiry_type="annual"
        )
        assert reg.days_remaining == 45

    def test_compliance_is_expired(self):
        """Should detect expired registration."""
        reg = ComplianceRegistration(
            user_id=uuid4(),
            type="bbbee",
            name="B-BBEE Certificate",
            short_name="B-BBEE",
            registration_number="BBBEE-OLD",
            issue_date=date(2024, 1, 1),
            expiry_date=date(2025, 1, 1),  # Past date
            expiry_type="annual"
        )
        assert reg.is_expired is True
        assert reg.is_valid is False

    def test_compliance_perpetual_never_expires(self):
        """Perpetual registrations should not expire."""
        reg = ComplianceRegistration(
            user_id=uuid4(),
            type="csd",
            name="Central Supplier Database",
            short_name="CSD",
            registration_number="MAAA0123456",
            issue_date=date(2023, 4, 15),
            expiry_date=None,
            expiry_type="perpetual"
        )
        assert reg.is_expired is False
        assert reg.is_valid is True
        assert reg.days_remaining is None  # No expiry


class TestCostItemModel:
    """Tests for CostItem model."""

    def test_create_cost_item(self):
        """Should create a cost item."""
        item = CostItem(
            user_id=uuid4(),
            category="preparation",
            description="Staff time - JHB Water ICT Tender",
            amount=Decimal("36000.00"),
            date=date(2025, 11, 15),
            tender_id=None,
            recurring=False
        )
        assert item.category == "preparation"
        assert item.amount == Decimal("36000.00")

    def test_cost_item_with_tender_link(self):
        """Cost item can be linked to a tender."""
        tender_id = uuid4()
        item = CostItem(
            user_id=uuid4(),
            category="submission",
            description="Printing and binding",
            amount=Decimal("2800.00"),
            date=date.today(),
            tender_id=tender_id,
            recurring=False
        )
        assert item.tender_id == tender_id

    def test_cost_item_categories(self):
        """Cost items must have valid categories."""
        valid_categories = ["platform", "compliance", "preparation", "submission", "opportunity"]
        for cat in valid_categories:
            item = CostItem(
                user_id=uuid4(),
                category=cat,
                description=f"Test {cat}",
                amount=Decimal("100.00"),
                date=date.today()
            )
            assert item.category == cat


class TestTenderOutcomeModel:
    """Tests for TenderOutcome model."""

    def test_create_outcome_won(self):
        """Should create a winning outcome."""
        outcome = TenderOutcome(
            user_id=uuid4(),
            tender_id=uuid4(),
            outcome="won",
            reason_code=None,
            reason_details=None,
            award_date=date(2025, 5, 20),
            award_amount=Decimal("2750000.00")
        )
        assert outcome.outcome == "won"
        assert outcome.award_amount == Decimal("2750000.00")

    def test_create_outcome_lost(self):
        """Should create a losing outcome with reason."""
        outcome = TenderOutcome(
            user_id=uuid4(),
            tender_id=uuid4(),
            outcome="lost",
            reason_code="price",
            reason_details="Our pricing was 15% higher than winning bidder",
            award_date=date(2025, 8, 25),
            award_amount=None
        )
        assert outcome.outcome == "lost"
        assert outcome.reason_code == "price"
        assert outcome.reason_details is not None

    def test_outcome_reason_codes(self):
        """Outcome reason codes must be valid."""
        valid_reasons = ["price", "functionality", "bbbee", "disqualified", "other"]
        for reason in valid_reasons:
            outcome = TenderOutcome(
                user_id=uuid4(),
                tender_id=uuid4(),
                outcome="lost",
                reason_code=reason,
                award_date=date.today()
            )
            assert outcome.reason_code == reason

    def test_outcome_with_payment_tracking(self):
        """Won outcome should track payment details."""
        outcome = TenderOutcome(
            user_id=uuid4(),
            tender_id=uuid4(),
            outcome="won",
            award_date=date(2025, 5, 20),
            award_amount=Decimal("2750000.00"),
            payment_received_date=date(2025, 9, 15)
        )
        assert outcome.payment_received_date == date(2025, 9, 15)
        # Days to payment should be calculated
        assert outcome.days_to_payment == 118


class TestPipelineTenderModel:
    """Tests for PipelineTender model."""

    def test_create_pipeline_tender(self):
        """Should create a pipeline tender."""
        pipeline = PipelineTender(
            user_id=uuid4(),
            tender_id=uuid4(),
            status="preparation",
            start_date=date(2026, 1, 10),
            phases=[
                {"name": "preparation", "start_date": "2026-01-10", "end_date": "2026-01-18", "status": "in_progress"},
                {"name": "review", "start_date": "2026-01-19", "end_date": "2026-01-20", "status": "pending"},
                {"name": "submission", "start_date": "2026-01-21", "end_date": "2026-01-22", "status": "pending"}
            ],
            assigned_to="Thabo Molefe",
            notes="Follow-up from 2024 contract"
        )
        assert pipeline.status == "preparation"
        assert len(pipeline.phases) == 3
        assert pipeline.assigned_to == "Thabo Molefe"

    def test_pipeline_status_values(self):
        """Pipeline status must be valid."""
        valid_statuses = ["discovered", "qualified", "preparation", "review", "submission", "submitted"]
        for status in valid_statuses:
            pipeline = PipelineTender(
                user_id=uuid4(),
                tender_id=uuid4(),
                status=status
            )
            assert pipeline.status == status

    def test_pipeline_conflict_flag(self):
        """Pipeline tender can have conflict flag."""
        pipeline = PipelineTender(
            user_id=uuid4(),
            tender_id=uuid4(),
            status="preparation",
            has_conflict=True
        )
        assert pipeline.has_conflict is True
