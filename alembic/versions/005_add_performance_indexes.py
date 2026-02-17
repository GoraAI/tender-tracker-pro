"""add_performance_indexes

Revision ID: 005_add_performance_indexes
Revises: ba3d97c1ff47
Create Date: 2026-02-17 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op


# revision identifiers, used by Alembic.
revision: str = '005_add_performance_indexes'
down_revision: Union[str, None] = 'ba3d97c1ff47'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.execute("CREATE INDEX IF NOT EXISTS idx_scores_user_id ON scores (user_id);")
    op.execute("CREATE INDEX IF NOT EXISTS idx_scores_tender_id ON scores (tender_id);")
    op.execute("CREATE INDEX IF NOT EXISTS idx_jobs_user_id ON jobs (user_id);")
    op.execute("CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs (status);")
    op.execute("CREATE INDEX IF NOT EXISTS idx_tenders_user_score ON tenders (source_id, discovered_at DESC);")


def downgrade() -> None:
    op.execute("DROP INDEX IF EXISTS idx_scores_user_id;")
    op.execute("DROP INDEX IF EXISTS idx_scores_tender_id;")
    op.execute("DROP INDEX IF EXISTS idx_jobs_user_id;")
    op.execute("DROP INDEX IF EXISTS idx_jobs_status;")
    op.execute("DROP INDEX IF EXISTS idx_tenders_user_score;")
