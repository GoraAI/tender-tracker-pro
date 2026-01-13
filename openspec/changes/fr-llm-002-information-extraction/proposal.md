# FR-LLM-002: Tender Information Extraction

**Status**: PROPOSED
**Priority**: P0
**Created**: 2026-01-12
**Dependencies**: FR-LLM-001 (Model Management), FR-EXTRACT-001 (PDF Text Extraction)

---

## Summary

Extract structured tender information from PDF text using Gemma 3n LLM with prompt engineering for precise extraction of key fields (title, deadline, value, requirements).

---

## Motivation

Raw PDF text needs to be converted into structured data:
- Tender titles/reference numbers
- Submission deadlines
- Estimated values/budgets
- Requirements and specifications
- Contact information

This enables:
- Searchable tender database
- Deadline tracking/alerts
- Relevance scoring (FR-LLM-003)
- Analytics and reporting

---

## Scope

### In Scope
- Structured extraction from tender PDF text
- Field extraction: title, reference, deadline, value, requirements
- JSON output format
- Retry logic for failed extractions
- Confidence scoring
- Batch processing support
- Error handling for unparseable text

### Out of Scope
- Fine-tuning Gemma 3n (use zero-shot prompting)
- Image/table extraction (text-only)
- Document classification (assume all are tenders)
- Multi-language support (English only)
- Real-time streaming (batch only)

---

## Acceptance Criteria

- [ ] Can extract title and reference number from tender text
- [ ] Can parse deadline dates (various formats)
- [ ] Can extract estimated value/budget if present
- [ ] Can summarize key requirements (max 500 chars)
- [ ] Returns structured JSON output
- [ ] Handles missing fields gracefully (null values)
- [ ] Provides confidence score (0.0-1.0)
- [ ] Processes batch of extractions concurrently
- [ ] Logs extraction quality metrics
- [ ] Unit tests with sample tender text
- [ ] Integration test with real tender PDFs

---

## Technical Approach

### Architecture

```
TenderExtractor
├── Uses ModelManager (from FR-LLM-001)
├── Uses PDFExtractor text (from FR-EXTRACT-001)
├── Prompt templates for extraction
├── JSON output parsing
└── Confidence scoring
```

### Extraction Flow

```
1. Load text from extracted PDF (FR-EXTRACT-001)
2. Build extraction prompt with text
3. Call Gemma 3n for structured output
4. Parse JSON response
5. Validate and score confidence
6. Return TenderInfo object
```

### Prompt Engineering

**System Prompt**:
```
You are a tender document analyzer. Extract key information from tender documents and return valid JSON.
```

**User Prompt Template**:
```
Extract tender information from this document:

{tender_text}

Return JSON with these fields:
- title: string (tender title/subject)
- reference: string (RFQ/RFP number)
- deadline: string (ISO 8601 date)
- estimated_value: number|null (in ZAR)
- requirements_summary: string (max 500 chars)
- contact_name: string|null
- contact_email: string|null

Return only valid JSON, no other text.
```

### Output Schema

```python
@dataclass
class TenderInfo:
    """Extracted tender information."""

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
```

---

## Implementation Plan

### Phase 1: Data Models
- Create TenderInfo dataclass
- Create ExtractionResult dataclass
- JSON serialization/deserialization

### Phase 2: Prompt Templates
- System prompt design
- User prompt template
- Few-shot examples (if needed)

### Phase 3: TenderExtractor
- Initialize with ModelManager
- Build prompts
- Call LLM
- Parse JSON response
- Error handling

### Phase 4: Confidence Scoring
- Field presence scoring
- Format validation
- Consistency checks

### Phase 5: Batch Processing
- Concurrent extractions
- Rate limiting
- Progress tracking

### Phase 6: Testing
- Unit tests with mock LLM
- Integration tests with real tenders
- Accuracy benchmarks

---

## Configuration

```yaml
# config/extraction/tender_extraction.yaml
extraction:
  # Model settings (from ModelManager)
  model: "gemma-3n"

  # Generation parameters
  max_tokens: 2048
  temperature: 0.1  # Low for structured extraction
  top_p: 0.9

  # Prompt settings
  system_prompt_file: "prompts/tender_extraction_system.txt"
  user_prompt_template: "prompts/tender_extraction_user.txt"

  # Processing
  max_concurrent: 3
  timeout_seconds: 60
  retry_attempts: 2

  # Quality thresholds
  min_confidence: 0.5  # Warn if below
  require_title: true
  require_reference: true

  # Output
  save_raw_response: true
  log_extraction_quality: true
```

---

## API Design

### TenderExtractor

```python
class TenderExtractor:
    """Extract structured tender information using LLM."""

    def __init__(
        self,
        model_manager: ModelManager,
        config: dict,
    ):
        """Initialize with model and config."""

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
        """

    async def extract_batch(
        self,
        tenders: List[Tuple[Path, str]],
    ) -> List[TenderInfo]:
        """
        Extract from multiple tenders concurrently.

        Args:
            tenders: List of (pdf_path, text) tuples

        Returns:
            List of TenderInfo objects
        """

    def _build_prompt(self, text: str) -> str:
        """Build extraction prompt from text."""

    def _parse_response(self, response: str) -> dict:
        """Parse LLM JSON response."""

    def _calculate_confidence(
        self,
        extracted: dict,
        raw_response: str,
    ) -> float:
        """Calculate extraction confidence score."""
```

---

## Performance Targets

- **Extraction time**: < 5 seconds per tender (single-threaded)
- **Batch throughput**: 10+ tenders/minute (concurrent)
- **Accuracy**: > 90% on required fields (title, reference)
- **Confidence**: > 0.7 average confidence score

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| LLM returns invalid JSON | Extraction fails | JSON validation, retry with stricter prompt |
| LLM hallucinates data | Incorrect info | Confidence scoring, require citations |
| Slow inference time | Poor UX | Batch processing, progress tracking |
| Variable tender formats | Low accuracy | Few-shot examples, robust parsing |
| Missing key fields | Incomplete data | Make fields optional, flag low confidence |

---

## Testing Strategy

1. **Unit Tests**:
   - Mock LLM responses
   - Test prompt building
   - Test JSON parsing
   - Test confidence scoring

2. **Integration Tests**:
   - Real Gemma 3n model
   - Sample tender PDFs (5-10 examples)
   - Validate extracted fields
   - Measure accuracy

3. **Accuracy Benchmarks**:
   - Manually label 20 tenders
   - Run extraction
   - Calculate precision/recall
   - Iterate on prompts

---

## Success Metrics

- [ ] > 90% accuracy on title extraction
- [ ] > 85% accuracy on reference extraction
- [ ] > 75% accuracy on deadline extraction
- [ ] Average confidence > 0.7
- [ ] < 5s per extraction
- [ ] All unit tests pass
- [ ] Integration tests pass with real tenders

---

## Future Enhancements

- Fine-tuning on tender dataset
- Table extraction (structured data)
- Multi-language support
- Automatic prompt optimization
- Streaming extraction (real-time)
- Similarity deduplication
