# FR-LLM-002: Tender Information Extraction - Implementation Tasks

**Status**: PROPOSED
**Total Tasks**: 30

---

## Phase 1: Project Setup (3 tasks)

- [ ] Create prompts/ directory
- [ ] Create prompts/tender_extraction_system.txt
- [ ] Create config/extraction/tender_extraction.yaml

## Phase 2: Data Models (3 tasks)

- [ ] Create src/extraction/models.py with TenderInfo dataclass
- [ ] Add to_dict() and from_dict() methods
- [ ] Create src/extraction/__init__.py with exports

## Phase 3: Prompt Engineering (4 tasks)

- [ ] Write system prompt in tender_extraction_system.txt
- [ ] Write user prompt template in prompts/tender_extraction_user.txt
- [ ] Test prompts manually with Gemma 3n
- [ ] Iterate based on initial results

## Phase 4: TenderExtractor Core (6 tasks)

- [ ] Create src/extraction/tender_extractor.py
- [ ] Implement __init__() with ModelManager integration
- [ ] Implement _build_prompt() method
- [ ] Implement _call_llm() method
- [ ] Implement _parse_response() method with JSON validation
- [ ] Implement _calculate_confidence() method

## Phase 5: Single Extraction (3 tasks)

- [ ] Implement extract_single() method
- [ ] Add error handling and retries
- [ ] Add structured logging

## Phase 6: Batch Extraction (3 tasks)

- [ ] Implement extract_batch() with concurrency
- [ ] Add progress tracking
- [ ] Add batch result summary

## Phase 7: Unit Tests (5 tasks)

- [ ] Create tests/test_tender_extractor.py
- [ ] Test prompt building (2 tests)
- [ ] Test JSON parsing (3 tests)
- [ ] Test confidence scoring (2 tests)
- [ ] Test extract_single() with mocked LLM (3 tests)

## Phase 8: Integration Tests (3 tasks)

- [ ] Create tests/test_tender_extraction_integration.py
- [ ] Test with 3 real tender PDFs from jhb-water
- [ ] Validate extracted fields match manual labels
- [ ] Measure and log accuracy metrics

---

**Total**: 30 tasks
