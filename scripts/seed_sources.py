#!/usr/bin/env python3
"""Seed the database with tender sources from config files."""

import asyncio
import sys
from pathlib import Path

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent))

import yaml
from sqlalchemy import select

from src.api.database import async_session_maker, init_db
from src.api.models.source import Source


async def seed_sources():
    """Seed sources from config/sources/*.yaml files."""
    # Initialize database tables
    await init_db()

    config_dir = Path("config/sources")
    if not config_dir.exists():
        print(f"Config directory not found: {config_dir}")
        return

    async with async_session_maker() as db:
        for config_file in config_dir.glob("*.yaml"):
            print(f"Processing: {config_file}")

            with open(config_file) as f:
                config = yaml.safe_load(f)

            source_config = config.get("source", {})
            urls_config = config.get("urls", {})

            source_id = source_config.get("id", config_file.stem)
            name = source_config.get("name", source_id)
            url = urls_config.get("rfq_listing") or urls_config.get("main_site", "")

            # Check if source already exists
            existing = await db.execute(
                select(Source).where(Source.id == source_id)
            )
            if existing.scalar_one_or_none():
                print(f"  Source already exists: {source_id}")
                continue

            # Create source
            source = Source(
                id=source_id,
                name=name,
                url=url,
                config_path=str(config_file),
                is_active=source_config.get("enabled", True),
            )
            db.add(source)
            print(f"  Added source: {source_id} ({name})")

        await db.commit()
        print("\nSeed complete!")


if __name__ == "__main__":
    asyncio.run(seed_sources())
