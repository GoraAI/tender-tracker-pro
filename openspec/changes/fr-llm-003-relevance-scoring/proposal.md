# FR-LLM-003: Tender Relevance Scoring

**Status**: PROPOSED
**Priority**: P0
**Created**: 2026-01-12
**Dependencies**: FR-LLM-001 (Model Management), FR-LLM-002 (Information Extraction)

---

## Summary

Score tender relevance to user-defined criteria using Gemma 3n LLM to rank and filter tenders by business fit, capabilities, and interests.

---

## Motivation

Users need to quickly identify relevant tenders from hundreds of options:
- Match to business capabilities
- Match to industry/sector
- Match to value range
- Match to location
- Match to keywords/interests

Relevance scoring enables:
- Prioritized tender list (highest scores first)
- Filtering by minimum relevance threshold
- Automated alerts for high-relevance tenders
- Better resource allocation (focus on best matches)

---

## Scope

### In Scope
- User criteria definition (YAML configuration)
- Relevance scoring (0-100 scale)
- Explanation of score (why relevant/not relevant)
- Batch scoring of multiple tenders
- Configurable scoring weights
- Threshold-based filtering

### Out of Scope
- Machine learning-based scoring (use LLM)
- Historical bid success prediction
- Competitor analysis
- Real-time score updates (batch only)
- Multi-user criteria management

---

## Acceptance Criteria

- [ ] Can define user criteria in YAML config
- [ ] Scores tenders on 0-100 scale
- [ ] Provides explanation for each score
- [ ] Processes batch of tenders concurrently
- [ ] Supports configurable criteria weights
- [ ] Filters tenders by minimum score threshold
- [ ] Logs scoring quality metrics
- [ ] Returns results sorted by score (descending)
- [ ] Unit tests with sample criteria/tenders
- [ ] Integration test with real tenders

---

## Technical Approach

### Architecture

```
RelevanceScorer
├── Uses ModelManager (FR-LLM-001)
├── Uses TenderInfo (FR-LLM-002)
├── UserCriteria configuration
├── Scoring prompt templates
└── Score + explanation output
```

### Scoring Flow

```
1. Load user criteria from config
2. For each tender (TenderInfo):
   a. Build scoring prompt with criteria + tender
   b. Call Gemma 3n for score + explanation
   c. Parse score (0-100) and explanation
   d. Return ScoredTender object
3. Sort by score (descending)
4. Filter by minimum threshold
```

### User Criteria Schema

```yaml
user_criteria:
  # Business profile
  company_name: "ABC Construction (Pty) Ltd"
  industry: "Construction & Civil Engineering"
  capabilities:
    - "Road construction and maintenance"
    - "Water pipeline installation"
    - "Building construction"
    - "Project management"

  # Preferences
  preferred_sectors:
    - "Water and sanitation"
    - "Infrastructure"
    - "Municipal services"

  preferred_locations:
    - "Gauteng"
    - "Johannesburg"

  value_range:
    min: 100000   # ZAR
    max: 5000000  # ZAR

  keywords:
    - "water"
    - "pipeline"
    - "construction"
    - "maintenance"

  # Weights (0.0-1.0, must sum to 1.0)
  weights:
    capability_match: 0.4    # Most important
    sector_match: 0.2
    location_match: 0.1
    value_match: 0.1
    keyword_match: 0.2
```

### Prompt Engineering

**System Prompt**:
```
You are a tender relevance analyzer. Score how well a tender matches a company's profile and capabilities.

Scoring scale:
- 90-100: Excellent match (core capability, ideal sector/location)
- 70-89: Good match (some capability fit, reasonable sector/location)
- 50-69: Moderate match (partial fit, some misalignment)
- 30-49: Weak match (little overlap, significant misalignment)
- 0-29: Poor match (no clear fit)

Return JSON with score (0-100) and brief explanation (max 200 chars).
```

**User Prompt Template**:
```
Score this tender for relevance to our company:

COMPANY PROFILE:
- Name: {company_name}
- Industry: {industry}
- Capabilities: {capabilities}

PREFERENCES:
- Sectors: {preferred_sectors}
- Locations: {preferred_locations}
- Value range: {value_range}
- Keywords: {keywords}

TENDER:
- Title: {title}
- Reference: {reference}
- Deadline: {deadline}
- Value: {estimated_value}
- Requirements: {requirements_summary}

Return JSON:
{{
  "score": <integer 0-100>,
  "explanation": "<why relevant or not, max 200 chars>"
}}
```

