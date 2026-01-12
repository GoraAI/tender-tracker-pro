# Active OpenSpec Proposals

**Last Updated:** 2026-01-12

---

## Currently Proposed (Awaiting Approval)

### FR-NAV-001: Browser Navigation Engine
- **Status:** 🟡 PROPOSED
- **Priority:** P0 (Critical)
- **Created:** 2026-01-12
- **Location:** `openspec/changes/fr-nav-001-browser-navigation-engine/`
- **Summary:** Playwright-based browser navigation engine with human-like behavior
- **Dependencies:** None
- **Blocks:** FR-NAV-002, FR-NAV-003
- **Action Required:** Review and approve proposal.md

---

## Upcoming P0 Proposals (To Be Created)

### FR-NAV-002: PDF Link Discovery
- **Status:** ⚪ NOT STARTED
- **Priority:** P0 (Critical)
- **Dependencies:** FR-NAV-001 (must be approved first)
- **Summary:** Discover PDF links on loaded pages using pattern matching

### FR-NAV-003: Direct PDF Download
- **Status:** ⚪ NOT STARTED
- **Priority:** P0 (Critical)
- **Dependencies:** FR-NAV-001, FR-NAV-002
- **Summary:** Download PDFs via browser with integrity verification

### FR-EXTRACT-001: PDF Text Extraction
- **Status:** ⚪ NOT STARTED
- **Priority:** P0 (Critical)
- **Dependencies:** FR-NAV-003
- **Summary:** Extract text from PDFs using pdfplumber + Tesseract OCR

### FR-LLM-001: Gemma 3n Model Management
- **Status:** ⚪ NOT STARTED
- **Priority:** P0 (Critical)
- **Dependencies:** None (parallel track)
- **Summary:** Load and manage Gemma 3n E2B/E4B models via llama.cpp

### FR-LLM-002: PDF-Based Tender Information Extraction
- **Status:** ⚪ NOT STARTED
- **Priority:** P0 (Critical)
- **Dependencies:** FR-EXTRACT-001, FR-LLM-001
- **Summary:** Extract structured tender info from PDF text using Gemma E4B

### FR-LLM-003: Relevance Scoring
- **Status:** ⚪ NOT STARTED
- **Priority:** P0 (Critical)
- **Dependencies:** FR-LLM-001, FR-LLM-002
- **Summary:** Score tender relevance 0-100 based on company profile

---

## Workflow

### Proposal Lifecycle

```
⚪ NOT STARTED → 🟡 PROPOSED → ✅ APPROVED → 🔵 IN PROGRESS → ✅ COMPLETE → 📦 ARCHIVED
```

### Commands

1. **Create Proposal:** `/openspec-proposal <feature-name>`
2. **Review:** Review `proposal.md`, `tasks.md`, `spec-delta.md`
3. **Approve:** Update proposal status to ✅ APPROVED
4. **Implement:** `/openspec-apply <change-name>`
5. **Complete:** `/openspec-archive <change-name>`

---

## P0 Feature Dependency Graph

```
Navigation Track:
FR-NAV-001 (Browser Engine)
    ↓
FR-NAV-002 (PDF Discovery)
    ↓
FR-NAV-003 (PDF Download)
    ↓
FR-EXTRACT-001 (Text Extraction)
    ↓
    ├→ FR-LLM-002 (Tender Info Extraction)

AI Track (Parallel):
FR-LLM-001 (Model Management)
    ↓
FR-LLM-002 (Tender Info Extraction)
    ↓
FR-LLM-003 (Relevance Scoring)

Integration:
FR-LLM-002 + FR-LLM-003 → End-to-End Pipeline
```

---

## Next Actions

1. **Review FR-NAV-001 proposal** (this is the active task)
2. Approve or request changes
3. Once approved, implement with `/openspec-apply fr-nav-001-browser-navigation-engine`
4. After FR-NAV-001 complete, create FR-NAV-002 proposal
5. Continue through P0 features in dependency order

---

**Tracking:** This file is updated as proposals are created, approved, implemented, and archived.
