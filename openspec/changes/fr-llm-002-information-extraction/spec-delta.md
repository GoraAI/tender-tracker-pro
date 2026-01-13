# FR-LLM-002: Tender Information Extraction - Spec Delta

**Status**: PROPOSED
**Date**: 2026-01-12

---

## ADDED

### Configuration: config/extraction/tender_extraction.yaml

```yaml
# Tender Information Extraction Configuration

extraction:
  # Model to use (from ModelManager)
  model: "gemma-3n"

  # Generation parameters
  max_tokens: 2048
  temperature: 0.1      # Low for structured output
  top_p: 0.9
  top_k: 40
  repeat_penalty: 1.1

  # Prompt files
  system_prompt_file: "prompts/tender_extraction_system.txt"
  user_prompt_template: "prompts/tender_extraction_user.txt"

  # Processing
  max_concurrent: 3
  timeout_seconds: 60
  retry_attempts: 2
  retry_delay_ms: 1000

  # Quality thresholds
  min_confidence: 0.5   # Warn if below
  require_title: true
  require_reference: true

  # Output
  save_raw_response: true
  log_extraction_quality: true
  output_format: "json"
```

---

### Prompt: prompts/tender_extraction_system.txt

```
You are a tender document analyzer specialized in extracting structured information from South African government tender documents.

Your task is to extract key information and return it as valid JSON.

Guidelines:
- Extract information accurately from the document text
- Return only valid JSON, no other text or formatting
- Use null for missing fields
- For dates, use ISO 8601 format (YYYY-MM-DD)
- For amounts, extract numeric value only (no currency symbols)
- Keep requirements_summary under 500 characters
- Be precise and factual - do not invent information
```

---

### Prompt: prompts/tender_extraction_user.txt

```
Extract tender information from this document:

{tender_text}

Return JSON with these exact fields:
{{
  "title": "string - tender title or subject",
  "reference": "string - RFQ/RFP/tender reference number",
  "deadline": "string|null - submission deadline in YYYY-MM-DD format",
  "estimated_value": "number|null - estimated value in ZAR",
  "requirements_summary": "string|null - key requirements, max 500 chars",
  "contact_name": "string|null - contact person name",
  "contact_email": "string|null - contact email address"
}}

Return only the JSON object, no other text.
```

---

### Module: src/extraction/models.py

```python
"""
Data models for tender information extraction.
"""

from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Optional


@dataclass
class TenderInfo:
    """Extracted tender information."""

    # Source
    pdf_path: Path

    # Required fields
    title: str
    reference: str

    # Optional fields
    deadline: Optional[datetime]
    estimated_value: Optional[float]  # ZAR
    requirements_summary: Optional[str]
    contact_name: Optional[str]
    contact_email: Optional[str]

    # Metadata
    confidence: float  # 0.0-1.0
    extraction_time_ms: int
    raw_response: str  # LLM raw output

    def to_dict(self) -> dict:
        """Convert to dictionary for JSON serialization."""
        return {
            "pdf_path": str(self.pdf_path),
            "title": self.title,
            "reference": self.reference,
            "deadline": self.deadline.isoformat() if self.deadline else None,
            "estimated_value": self.estimated_value,
            "requirements_summary": self.requirements_summary,
            "contact_name": self.contact_name,
            "contact_email": self.contact_email,
            "confidence": self.confidence,
            "extraction_time_ms": self.extraction_time_ms,
            "raw_response": self.raw_response,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "TenderInfo":
        """Create TenderInfo from dictionary."""
        return cls(
            pdf_path=Path(data["pdf_path"]),
            title=data["title"],
            reference=data["reference"],
            deadline=(
                datetime.fromisoformat(data["deadline"])
                if data.get("deadline")
                else None
            ),
            estimated_value=data.get("estimated_value"),
            requirements_summary=data.get("requirements_summary"),
            contact_name=data.get("contact_name"),
            contact_email=data.get("contact_email"),
            confidence=data["confidence"],
            extraction_time_ms=data["extraction_time_ms"],
            raw_response=data["raw_response"],
        )


@dataclass
class ExtractionResult:
    """Result of extraction attempt (success or failure)."""

    pdf_path: Path
    success: bool
    tender_info: Optional[TenderInfo]
    error: Optional[str]
```

---

### Module: src/extraction/tender_extractor.py

