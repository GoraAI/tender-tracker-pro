"""Add analytics tables for zero-mock-data production.

Revision ID: 002_analytics_tables
Revises: 001_initial_schema
Create Date: 2026-01-17

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = '002_analytics_tables'
down_revision: Union[str, None] = '001_initial_schema'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Create company_profiles table
    op.create_table(
        'company_profiles',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('legal_name', sa.String(255), nullable=False),
        sa.Column('trading_name', sa.String(255), nullable=True),
        sa.Column('industry', sa.String(100), nullable=False),
        sa.Column('capabilities', postgresql.JSONB(), nullable=True, server_default='[]'),
        sa.Column('preferred_sectors', postgresql.JSONB(), nullable=True, server_default='[]'),
        sa.Column('preferred_provinces', postgresql.JSONB(), nullable=True, server_default='[]'),
        sa.Column('keywords', postgresql.JSONB(), nullable=True, server_default='[]'),
        sa.Column('value_range_min', sa.Numeric(15, 2), nullable=True),
        sa.Column('value_range_max', sa.Numeric(15, 2), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('user_id', name='uq_company_profile_user')
    )

    # Create compliance_registrations table
    op.create_table(
        'compliance_registrations',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('type', sa.String(20), nullable=False),
        sa.Column('name', sa.String(255), nullable=False),
        sa.Column('short_name', sa.String(20), nullable=False),
        sa.Column('registration_number', sa.String(100), nullable=False),
        sa.Column('issue_date', sa.Date(), nullable=False),
        sa.Column('expiry_date', sa.Date(), nullable=True),
        sa.Column('expiry_type', sa.String(20), nullable=False),
        sa.Column('renewal_url', sa.String(500), nullable=True),
        sa.Column('documents', postgresql.JSONB(), nullable=True, server_default='[]'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('idx_compliance_user', 'compliance_registrations', ['user_id'])
    op.create_index('idx_compliance_type', 'compliance_registrations', ['type'])

    # Create cost_items table
    op.create_table(
        'cost_items',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('tender_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('category', sa.String(50), nullable=False),
        sa.Column('description', sa.Text(), nullable=False),
        sa.Column('amount', sa.Numeric(12, 2), nullable=False),
        sa.Column('date', sa.Date(), nullable=False),
        sa.Column('recurring', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('frequency', sa.String(20), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['tender_id'], ['tenders.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('idx_cost_items_user', 'cost_items', ['user_id'])
    op.create_index('idx_cost_items_category', 'cost_items', ['category'])

    # Create tender_outcomes table
    op.create_table(
        'tender_outcomes',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('tender_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('outcome', sa.String(20), nullable=False),
        sa.Column('reason_code', sa.String(50), nullable=True),
        sa.Column('reason_details', sa.Text(), nullable=True),
        sa.Column('award_date', sa.Date(), nullable=False),
        sa.Column('award_amount', sa.Numeric(15, 2), nullable=True),
        sa.Column('payment_received_date', sa.Date(), nullable=True),
        sa.Column('days_to_payment', sa.Integer(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['tender_id'], ['tenders.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('user_id', 'tender_id', name='uq_tender_outcome_user_tender')
    )
    op.create_index('idx_tender_outcomes_user', 'tender_outcomes', ['user_id'])
    op.create_index('idx_tender_outcomes_outcome', 'tender_outcomes', ['outcome'])

    # Create pipeline_tenders table
    op.create_table(
        'pipeline_tenders',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('tender_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('status', sa.String(20), nullable=False),
        sa.Column('start_date', sa.Date(), nullable=True),
        sa.Column('phases', postgresql.JSONB(), nullable=True, server_default='[]'),
        sa.Column('assigned_to', sa.String(255), nullable=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('has_conflict', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['tender_id'], ['tenders.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('user_id', 'tender_id', name='uq_pipeline_user_tender')
    )
    op.create_index('idx_pipeline_tenders_user', 'pipeline_tenders', ['user_id'])
    op.create_index('idx_pipeline_tenders_status', 'pipeline_tenders', ['status'])


def downgrade() -> None:
    op.drop_table('pipeline_tenders')
    op.drop_table('tender_outcomes')
    op.drop_table('cost_items')
    op.drop_table('compliance_registrations')
    op.drop_table('company_profiles')
