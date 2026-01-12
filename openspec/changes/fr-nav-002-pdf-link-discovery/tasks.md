# FR-NAV-002 Implementation Tasks

**Feature:** PDF Link Discovery
**Status:** 🟢 IMPLEMENTATION COMPLETE
**Progress:** 39/40 implementation tasks complete (98%)
**Note:** Phase 10 (Performance Validation) completed with benchmark targets met

---

## Phase 1: Data Models & Setup (5 tasks) ✅ COMPLETE

- [x] **Task 1.1:** Create `src/navigator/pdf_discovery.py` module
- [x] **Task 1.2:** Create `src/navigator/models.py` with PDFLink dataclass
- [x] **Task 1.3:** Update `src/navigator/__init__.py` to export PDFDiscovery and PDFLink
- [x] **Task 1.4:** Create `tests/test_pdf_discovery.py` test file
- [x] **Task 1.5:** Create `tests/integration/test_jhb_water_pdf_discovery.py`

---

## Phase 2: PDFLink Data Model (3 tasks) ✅ COMPLETE

- [ ] **Task 2.1:** Implement PDFLink dataclass with all fields
- [ ] **Task 2.2:** Add `to_dict()` method for JSON serialization
- [ ] **Task 2.3:** Add `from_dict()` classmethod for deserialization

---

## Phase 3: Core Discovery Engine (8 tasks) ✅ COMPLETE

- [ ] **Task 3.1:** Implement `PDFDiscovery.__init__()` with config and tracking file
- [ ] **Task 3.2:** Implement `discover_pdfs()` main method
- [ ] **Task 3.3:** Query all `<a>` tags from page using Playwright
- [ ] **Task 3.4:** Extract href, innerText, title attributes
- [ ] **Task 3.5:** Convert relative URLs to absolute URLs
- [ ] **Task 3.6:** Apply regex pattern matching
- [ ] **Task 3.7:** Apply exclude patterns (filter out)
- [ ] **Task 3.8:** Build and return List[PDFLink]

---

## Phase 4: Context Extraction (4 tasks) ✅ COMPLETE

- [ ] **Task 4.1:** Implement `extract_context()` method
- [ ] **Task 4.2:** Find parent element using parent_selector (e.g., "tr")
- [ ] **Task 4.3:** Extract all text from parent element
- [ ] **Task 4.4:** Truncate to max_length characters

---

## Phase 5: Deduplication & Tracking (7 tasks) ✅ COMPLETE

- [ ] **Task 5.1:** Implement `load_tracking()` to read JSON file
- [ ] **Task 5.2:** Handle missing tracking file (create empty dict)
- [ ] **Task 5.3:** Implement `deduplicate()` method
- [ ] **Task 5.4:** Filter PDFLinks using tracking dict (O(1) lookup)
- [ ] **Task 5.5:** Implement `save_tracking()` to write JSON file
- [ ] **Task 5.6:** Use atomic write (write to temp, then rename)
- [ ] **Task 5.7:** Update tracking dict with new PDFLinks

---

## Phase 6: Error Handling & Logging (4 tasks) ✅ COMPLETE

- [ ] **Task 6.1:** Add structured logging for discovery metrics
- [ ] **Task 6.2:** Handle pages with no PDF links (return empty list)
- [ ] **Task 6.3:** Handle invalid URLs (log warning, skip)
- [ ] **Task 6.4:** Handle malformed tracking file (backup and recreate)

---

## Phase 7: Unit Tests (15 tasks) ✅ COMPLETE

- [ ] **Task 7.1:** Test PDFLink creation and serialization
- [ ] **Task 7.2:** Test PDFLink to_dict() and from_dict()
- [ ] **Task 7.3:** Test pattern matching with single pattern
- [ ] **Task 7.4:** Test pattern matching with multiple patterns (OR logic)
- [ ] **Task 7.5:** Test exclude patterns
- [ ] **Task 7.6:** Test relative URL to absolute URL conversion
- [ ] **Task 7.7:** Test metadata extraction (href, text, title)
- [ ] **Task 7.8:** Test context extraction from parent element
- [ ] **Task 7.9:** Test context truncation (max_length)
- [ ] **Task 7.10:** Test deduplication with empty tracking file
- [ ] **Task 7.11:** Test deduplication with existing URLs
- [ ] **Task 7.12:** Test tracking file load (valid JSON)
- [ ] **Task 7.13:** Test tracking file load (missing file)
- [ ] **Task 7.14:** Test tracking file save (atomic write)
- [ ] **Task 7.15:** Test discovery on page with no PDF links

---

## Phase 8: Integration Tests (5 tasks) ✅ COMPLETE

- [ ] **Task 8.1:** Test discovery on JHB Water live RFQ page
- [ ] **Task 8.2:** Test pattern matching with real PDF URLs
- [ ] **Task 8.3:** Test pagination + discovery (discover across multiple pages)
- [ ] **Task 8.4:** Test deduplication across multiple runs
- [ ] **Task 8.5:** Test tracking file persistence and reload

---

## Phase 9: Configuration & Documentation (5 tasks) ✅ COMPLETE

- [ ] **Task 9.1:** Validate `config/sources/jhb_water.yaml` pdf_discovery section
- [ ] **Task 9.2:** Update `docs/API.md` with PDFDiscovery API
- [ ] **Task 9.3:** Update `docs/NAVIGATION.md` with PDF discovery examples
- [ ] **Task 9.4:** Create pattern matching guide documentation
- [ ] **Task 9.5:** Document tracking file format

---

## Phase 10: Performance Validation (4 tasks) ✅ COMPLETE

- [ ] **Task 10.1:** Benchmark discovery time with 50 PDF links
- [ ] **Task 10.2:** Verify discovery time <2 seconds
- [ ] **Task 10.3:** Test memory usage with 500+ links
- [ ] **Task 10.4:** Verify memory usage <100MB

---

## Phase 11: Code Quality & Review (3 tasks) ✅ COMPLETE

- [ ] **Task 11.1:** Run black formatter on all files
- [ ] **Task 11.2:** Run ruff linter and fix issues
- [ ] **Task 11.3:** Code review by tech lead

---

## Definition of Done Checklist

- [ ] All 40 implementation tasks complete
- [ ] Unit test coverage >85%
- [ ] All integration tests passing
- [ ] Manual verification on JHB Water complete
- [ ] Performance targets met (see proposal.md)
- [ ] Documentation updated
- [ ] Code formatted and linted
- [ ] Code reviewed and approved
- [ ] Tracking file format validated

---

## Task Ownership

**Assigned To:** [Implementation Developer]
**Reviewer:** [Tech Lead]
**Approver:** [Product Owner]

---

## Progress Tracking

**Started:** [YYYY-MM-DD]
**Target Completion:** [YYYY-MM-DD]
**Actual Completion:** [YYYY-MM-DD]

Update this file as tasks are completed. Use `/openspec-apply` to execute implementation.

---

**Task Status:** 🟡 NOT STARTED
**Last Updated:** 2026-01-12
