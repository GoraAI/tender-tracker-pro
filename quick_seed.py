#!/usr/bin/env python3
"""Quick database seeding with SQLAlchemy ORM."""

import asyncio
from datetime import datetime, timedelta
from pathlib import Path
from uuid import uuid4

from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker
from sqlalchemy import text

# Import models
from src.api.models import Base, Tender, Source, User, UserCriteria, Score
from src.api.services.auth_service import hash_password


async def main():
    # Create engine and session
    engine = create_async_engine("sqlite+aiosqlite:///./tender_tracker.db", echo=False)

    # Create all tables
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    # Create session
    async_session = async_sessionmaker(engine, expire_on_commit=False)

    print("\n" + "="*60)
    print("SEEDING DATABASE")
    print("="*60 + "\n")

    async with async_session() as session:
        # 1. Create source
        source = Source(
            id="jhb-water",
            name="Johannesburg Water",
            url="https://scm.johannesburgwater.co.za/",
            is_active=True
        )
        session.add(source)
        print(f"✅ Created source: {source.name}")

        # 2. Create user
        user = User(
            id=uuid4(),
            email="user@test.com",
            password_hash=hash_password("Password123"),
            full_name="Test User",
            is_active=True
        )
        session.add(user)
        print(f"✅ Created user: {user.email}")

        # 3. Create criteria
        criteria = UserCriteria(
            id=uuid4(),
            user_id=user.id,
            name="Default Criteria",
            capabilities=["construction", "water infrastructure"],
            preferred_sectors=["water", "utilities"],
            preferred_locations=["johannesburg"],
            value_range={"min": 100000, "max": 10000000},
            keywords=["water", "infrastructure"],
            weights={"capability_match": 0.4, "keyword_match": 0.6},
            is_default=True
        )
        session.add(criteria)
        print(f"✅ Created criteria: {criteria.name}")

        await session.commit()

        # 4. Create tenders with extracted text
        extracted_dir = Path("documents/extracted/jhb-water/2026-01")
        pdfs = [
            ("MBD-4.pdf", "MBD-4.txt", "Declaration of Interest Form", "MBD-4-2026"),
            ("MBD-8.pdf", "MBD-8.txt", "Declaration Form", "MBD-8-2026"),
            ("Quotation-Conditions.pdf", "Quotation-Conditions.txt", "Quotation Conditions", "QC-2026"),
        ]

        for pdf_name, txt_name, title, ref in pdfs:
            text_file = extracted_dir / txt_name
            if not text_file.exists():
                continue

            extracted_text = text_file.read_text()

            tender = Tender(
                id=uuid4(),
                source_id=source.id,
                pdf_url=f"http://example.com/{pdf_name}",
                pdf_path=f"documents/downloads/jhb-water/2026-01/{pdf_name}",
                title=title,
                reference=ref,
                deadline=datetime.now() + timedelta(days=30),
                estimated_value=500000.00,
                requirements_summary=f"Tender documentation: {title}",
                raw_text=extracted_text,
                discovered_at=datetime.now(),
                extracted_at=datetime.now()
            )
            session.add(tender)
            await session.flush()

            # Create score
            score_value = 70  # Default score
            score = Score(
                id=uuid4(),
                tender_id=tender.id,
                user_id=user.id,
                criteria_id=criteria.id,
                score=score_value,
                explanation=f"Test score for {title}",
                relevance_category="medium",
                scored_at=datetime.now()
            )
            session.add(score)

            print(f"✅ Created tender: {title} (score: {score_value})")

        await session.commit()

        # Get counts
        result = await session.execute(text("SELECT COUNT(*) FROM tenders"))
        tender_count = result.scalar()
        result = await session.execute(text("SELECT COUNT(*) FROM scores"))
        score_count = result.scalar()

        print("\n" + "="*60)
        print("SEEDING COMPLETE")
        print("="*60)
        print(f"\nDatabase contains:")
        print(f"  - {tender_count} tenders")
        print(f"  - {score_count} scores")
        print(f"\nLogin: user@test.com / Password123")
        print("\n✅ Ready for testing!\n")

    await engine.dispose()


if __name__ == "__main__":
    asyncio.run(main())
