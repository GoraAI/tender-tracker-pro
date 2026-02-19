"""
Tender relevance scoring using LLM.
"""

import asyncio
import json
import re
import time
from typing import List, Optional

import structlog
import yaml

from src.extraction.models import TenderInfo
from src.llm import ModelManager
from .models import ScoredTender, ScoringResult

logger = structlog.get_logger()


class RelevanceScorer:
    """Score tender relevance to user criteria."""

    def __init__(
        self,
        model_manager: ModelManager,
        criteria: dict | None = None,
        criteria_path: str = "config/user/criteria.yaml",
        config_path: str = "config/scoring/relevance_scoring.yaml",
    ):
        """
        Initialize scorer.

        Args:
            model_manager: ModelManager instance with loaded model
            criteria: Optional criteria dict (skips YAML loading if provided)
            criteria_path: Path to user criteria YAML (fallback)
            config_path: Path to scoring config YAML
        """
        # Load criteria from dict or YAML
        if criteria is not None:
            self.criteria = criteria
        else:
            try:
                with open(criteria_path) as f:
                    self.criteria = yaml.safe_load(f)["user_criteria"]
            except FileNotFoundError:
                logger.warning("criteria_yaml_not_found", path=criteria_path)
                self.criteria = {
                    "company_name": "General Business",
                    "industry": "Government Contracting",
                    "capabilities": [],
                    "preferred_sectors": [],
                    "preferred_locations": [],
                    "value_range": {"min": 0, "max": 50000000},
                    "keywords": [],
                }

        # Load config
        try:
            with open(config_path) as f:
                self.config = yaml.safe_load(f)["scoring"]
        except FileNotFoundError:
            logger.warning("scoring_config_not_found", path=config_path)
            self.config = {
                "retry_attempts": 2,
                "retry_delay_ms": 1000,
                "max_concurrent": 3,
                "max_tokens": 500,
                "temperature": 0.3,
                "top_p": 0.9,
                "top_k": 40,
                "repeat_penalty": 1.1,
                "min_score_threshold": 0,
                "sort_by_score": True,
            }

        self.model_manager = model_manager
        self.model = None  # Lazy load

        # Load prompts (with fallback defaults)
        self.system_prompt = "You are a tender relevance scoring assistant. Score tenders from 0-100 based on relevance to the company profile. Respond with valid JSON containing 'score' and 'explanation' fields."
        self.user_prompt_template = None

        if "system_prompt_file" in self.config:
            try:
                with open(self.config["system_prompt_file"]) as f:
                    self.system_prompt = f.read().strip()
            except FileNotFoundError:
                logger.warning("system_prompt_not_found")

        if "user_prompt_template" in self.config:
            try:
                with open(self.config["user_prompt_template"]) as f:
                    self.user_prompt_template = f.read().strip()
            except FileNotFoundError:
                logger.warning("user_prompt_template_not_found")

    def _ensure_model_loaded(self):
        """Ensure model is loaded (lazy loading)."""
        if self.model is None:
            logger.info("loading_model_for_scoring")
            self.model = self.model_manager.load_model()

    async def score_single(
        self,
        tender_info: TenderInfo,
    ) -> ScoredTender:
        """
        Score single tender.

        Args:
            tender_info: Extracted tender information

        Returns:
            ScoredTender with score and explanation

        Raises:
            Exception: If scoring fails after retries
        """
        self._ensure_model_loaded()

        logger.info("scoring_started", pdf_path=str(tender_info.pdf_path))
        start_time = time.time()

        # Retry loop
        last_error = None
        for attempt in range(1, self.config["retry_attempts"] + 1):
            try:
                # Build prompt
                prompt = self._build_prompt(tender_info)

                # Call LLM
                response = await self._call_llm(prompt)

                # Parse response
                score, explanation = self._parse_response(response)

                # Build ScoredTender
                duration_ms = int((time.time() - start_time) * 1000)

                scored_tender = ScoredTender(
                    tender_info=tender_info,
                    score=score,
                    explanation=explanation,
                    scoring_time_ms=duration_ms,
                )

                logger.info(
                    "scoring_success",
                    pdf_path=str(tender_info.pdf_path),
                    score=score,
                    category=scored_tender.relevance_category,
                    duration_ms=duration_ms,
                )

                return scored_tender

            except Exception as e:
                last_error = e
                logger.warning(
                    "scoring_attempt_failed",
                    pdf_path=str(tender_info.pdf_path),
                    attempt=attempt,
                    error=str(e),
                )

                if attempt < self.config["retry_attempts"]:
                    await asyncio.sleep(self.config["retry_delay_ms"] / 1000)

        # All retries failed
        duration_ms = int((time.time() - start_time) * 1000)
        logger.error(
            "scoring_failed",
            pdf_path=str(tender_info.pdf_path),
            error=str(last_error),
            duration_ms=duration_ms,
        )
        raise last_error

    async def score_batch(
        self,
        tender_infos: List[TenderInfo],
        min_score: Optional[int] = None,
    ) -> ScoringResult:
        """
        Score multiple tenders concurrently.

        Args:
            tender_infos: List of extracted tenders
            min_score: Optional minimum score threshold

        Returns:
            ScoringResult with scored tenders sorted by score
        """
        logger.info("scoring_batch_started", total=len(tender_infos))

        semaphore = asyncio.Semaphore(self.config["max_concurrent"])

        async def score_with_limit(tender_info: TenderInfo) -> Optional[ScoredTender]:
            async with semaphore:
                try:
                    return await self.score_single(tender_info)
                except Exception as e:
                    logger.error(
                        "scoring_failed_in_batch",
                        pdf_path=str(tender_info.pdf_path),
                        error=str(e),
                    )
                    return None

        # Create tasks
        tasks = [score_with_limit(ti) for ti in tender_infos]

        # Execute concurrently
        results = await asyncio.gather(*tasks)

        # Filter out failures
        scored_tenders = [r for r in results if r is not None]

        # Apply minimum score threshold
        threshold = min_score or self.config.get("min_score_threshold", 0)
        filtered_tenders = [st for st in scored_tenders if st.score >= threshold]

        # Sort by score (descending)
        if self.config.get("sort_by_score", True):
            filtered_tenders.sort(key=lambda x: x.score, reverse=True)

        # Calculate statistics
        all_scores = [st.score for st in scored_tenders]
        avg_score = sum(all_scores) / len(all_scores) if all_scores else 0.0

        result = ScoringResult(
            scored_tenders=filtered_tenders,
            total_count=len(tender_infos),
            avg_score=avg_score,
            min_score=min(all_scores) if all_scores else 0,
            max_score=max(all_scores) if all_scores else 0,
            filtered_count=len(filtered_tenders),
        )

        logger.info(
            "scoring_batch_complete",
            total=result.total_count,
            scored=len(scored_tenders),
            filtered=result.filtered_count,
            avg_score=result.avg_score,
            threshold=threshold,
        )

        return result

    def _build_prompt(self, tender_info: TenderInfo) -> str:
        """
        Build scoring prompt.

        Args:
            tender_info: Extracted tender information

        Returns:
            Formatted prompt
        """
        # Format criteria
        capabilities_str = "\n  - ".join(self.criteria.get("capabilities", []))
        sectors_str = ", ".join(self.criteria.get("preferred_sectors", []))
        locations_str = ", ".join(self.criteria.get("preferred_locations", []))
        keywords_str = ", ".join(self.criteria.get("keywords", []))

        # Format tender info
        value_str = (
            f"R{tender_info.estimated_value:,.0f}"
            if tender_info.estimated_value
            else "Not specified"
        )
        deadline_str = (
            tender_info.deadline.strftime("%Y-%m-%d")
            if tender_info.deadline
            else "Not specified"
        )
        requirements_str = tender_info.requirements_summary or "Not available"

        # Use template if available, otherwise build a default prompt
        if self.user_prompt_template:
            return self.user_prompt_template.format(
                company_name=self.criteria.get("company_name", "General Business"),
                industry=self.criteria.get("industry", "Government Contracting"),
                capabilities=capabilities_str,
                preferred_sectors=sectors_str,
                preferred_locations=locations_str,
                value_min=self.criteria.get("value_range", {}).get("min", 0),
                value_max=self.criteria.get("value_range", {}).get("max", 50000000),
                keywords=keywords_str,
                tender_title=tender_info.title,
                tender_reference=tender_info.reference,
                tender_deadline=deadline_str,
                tender_value=value_str,
                tender_requirements=requirements_str,
            )

        # Fallback prompt when no template file exists
        return f"""Score this tender for relevance to the company profile.

Company: {self.criteria.get("company_name", "General Business")}
Industry: {self.criteria.get("industry", "Government Contracting")}
Capabilities: {capabilities_str or "Not specified"}
Preferred Sectors: {sectors_str or "Not specified"}
Preferred Locations: {locations_str or "Not specified"}
Keywords: {keywords_str or "Not specified"}

Tender Title: {tender_info.title}
Reference: {tender_info.reference}
Deadline: {deadline_str}
Estimated Value: {value_str}
Requirements: {requirements_str}

Respond with JSON: {{"score": 0-100, "explanation": "brief reason"}}"""

    async def _call_llm(self, prompt: str) -> str:
        """
        Call LLM for scoring.

        Args:
            prompt: Formatted prompt

        Returns:
            LLM response text
        """
        response = self.model.create_chat_completion(
            messages=[
                {"role": "system", "content": self.system_prompt},
                {"role": "user", "content": prompt},
            ],
            max_tokens=self.config["max_tokens"],
            temperature=self.config["temperature"],
            top_p=self.config["top_p"],
            top_k=self.config["top_k"],
            repeat_penalty=self.config["repeat_penalty"],
        )

        return response["choices"][0]["message"]["content"]

    def _parse_response(self, response: str) -> tuple:
        """
        Parse LLM response.

        Args:
            response: Raw LLM output

        Returns:
            Tuple of (score, explanation)

        Raises:
            ValueError: If response is invalid
        """
        # Try to extract JSON
        json_match = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", response, re.DOTALL)
        if json_match:
            json_str = json_match.group(1)
        else:
            json_match = re.search(r"\{.*\}", response, re.DOTALL)
            if json_match:
                json_str = json_match.group(0)
            else:
                raise ValueError("No JSON found in response")

        # Parse JSON
        try:
            data = json.loads(json_str)
        except json.JSONDecodeError as e:
            raise ValueError(f"Invalid JSON: {e}")

        # Extract score
        if "score" not in data:
            raise ValueError("Missing required field: score")

        score = int(data["score"])

        # Validate score range
        if not 0 <= score <= 100:
            raise ValueError(f"Score out of range: {score} (must be 0-100)")

        # Extract explanation
        explanation = data.get("explanation", "No explanation provided")

        # Truncate explanation if too long
        if len(explanation) > 250:
            explanation = explanation[:247] + "..."

        return score, explanation