### Output Schema

```python
@dataclass
class ScoredTender:
    """Tender with relevance score."""

    tender_info: TenderInfo
    score: int  # 0-100
    explanation: str
    scoring_time_ms: int
```

---

## Implementation Plan

### Phase 1: User Criteria
- Define criteria schema
- Create config/user/criteria.yaml
- Load and validate criteria

### Phase 2: RelevanceScorer
- Initialize with ModelManager + criteria
- Build scoring prompts
- Call LLM and parse response
- Single tender scoring

### Phase 3: Batch Scoring
- Concurrent scoring
- Progress tracking
- Result sorting/filtering

### Phase 4: Testing
- Unit tests with mock LLM
- Integration tests with real tenders
- Validate scoring consistency

---

## Configuration

```yaml
# config/scoring/relevance_scoring.yaml
scoring:
  # Model settings
  model: "gemma-3n"

  # Generation parameters
  max_tokens: 512  # Shorter than extraction
  temperature: 0.2  # Slightly higher for reasoning
  top_p: 0.9

  # Prompts
  system_prompt_file: "prompts/relevance_scoring_system.txt"
  user_prompt_template: "prompts/relevance_scoring_user.txt"

  # Processing
  max_concurrent: 5  # Can be higher than extraction
  timeout_seconds: 30
  retry_attempts: 2

  # Filtering
  min_score_threshold: 50  # Only show tenders >= 50
  sort_by_score: true      # Descending

  # Output
  save_scores: true
  log_score_distribution: true
```

---

## API Design

### RelevanceScorer

```python
class RelevanceScorer:
    """Score tender relevance to user criteria."""

    def __init__(
        self,
        model_manager: ModelManager,
        user_criteria: dict,
        config: dict,
    ):
        """Initialize with model, criteria, and config."""

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
        """

    async def score_batch(
        self,
        tender_infos: List[TenderInfo],
        min_score: Optional[int] = None,
    ) -> List[ScoredTender]:
        """
        Score multiple tenders concurrently.

        Args:
            tender_infos: List of extracted tenders
            min_score: Optional minimum score threshold

        Returns:
            List of ScoredTender, sorted by score (descending)
        """

    def _build_prompt(
        self,
        tender_info: TenderInfo,
    ) -> str:
        """Build scoring prompt."""

    def _parse_response(self, response: str) -> Tuple[int, str]:
        """Parse LLM response to (score, explanation)."""
```

---

## Performance Targets

- **Scoring time**: < 3 seconds per tender
- **Batch throughput**: 15+ tenders/minute (concurrent)
- **Consistency**: Scoring same tender twice should be within ±5 points

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Subjective scoring | Inconsistent results | Clear scoring rubric in prompt |
| LLM bias | Unfair scoring | Test with diverse tenders, iterate prompts |
| Score inflation | All tenders rated high | Validate distribution, adjust prompts |
| Slow inference | Poor UX | Batch processing, caching |
| Criteria too broad/narrow | All high/low scores | Provide guidance on criteria definition |

---

## Testing Strategy

1. **Unit Tests**:
   - Mock LLM responses
   - Test prompt building
   - Test score parsing
   - Test filtering/sorting

2. **Integration Tests**:
   - Real Gemma 3n model
   - 10 real tenders with manual labels
   - Validate score ranking matches human judgment

3. **Consistency Tests**:
   - Score same tender 3 times
   - Variance should be < 5 points

---

## Success Metrics

- [ ] Scoring correlation > 0.7 with human labels
- [ ] Average scoring time < 3s
- [ ] Score distribution reasonable (not all high or all low)
- [ ] All unit tests pass
- [ ] Integration tests pass

---

## Future Enhancements

- Historical bid success weighting
- Competitor analysis integration
- Learning from user feedback (marked as won/lost)
- Multi-dimensional scoring (separate scores for each criterion)
- Confidence intervals for scores
- Personalized scoring models per user
