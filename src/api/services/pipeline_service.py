"""Pipeline service for job orchestration."""

import asyncio
from datetime import datetime
from pathlib import Path
from typing import Optional
from uuid import UUID

import yaml
from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from ..models.job import Job, JobLog
from ..models.tender import Tender
from ..models.source import Source
from ..database import async_session_maker as async_session_factory


class PipelineService:
    """Service for managing pipeline jobs."""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def create_job(
        self,
        user_id: UUID,
        job_type: str,
        source_id: Optional[str] = None,
    ) -> Job:
        """Create a new pipeline job."""
        job = Job(
            user_id=user_id,
            job_type=job_type,
            source_id=source_id,
            status="pending",
            progress=0,
            processed_items=0,
        )
        self.db.add(job)
        await self.db.commit()
        await self.db.refresh(job)
        return job

    async def get_job(self, job_id: UUID, user_id: UUID) -> Optional[Job]:
        """Get a job by ID for a specific user."""
        result = await self.db.execute(
            select(Job).where(Job.id == job_id, Job.user_id == user_id)
        )
        return result.scalar_one_or_none()

    async def get_user_jobs(
        self, user_id: UUID, limit: int = 20, offset: int = 0
    ) -> list[Job]:
        """Get jobs for a user."""
        result = await self.db.execute(
            select(Job)
            .where(Job.user_id == user_id)
            .order_by(Job.created_at.desc())
            .limit(limit)
            .offset(offset)
        )
        return list(result.scalars().all())

    async def get_job_logs(self, job_id: UUID) -> list[JobLog]:
        """Get logs for a job."""
        result = await self.db.execute(
            select(JobLog)
            .where(JobLog.job_id == job_id)
            .order_by(JobLog.created_at.asc())
        )
        return list(result.scalars().all())

    async def add_log(
        self,
        job_id: UUID,
        message: str,
        level: str = "info",
        extra_data: Optional[dict] = None,
    ) -> JobLog:
        """Add a log entry to a job."""
        log = JobLog(
            job_id=job_id,
            level=level,
            message=message,
            extra_data=extra_data,
        )
        self.db.add(log)
        await self.db.commit()
        await self.db.refresh(log)
        return log

    async def update_job_status(
        self,
        job_id: UUID,
        status: str,
        progress: Optional[int] = None,
        processed_items: Optional[int] = None,
        total_items: Optional[int] = None,
        error_message: Optional[str] = None,
    ):
        """Update job status and progress."""
        update_data = {"status": status}
        if progress is not None:
            update_data["progress"] = progress
        if processed_items is not None:
            update_data["processed_items"] = processed_items
        if total_items is not None:
            update_data["total_items"] = total_items
        if error_message is not None:
            update_data["error_message"] = error_message
        if status == "running" and "started_at" not in update_data:
            update_data["started_at"] = datetime.utcnow()
        if status in ("completed", "failed"):
            update_data["completed_at"] = datetime.utcnow()

        await self.db.execute(
            update(Job).where(Job.id == job_id).values(**update_data)
        )
        await self.db.commit()

    async def cancel_job(self, job_id: UUID, user_id: UUID) -> Optional[Job]:
        """Cancel a running job."""
        job = await self.get_job(job_id, user_id)
        if not job or job.status not in ("pending", "running"):
            return None

        await self.update_job_status(job_id, "failed", error_message="Cancelled by user")
        await self.add_log(job_id, "Job cancelled by user", level="warning")

        return await self.get_job(job_id, user_id)

    # Background task methods - these run in separate context
    async def run_discover(self, job_id: UUID):
        """Background task for PDF discovery."""
        async with async_session_factory() as db:
            service = PipelineService(db)
            try:
                await service.update_job_status(job_id, "running")
                await service.add_log(job_id, "Starting PDF discovery...")

                # Get job to check source_id
                result = await db.execute(select(Job).where(Job.id == job_id))
                job = result.scalar_one_or_none()
                source_id = job.source_id or "jhb-water"

                await service.add_log(job_id, f"Loading source config: {source_id}")

                # Load source configuration
                config_path = Path(f"config/sources/{source_id.replace('-', '_')}.yaml")
                if not config_path.exists():
                    raise Exception(f"Source config not found: {config_path}")

                with open(config_path) as f:
                    config = yaml.safe_load(f)

                # Import navigator components
                from src.navigator import BrowserEngine, PDFDiscovery

                # Configure browser
                browser_config = {
                    "browser": config["navigation"]["browser"],
                    "timeouts": {"page_load": 30, "navigation": 60},
                    "delays": config["navigation"]["delays"],
                    "max_retries": 3,
                    "screenshot_on_error": True,
                }

                await service.add_log(job_id, "Initializing browser engine...")
                engine = BrowserEngine(browser_config)
                await engine.initialize()

                try:
                    # Navigate to RFQ listing
                    rfq_url = config["urls"]["rfq_listing"]
                    await service.add_log(job_id, f"Navigating to: {rfq_url}")
                    page = await engine.navigate(rfq_url)

                    # Set up PDF discovery with tracking file
                    discovery_config = config.get("pdf_discovery", {})
                    tracking_file = Path(f"data/tracking/{source_id}_pdfs.json")
                    tracking_file.parent.mkdir(parents=True, exist_ok=True)
                    discovery = PDFDiscovery(discovery_config, tracking_file)

                    await service.add_log(job_id, "Discovering PDF links...")
                    pdf_links = await discovery.discover_pdfs(page, source_id)

                    total = len(pdf_links)
                    await service.update_job_status(
                        job_id, "running", total_items=total, progress=100
                    )
                    await service.add_log(job_id, f"Discovered {total} PDF links")

                    # Store discovered links in database
                    for link in pdf_links:
                        # Check if tender already exists
                        existing = await db.execute(
                            select(Tender).where(
                                Tender.source_id == source_id,
                                Tender.pdf_url == link.url
                            )
                        )
                        if existing.scalar_one_or_none():
                            continue

                        # Extract filename from URL for reference
                        filename = link.url.split("/")[-1].split("?")[0]

                        tender = Tender(
                            source_id=source_id,
                            pdf_url=link.url,
                            title=link.link_text or link.title or filename,
                            reference=filename,
                        )
                        db.add(tender)

                    await db.commit()

                    # Save discovered PDFs to tracking file
                    discovery.save_tracking(pdf_links)

                    await service.update_job_status(job_id, "completed", processed_items=total)
                    await service.add_log(job_id, "Discovery complete")

                finally:
                    await engine.close()

            except Exception as e:
                await service.update_job_status(
                    job_id, "failed", error_message=str(e)
                )
                await service.add_log(job_id, f"Discovery failed: {e}", level="error")

    async def run_download(self, job_id: UUID):
        """Background task for PDF downloads."""
        async with async_session_factory() as db:
            service = PipelineService(db)
            try:
                await service.update_job_status(job_id, "running")
                await service.add_log(job_id, "Starting PDF downloads...")

                # Get job source
                result = await db.execute(select(Job).where(Job.id == job_id))
                job = result.scalar_one_or_none()
                source_id = job.source_id or "jhb-water"

                # Load config
                config_path = Path(f"config/sources/{source_id.replace('-', '_')}.yaml")
                with open(config_path) as f:
                    config = yaml.safe_load(f)

                # Get tenders needing download
                tenders_result = await db.execute(
                    select(Tender).where(
                        Tender.source_id == source_id,
                        Tender.pdf_path.is_(None)
                    )
                )
                tenders = list(tenders_result.scalars().all())
                total = len(tenders)

                if total == 0:
                    await service.add_log(job_id, "No PDFs to download")
                    await service.update_job_status(job_id, "completed", progress=100)
                    return

                await service.update_job_status(job_id, "running", total_items=total)
                await service.add_log(job_id, f"Found {total} PDFs to download")

                # Import downloader
                from src.navigator import PDFDownloader

                download_config = config.get("download", {})
                downloader = PDFDownloader(download_config)

                downloaded = 0
                for i, tender in enumerate(tenders):
                    try:
                        # Create PDFLink-like object
                        pdf_link = type('PDFLink', (), {
                            'url': tender.pdf_url,
                            'filename': tender.reference or f"tender_{tender.id}.pdf"
                        })()

                        result = await asyncio.to_thread(
                            downloader.download, pdf_link, source_id
                        )

                        if result.success:
                            tender.pdf_path = str(result.path)
                            await db.commit()
                            downloaded += 1

                        progress = int((i + 1) / total * 100)
                        await service.update_job_status(
                            job_id, "running", progress=progress, processed_items=downloaded
                        )
                        await service.add_log(job_id, f"Downloaded: {tender.reference}")

                    except Exception as e:
                        await service.add_log(
                            job_id, f"Failed to download {tender.reference}: {e}",
                            level="warning"
                        )

                await service.update_job_status(job_id, "completed")
                await service.add_log(job_id, f"Download complete: {downloaded}/{total} files")

            except Exception as e:
                await service.update_job_status(job_id, "failed", error_message=str(e))
                await service.add_log(job_id, f"Download failed: {e}", level="error")

    async def run_extract(self, job_id: UUID):
        """Background task for text extraction."""
        async with async_session_factory() as db:
            service = PipelineService(db)
            try:
                await service.update_job_status(job_id, "running")
                await service.add_log(job_id, "Starting text extraction...")

                # Get job source
                result = await db.execute(select(Job).where(Job.id == job_id))
                job = result.scalar_one_or_none()
                source_id = job.source_id or "jhb-water"

                # Get tenders needing extraction
                tenders_result = await db.execute(
                    select(Tender).where(
                        Tender.source_id == source_id,
                        Tender.pdf_path.isnot(None),
                        Tender.raw_text.is_(None)
                    )
                )
                tenders = list(tenders_result.scalars().all())
                total = len(tenders)

                if total == 0:
                    await service.add_log(job_id, "No PDFs to extract")
                    await service.update_job_status(job_id, "completed", progress=100)
                    return

                await service.update_job_status(job_id, "running", total_items=total)
                await service.add_log(job_id, f"Found {total} PDFs to extract")

                # Use pdfplumber for text extraction
                import pdfplumber

                extracted = 0
                for i, tender in enumerate(tenders):
                    try:
                        pdf_path = Path(tender.pdf_path)
                        if not pdf_path.exists():
                            raise Exception(f"PDF not found: {pdf_path}")

                        # Extract text
                        text_parts = []
                        with pdfplumber.open(pdf_path) as pdf:
                            for page in pdf.pages:
                                page_text = page.extract_text()
                                if page_text:
                                    text_parts.append(page_text)

                        tender.raw_text = "\n\n".join(text_parts)
                        tender.extracted_at = datetime.utcnow()
                        await db.commit()

                        extracted += 1
                        progress = int((i + 1) / total * 100)
                        await service.update_job_status(
                            job_id, "running", progress=progress, processed_items=extracted
                        )
                        await service.add_log(job_id, f"Extracted: {tender.reference}")

                    except Exception as e:
                        await service.add_log(
                            job_id, f"Failed to extract {tender.reference}: {e}",
                            level="warning"
                        )

                await service.update_job_status(job_id, "completed")
                await service.add_log(job_id, f"Extraction complete: {extracted}/{total} files")

            except Exception as e:
                await service.update_job_status(job_id, "failed", error_message=str(e))
                await service.add_log(job_id, f"Extraction failed: {e}", level="error")

    async def run_score(self, job_id: UUID):
        """Background task for relevance scoring."""
        async with async_session_factory() as db:
            service = PipelineService(db)
            try:
                await service.update_job_status(job_id, "running")
                await service.add_log(job_id, "Starting relevance scoring...")

                # Get job info
                result = await db.execute(select(Job).where(Job.id == job_id))
                job = result.scalar_one_or_none()
                source_id = job.source_id or "jhb-water"

                # Get tenders needing scoring (have raw_text but no scores)
                from ..models.score import Score

                tenders_result = await db.execute(
                    select(Tender).where(
                        Tender.source_id == source_id,
                        Tender.raw_text.isnot(None)
                    )
                )
                tenders = list(tenders_result.scalars().all())
                total = len(tenders)

                if total == 0:
                    await service.add_log(job_id, "No tenders to score")
                    await service.update_job_status(job_id, "completed", progress=100)
                    return

                await service.update_job_status(job_id, "running", total_items=total)
                await service.add_log(job_id, f"Found {total} tenders to score")

                # Try to use LLM scorer, fall back to keyword matching
                try:
                    from src.llm import ModelManager
                    from src.scoring import RelevanceScorer

                    await service.add_log(job_id, "Loading LLM model for scoring...")
                    model_manager = ModelManager()
                    scorer = RelevanceScorer(model_manager)

                    scored = 0
                    for i, tender in enumerate(tenders):
                        try:
                            from src.extraction.models import TenderInfo

                            tender_info = TenderInfo(
                                title=tender.title,
                                reference=tender.reference,
                                description=tender.raw_text[:2000] if tender.raw_text else "",
                            )

                            result = await asyncio.to_thread(
                                scorer.score, tender_info
                            )

                            # Create score record
                            score = Score(
                                tender_id=tender.id,
                                user_id=job.user_id,
                                score=result.score,
                                explanation=result.explanation,
                                relevance_category=result.category,
                            )
                            db.add(score)
                            await db.commit()

                            scored += 1
                            progress = int((i + 1) / total * 100)
                            await service.update_job_status(
                                job_id, "running", progress=progress, processed_items=scored
                            )
                            await service.add_log(
                                job_id, f"Scored: {tender.reference} = {result.score}"
                            )

                        except Exception as e:
                            await service.add_log(
                                job_id, f"Failed to score {tender.reference}: {e}",
                                level="warning"
                            )

                except ImportError:
                    # Fall back to simple keyword scoring
                    await service.add_log(
                        job_id, "LLM not available, using keyword matching", level="warning"
                    )

                    keywords = ["water", "pipe", "treatment", "supply", "maintenance"]
                    scored = 0

                    for i, tender in enumerate(tenders):
                        try:
                            text = (tender.raw_text or "").lower()
                            matches = sum(1 for kw in keywords if kw in text)
                            score_value = min(100, matches * 20)

                            score = Score(
                                tender_id=tender.id,
                                user_id=job.user_id,
                                score=score_value,
                                explanation=f"Matched {matches} keywords",
                                relevance_category="Good" if score_value >= 60 else "Moderate",
                            )
                            db.add(score)
                            await db.commit()

                            scored += 1
                            progress = int((i + 1) / total * 100)
                            await service.update_job_status(
                                job_id, "running", progress=progress, processed_items=scored
                            )

                        except Exception as e:
                            await service.add_log(
                                job_id, f"Failed to score {tender.reference}: {e}",
                                level="warning"
                            )

                await service.update_job_status(job_id, "completed", progress=100)
                await service.add_log(job_id, f"Scoring complete: {scored}/{total} tenders")

            except Exception as e:
                await service.update_job_status(job_id, "failed", error_message=str(e))
                await service.add_log(job_id, f"Scoring failed: {e}", level="error")

    async def run_full_pipeline(self, job_id: UUID):
        """Background task for full pipeline."""
        async with async_session_factory() as db:
            service = PipelineService(db)
            try:
                await service.update_job_status(job_id, "running")
                await service.add_log(job_id, "Starting full pipeline...")

                # Run each step sequentially
                steps = ["discover", "download", "extract", "score"]

                for i, step_name in enumerate(steps):
                    await service.add_log(job_id, f"Step {i+1}/4: {step_name}")
                    progress = int(i / 4 * 100)
                    await service.update_job_status(job_id, "running", progress=progress)

                    if step_name == "discover":
                        await self.run_discover(job_id)
                    elif step_name == "download":
                        await self.run_download(job_id)
                    elif step_name == "extract":
                        await self.run_extract(job_id)
                    elif step_name == "score":
                        await self.run_score(job_id)

                    # Check if step failed
                    result = await db.execute(select(Job).where(Job.id == job_id))
                    job = result.scalar_one_or_none()
                    if job.status == "failed":
                        await service.add_log(
                            job_id, f"Pipeline stopped at step: {step_name}", level="error"
                        )
                        return

                    await service.add_log(job_id, f"Step {step_name} complete")

                await service.update_job_status(job_id, "completed", progress=100)
                await service.add_log(job_id, "Full pipeline complete")

            except Exception as e:
                await service.update_job_status(job_id, "failed", error_message=str(e))
                await service.add_log(job_id, f"Pipeline failed: {e}", level="error")
