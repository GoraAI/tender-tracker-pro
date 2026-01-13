# FR-LLM-003: Relevance Scoring - Implementation Tasks

**Status**: PROPOSED
**Total Tasks**: 25

---

## Phase 1: Project Setup (3 tasks)

- [ ] Create config/user/criteria.yaml with example criteria
- [ ] Create prompts/relevance_scoring_system.txt
- [ ] Create config/scoring/relevance_scoring.yaml

## Phase 2: Data Models (2 tasks)

- [ ] Create src/scoring/models.py with ScoredTender dataclass
- [ ] Create src/scoring/__init__.py with exports

## Phase 3: Prompt Engineering (3 tasks)

- [ ] Write system prompt in relevance_scoring_system.txt
- [ ] Write user prompt template in prompts/relevance_scoring_user.txt
- [ ] Test prompts manually with sample tenders

## Phase 4: RelevanceScorer Core (5 tasks)

- [ ] Create src/scoring/relevance_scorer.py
- [ ] Implement __init__() with ModelManager + criteria loading
- [ ] Implement _build_prompt() with criteria formatting
- [ ] Implement _call_llm() method
- [ ] Implement _parse_response() with score validation

## Phase 5: Single Scoring (2 tasks)

- [ ] Implement score_single() method
- [ ] Add error handling and retries

## Phase 6: Batch Scoring (3 tasks)

- [ ] Implement score_batch() with concurrency
- [ ] Add sorting by score (descending)
- [ ] Add filtering by min_score threshold

## Phase 7: Unit Tests (4 tasks)

- [ ] Create tests/test_relevance_scorer.py
- [ ] Test prompt building (2 tests)
- [ ] Test score parsing (3 tests)
- [ ] Test score_single() with mocked LLM (3 tests)

## Phase 8: Integration Tests (3 tasks)

- [ ] Create tests/test_relevance_scoring_integration.py
- [ ] Score 5 real tenders with defined criteria
- [ ] Validate score ranking is reasonable
- [ ] Test consistency (score same tender 3 times)

---

**Total**: 25 tasks
