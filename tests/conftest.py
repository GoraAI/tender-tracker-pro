"""
Pytest configuration for Tender Tracker Pro tests.

Adds project root to sys.path for imports.
"""

import sys
from pathlib import Path

# Add project root to sys.path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))
