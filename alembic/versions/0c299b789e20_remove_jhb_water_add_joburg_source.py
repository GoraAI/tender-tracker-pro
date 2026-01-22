"""remove_jhb_water_add_joburg_source

Revision ID: 0c299b789e20
Revises: 002_analytics_tables
Create Date: 2026-01-22 17:58:14.308513

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '0c299b789e20'
down_revision: Union[str, None] = '002_analytics_tables'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Remove jhb_water source (non-functional)
    op.execute("""
        DELETE FROM sources WHERE id IN ('jhb-water', 'jhb_water');
    """)

    # Add City of Johannesburg source
    op.execute("""
        INSERT INTO sources (id, name, url, config_path, is_active, last_scraped_at, created_at)
        VALUES (
            'joburg',
            'City of Johannesburg',
            'https://joburg.org.za/work_/Pages/2026-Tenders/Request-for-Quotations.aspx',
            'config/sources/joburg.yaml',
            true,
            NULL,
            NOW()
        )
        ON CONFLICT (id) DO UPDATE SET
            name = EXCLUDED.name,
            url = EXCLUDED.url,
            config_path = EXCLUDED.config_path,
            is_active = EXCLUDED.is_active;
    """)

    # Update SARS source if it exists (ensure correct config path)
    op.execute("""
        INSERT INTO sources (id, name, url, config_path, is_active, last_scraped_at, created_at)
        VALUES (
            'sars',
            'SARS - South African Revenue Service',
            'https://www.sars.gov.za/procurement/published-tenders/',
            'config/sources/sars.yaml',
            true,
            NULL,
            NOW()
        )
        ON CONFLICT (id) DO UPDATE SET
            name = EXCLUDED.name,
            url = EXCLUDED.url,
            config_path = EXCLUDED.config_path,
            is_active = EXCLUDED.is_active;
    """)


def downgrade() -> None:
    # Remove joburg source
    op.execute("""
        DELETE FROM sources WHERE id = 'joburg';
    """)

    # Re-add jhb_water source (for rollback)
    op.execute("""
        INSERT INTO sources (id, name, url, config_path, is_active, last_scraped_at, created_at)
        VALUES (
            'jhb-water',
            'Johannesburg Water',
            'https://www.johannesburgwater.co.za/tenders',
            'config/sources/jhb_water.yaml',
            true,
            NULL,
            NOW()
        )
        ON CONFLICT (id) DO NOTHING;
    """)
