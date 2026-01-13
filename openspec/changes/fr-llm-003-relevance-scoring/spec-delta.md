# FR-LLM-003: Relevance Scoring - Spec Delta

**Status**: PROPOSED
**Date**: 2026-01-12

---

## ADDED

### Configuration: config/user/criteria.yaml

```yaml
# User Criteria for Tender Relevance Scoring

user_criteria:
  # Company profile
  company_name: "ABC Construction (Pty) Ltd"
  industry: "Construction & Civil Engineering"

  capabilities:
    - "Road construction and maintenance"
    - "Water pipeline installation and repair"
    - "Building construction (residential and commercial)"
    - "Civil engineering projects"
    - "Project management and supervision"

  # Preferences
  preferred_sectors:
    - "Water and sanitation"
    - "Infrastructure development"
    - "Municipal services"
    - "Roads and transport"

  preferred_locations:
    - "Gauteng"
    - "Johannesburg"
    - "Pretoria"

  # Value range (ZAR)
  value_range:
    min: 100000       # R100k minimum
    max: 5000000      # R5m maximum

  # Keywords of interest
  keywords:
    - "water"
    - "pipeline"
    - "construction"
    - "maintenance"
    - "infrastructure"
    - "civil"
    - "engineering"

  # Scoring weights (must sum to 1.0)
  weights:
    capability_match: 0.4     # Most important
    sector_match: 0.2
    location_match: 0.1
    value_match: 0.1
    keyword_match: 0.2
```

---

### Configuration: config/scoring/relevance_scoring.yaml

```yaml
# Relevance Scoring Configuration

scoring:
  # Model to use
  model: "gemma-3n"

  # Generation parameters
  max_tokens: 512       # Shorter output than extraction
  temperature: 0.2      # Slightly higher for reasoning
  top_p: 0.9
  top_k: 40
  repeat_penalty: 1.1

  # Prompt files
  system_prompt_file: "prompts/relevance_scoring_system.txt"
  user_prompt_template: "prompts/relevance_scoring_user.txt"

  # Processing
  max_concurrent: 5
  timeout_seconds: 30
  retry_attempts: 2
  retry_delay_ms: 1000

  # Filtering and sorting
  min_score_threshold: 50   # Default minimum score
  sort_by_score: true       # Sort descending
  max_results: 100          # Limit returned results

  # Output
  save_scores: true
  log_score_distribution: true
  score_cache_ttl_hours: 24  # Cache scores for 24 hours
```

---

### Prompt: prompts/relevance_scoring_system.txt

```
You are a tender relevance analyzer. Your job is to score how well a tender matches a company's profile, capabilities, and preferences.

Scoring Guidelines:
- 90-100: Excellent match
  * Core capability perfect fit
  * Ideal sector and location
  * Value in sweet spot
  * Multiple keyword matches

- 70-89: Good match
  * Strong capability alignment
  * Reasonable sector/location
  * Value acceptable
  * Some keyword matches

- 50-69: Moderate match
  * Partial capability fit
  * Some sector/location alignment
  * Value within range
  * Few keyword matches

- 30-49: Weak match
  * Little capability overlap
  * Sector/location not ideal
  * Value borderline
  * Minimal keywords

- 0-29: Poor match
  * No clear capability fit
  * Wrong sector/location
  * Value out of range
  * No keyword matches

Be objective and factual. Base your score on clear evidence from the tender requirements and company profile.

Return JSON with score (integer 0-100) and brief explanation (max 200 characters).
```

---

### Prompt: prompts/relevance_scoring_user.txt

```
Score this tender for relevance to our company:

COMPANY PROFILE:
- Name: {company_name}
- Industry: {industry}
- Capabilities: {capabilities}

PREFERENCES:
- Sectors: {preferred_sectors}
- Locations: {preferred_locations}
- Value range: R{value_min:,} - R{value_max:,}
- Keywords: {keywords}

TENDER TO SCORE:
- Title: {tender_title}
- Reference: {tender_reference}
- Deadline: {tender_deadline}
- Estimated Value: {tender_value}
- Requirements Summary: {tender_requirements}

Consider:
1. Do we have the capabilities for this tender?
2. Does it match our preferred sectors?
3. Is the location suitable?
4. Is the value in our range?
5. Do keywords align with our interests?

Return JSON:
{{
  "score": <integer 0-100>,
  "explanation": "<brief reason for score, max 200 chars>"
}}

Return only JSON, no other text.
```