```python
"""
Tender information extraction using LLM.
"""

import asyncio
import json
import re
import time
from datetime import datetime
from pathlib import Path
from typing import List, Optional, Tuple

import structlog
import yaml

from llm import ModelManager
from .models import ExtractionResult, TenderInfo

logger = structlog.get_logger()


class TenderExtractor:
    """Extract structured tender information using LLM."""

    def __init__(
        self,
        model_manager: ModelManager,
        config_path: str = "config/extraction/tender_extraction.yaml",
    ):
        """
        Initialize extractor.

        Args:
            model_manager: ModelManager instance with loaded model
            config_path: Path to extraction config
        """
        with open(config_path) as f:
            self.config = yaml.safe_load(f)["extraction"]

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
            logger.info("loading_model_for_extraction")
            self.model = self.model_manager.load_model()

    async def extract_single(
        self,
        pdf_path: Path,
        text: str,
    ) -> TenderInfo:
        """
        Extract from single tender.

        Args:
            pdf_path: Path to original PDF
            text: Extracted text from PDF

        Returns:
            TenderInfo with extracted data

        Raises:
            Exception: If extraction fails after retries
        """
        self._ensure_model_loaded()

        logger.info("extraction_started", pdf_path=str(pdf_path))
        start_time = time.time()

        # Retry loop
        last_error = None
        for attempt in range(1, self.config["retry_attempts"] + 1):
            try:
                # Build prompt
                prompt = self._build_prompt(text)

                # Call LLM
                response = await self._call_llm(prompt)

                # Parse response
                extracted = self._parse_response(response)

                # Calculate confidence
                confidence = self._calculate_confidence(extracted, response)

                # Build TenderInfo
                duration_ms = int((time.time() - start_time) * 1000)

                tender_info = TenderInfo(
                    pdf_path=pdf_path,
                    title=extracted["title"],
                    reference=extracted["reference"],
                    deadline=self._parse_date(extracted.get("deadline")),
                    estimated_value=extracted.get("estimated_value"),
                    requirements_summary=extracted.get("requirements_summary"),
                    contact_name=extracted.get("contact_name"),
                    contact_email=extracted.get("contact_email"),
                    confidence=confidence,
                    extraction_time_ms=duration_ms,
                    raw_response=response,
                )

                logger.info(
                    "extraction_success",
                    pdf_path=str(pdf_path),
                    confidence=confidence,
                    duration_ms=duration_ms,
                )

                return tender_info

            except Exception as e:
                last_error = e
                logger.warning(
                    "extraction_attempt_failed",
                    pdf_path=str(pdf_path),
                    attempt=attempt,
                    error=str(e),
                )

                if attempt < self.config["retry_attempts"]:
                    await asyncio.sleep(self.config["retry_delay_ms"] / 1000)

        # All retries failed
        duration_ms = int((time.time() - start_time) * 1000)
        logger.error(
            "extraction_failed",
            pdf_path=str(pdf_path),
            error=str(last_error),
            duration_ms=duration_ms,
        )
        raise last_error

    async def extract_batch(
        self,
        tenders: List[Tuple[Path, str]],
    ) -> List[ExtractionResult]:
        """
        Extract from multiple tenders concurrently.

        Args:
            tenders: List of (pdf_path, text) tuples

        Returns:
            List of ExtractionResult objects
        """
        logger.info("extraction_batch_started", total=len(tenders))

        semaphore = asyncio.Semaphore(self.config["max_concurrent"])

        async def extract_with_limit(pdf_path: Path, text: str) -> ExtractionResult:
            async with semaphore:
                try:
                    tender_info = await self.extract_single(pdf_path, text)
                    return ExtractionResult(
                        pdf_path=pdf_path,
                        success=True,
                        tender_info=tender_info,
                        error=None,
                    )
                except Exception as e:
                    return ExtractionResult(
                        pdf_path=pdf_path,
                        success=False,
                        tender_info=None,
                        error=str(e),
                    )

        # Create tasks
        tasks = [extract_with_limit(path, text) for path, text in tenders]

        # Execute concurrently
        results = await asyncio.gather(*tasks)

        # Log summary
        success_count = sum(1 for r in results if r.success)
        avg_confidence = sum(
            r.tender_info.confidence for r in results if r.success
        ) / max(success_count, 1)

        logger.info(
            "extraction_batch_complete",
            total=len(results),
            success=success_count,
            failed=len(results) - success_count,
            avg_confidence=avg_confidence,
        )

        return results

    def _build_prompt(self, text: str) -> str:
        """
        Build extraction prompt from tender text.

        Args:
            text: Raw tender text

        Returns:
            Formatted prompt
        """
        # Truncate text if too long (leave room for prompt + output)
        max_text_tokens = self.config["max_tokens"] - 500  # Reserve for prompt/output
        max_text_chars = max_text_tokens * 4  # Rough estimate: 1 token ≈ 4 chars

        if len(text) > max_text_chars:
            text = text[:max_text_chars] + "\n[...truncated...]"

        return self.user_prompt_template.format(tender_text=text)

    async def _call_llm(self, prompt: str) -> str:
        """
        Call LLM for extraction.

        Args:
            prompt: Formatted prompt

        Returns:
            LLM response text

        Raises:
            Exception: If LLM call fails
        """
        # Call llama-cpp-python model
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

        # Extract text from response
        return response["choices"][0]["message"]["content"]

    def _parse_response(self, response: str) -> dict:
        """
        Parse LLM JSON response.

        Args:
            response: Raw LLM output

        Returns:
            Parsed JSON dict

        Raises:
            ValueError: If JSON is invalid or missing required fields
        """
        # Try to extract JSON from response (handle markdown code blocks)
        json_match = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", response, re.DOTALL)
        if json_match:
            json_str = json_match.group(1)
        else:
            # Try to find JSON object directly
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

        # Validate required fields
        if "title" not in data or not data["title"]:
            raise ValueError("Missing required field: title")
        if "reference" not in data or not data["reference"]:
            raise ValueError("Missing required field: reference")

        return data

    def _parse_date(self, date_str: Optional[str]) -> Optional[datetime]:
        """
        Parse date string to datetime.

        Args:
            date_str: Date string (various formats)

        Returns:
            datetime or None if unparseable
        """
        if not date_str:
            return None

        # Try common formats
        formats = [
            "%Y-%m-%d",  # ISO 8601
            "%d/%m/%Y",  # DD/MM/YYYY
            "%m/%d/%Y",  # MM/DD/YYYY
            "%d-%m-%Y",  # DD-MM-YYYY
            "%Y/%m/%d",  # YYYY/MM/DD
        ]

        for fmt in formats:
            try:
                return datetime.strptime(date_str, fmt)
            except ValueError:
                continue

        logger.warning("date_parse_failed", date_str=date_str)
        return None

    def _calculate_confidence(self, extracted: dict, raw_response: str) -> float:
        """
        Calculate extraction confidence score.

        Args:
            extracted: Parsed extraction dict
            raw_response: Raw LLM output

        Returns:
            Confidence score (0.0-1.0)
        """
        score = 0.0
        max_score = 7.0  # Total possible points

        # Required fields (1 point each)
        if extracted.get("title"):
            score += 1.0
        if extracted.get("reference"):
            score += 1.0

        # Optional fields (0.5 points each)
        if extracted.get("deadline"):
            score += 0.5
        if extracted.get("estimated_value"):
            score += 0.5
        if extracted.get("requirements_summary"):
            score += 0.5
        if extracted.get("contact_name"):
            score += 0.5
        if extracted.get("contact_email"):
            score += 0.5

        # JSON formatting quality (2 points)
        # Well-formed JSON that parsed successfully
        score += 2.0

        # Response clarity (1 point)
        # No extra text, clean JSON
        if raw_response.strip().startswith("{") and raw_response.strip().endswith("}"):
            score += 1.0
        elif "```json" in raw_response or "```" in raw_response:
            score += 0.5  # Partial credit for markdown

        return min(score / max_score, 1.0)
```

---

### Module: src/extraction/__init__.py

```python
"""
Extraction module for tender information extraction.

Components:
- TenderExtractor: Extract structured tender info from PDF text
- TenderInfo: Extracted tender data model
- ExtractionResult: Extraction result (success/failure)
"""

from .models import ExtractionResult, TenderInfo
from .tender_extractor import TenderExtractor

__all__ = [
    "TenderExtractor",
    "TenderInfo",
    "ExtractionResult",
]
```

---

## MODIFIED

None - This is a new feature with no modifications to existing code.

---

## REMOVED

None - This is a new feature with no removals.