---

### Module: src/scoring/models.py

```python
"""
Data models for tender relevance scoring.
"""

from dataclasses import dataclass

from extraction.models import TenderInfo


@dataclass
class ScoredTender:
    """Tender with relevance score."""

    tender_info: TenderInfo
    score: int  # 0-100
    explanation: str
    scoring_time_ms: int

    def to_dict(self) -> dict:
        """Convert to dictionary for JSON serialization."""
        return {
            "tender_info": self.tender_info.to_dict(),
            "score": self.score,
            "explanation": self.explanation,
            "scoring_time_ms": self.scoring_time_ms,
        }

    @property
    def relevance_category(self) -> str:
        """Get human-readable relevance category."""
        if self.score >= 90:
            return "Excellent"
        elif self.score >= 70:
            return "Good"
        elif self.score >= 50:
            return "Moderate"
        elif self.score >= 30:
            return "Weak"
        else:
            return "Poor"


@dataclass
class ScoringResult:
    """Result of batch scoring operation."""

    scored_tenders: list  # List[ScoredTender]
    total_count: int
    avg_score: float
    min_score: int
    max_score: int
    filtered_count: int  # After applying threshold
```

---

### Module: src/scoring/relevance_scorer.py

```python
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

from extraction.models import TenderInfo
from llm import ModelManager
from .models import ScoredTender, ScoringResult

logger = structlog.get_logger()


class RelevanceScorer:
    """Score tender relevance to user criteria."""

    def __init__(
        self,
        model_manager: ModelManager,
        criteria_path: str = "config/user/criteria.yaml",
        config_path: str = "config/scoring/relevance_scoring.yaml",
    ):
        """
        Initialize scorer.

        Args:
            model_manager: ModelManager instance with loaded model
            criteria_path: Path to user criteria YAML
            config_path: Path to scoring config YAML
        """
        # Load criteria
        with open(criteria_path) as f:
            self.criteria = yaml.safe_load(f)["user_criteria"]

        # Load config
        with open(config_path) as f:
            self.config = yaml.safe_load(f)["scoring"]

        self.model_manager = model_manager
        self.model = None  # Lazy load

        # Load prompts
        system_prompt_path = self.config["system_prompt_file"]
        user_prompt_path = self.config["user_prompt_template"]

        with open(system_prompt_path) as f:
            self.system_prompt = f.read().strip()

        with open(user_prompt_path) as f:
            self.user_prompt_template = f.read().strip()

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
        capabilities_str = "\n  - ".join(self.criteria["capabilities"])
        sectors_str = ", ".join(self.criteria["preferred_sectors"])
        locations_str = ", ".join(self.criteria["preferred_locations"])
        keywords_str = ", ".join(self.criteria["keywords"])

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

        # Build prompt
        return self.user_prompt_template.format(
            company_name=self.criteria["company_name"],
            industry=self.criteria["industry"],
            capabilities=capabilities_str,
            preferred_sectors=sectors_str,
            preferred_locations=locations_str,
            value_min=self.criteria["value_range"]["min"],
            value_max=self.criteria["value_range"]["max"],
            keywords=keywords_str,
            tender_title=tender_info.title,
            tender_reference=tender_info.reference,
            tender_deadline=deadline_str,
            tender_value=value_str,
            tender_requirements=requirements_str,
        )

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
```

---

### Module: src/scoring/__init__.py

```python
"""
Scoring module for tender relevance scoring.

Components:
- RelevanceScorer: Score tender relevance to user criteria
- ScoredTender: Tender with relevance score
- ScoringResult: Batch scoring result
"""

from .models import ScoredTender, ScoringResult
from .relevance_scorer import RelevanceScorer

__all__ = [
    "RelevanceScorer",
    "ScoredTender",
    "ScoringResult",
]
```

---

## MODIFIED

None - This is a new feature with no modifications to existing code.

---

## REMOVED

None - This is a new feature with no removals.
