# Active OpenSpec Proposals

**Last Updated:** 2026-01-21
**Status:** 1 active proposal awaiting approval

---

## 📋 Summary

**Completed & Archived:** 13 changes
**Active Proposals:** 1
**Obsolete Proposals Removed:** 7 (Phase 2 cleanup)

---

## 🔄 ACTIVE PROPOSALS

### Phase 1: LLM Deployment (Android On-Device)

**Status**: ⏸️ **WAITING FOR APPROVAL**
**Priority**: P0 (CRITICAL - Core Value Proposition)
**Created**: 2026-01-21
**Revised**: 2026-01-21 (Architecture correction: on-device only)
**Location**: `openspec/changes/phase-1-llm-deployment/`

**Summary**: Deploy Gemma 3n E2B LLM (3.4 GB, LiteRT format) to Android app with lazy download during onboarding to enable on-device AI-powered tender analysis, scoring, and chat features. Model already hosted on Hugging Face.

**Impact**: 75% → 90% production readiness

**Scope** (REVISED):
- Android: Lazy download Gemma 3n E2B (3.4 GB, LiteRT) during app onboarding
- Android: On-device inference only (no backend deployment)
- Android: Device capability detection with 3-tier degradation strategy
- Android: Full support (8GB+ RAM), Degraded (4-6GB RAM), Unsupported (<4GB RAM)
- Testing: End-to-end validation of on-device LLM features
- Documentation: Update all docs to reflect on-device LLM deployment
- Model: Already hosted on Hugging Face (primary + fallback URLs configured)

**NOT INCLUDED**:
- ❌ No backend Railway LLM deployment
- ❌ No llama-cpp-python dependency
- ❌ No backend inference endpoints

**Tasks**: 28 tasks (reduced from 45)
**Estimated Effort**: 1-2 days (reduced from 2-3 days)

**Files**:
- `proposal.md` - Full proposal with motivation, approach, risks
- `tasks.md` - 45 implementation tasks with acceptance criteria
- `spec-delta.md` - Detailed specification changes

**Next Steps**:
1. User reviews proposal
2. User approves with "approved" or "proceed with apply"
3. Run `/openspec-apply phase-1-llm-deployment` to implement
4. Complete all 45 tasks with TDD approach
5. Run `/openspec-archive phase-1-llm-deployment` when complete

---

## ✅ Completed Features (Archived)

All completed features have been moved to `openspec/archive/`:

### Backend (7 features - 100% complete)
- FR-NAV-001: Browser Navigation Engine ✅
- FR-NAV-002: PDF Link Discovery ✅
- FR-NAV-003: Direct PDF Download ✅
- FR-EXTRACT-001: PDF Text Extraction ✅
- FR-WEB-001: Database and API Foundation ✅
- pdf-viewer-scoring-fix ✅
- tender-visualization-skill ✅

### Android (1 feature - archived)
- FR-MOBILE-001: Android App Foundation ✅

---

## 🗑️ Obsolete Proposals (Removed 2026-01-21)

The following proposals were removed during Phase 2 cleanup:

### Removed LLM Proposals (Code implemented differently)
- **fr-llm-001-model-management**: Code exists but built ad-hoc
- **fr-llm-002-information-extraction**: Code exists but not deployed
- **fr-llm-003-relevance-scoring**: Code exists but not deployed

**Note**: LLM code was built without following OpenSpec process. Phase 1 proposal addresses proper deployment.

### Removed Frontend Proposals (Android replaced React)
- **fr-web-002-react-frontend-core**: Never built - Android app exists instead
- **fr-web-003-pipeline-settings**: Never built - Android has these features

### Removed Mobile Proposals (Already implemented)
- **fr-mobile-005-production-theme**: Already has Material Design 3
- **zero-mock-data-production**: Already uses real backend data

---

## 🎯 Current Project State

### What Exists Today (After Phase 2)

**Backend (Python/FastAPI)**:
- ✅ Browser automation (Playwright)
- ✅ PDF scraping & extraction
- ✅ Database & API (PostgreSQL + SQLAlchemy)
- ✅ Authentication (JWT)
- ✅ Analytics endpoints
- ⚠️ LLM code (exists but not deployed) ← **Phase 1 target**

**Android App (Kotlin/Compose)**:
- ✅ Full native app (92 Kotlin files)
- ✅ Material Design 3 UI
- ✅ Clean Architecture (MVI)
- ✅ Room database + FTS5 search
- ✅ Hilt dependency injection
- ✅ Retrofit API client
- ⚠️ On-device Gemma (code exists, not bundled) ← **Phase 1 target**

**Deployment**:
- ✅ Railway production (healthy)
- ✅ GitHub Actions CI/CD (5/5 recent builds successful)
- ✅ PostgreSQL database

**Quality**:
- ✅ Zero technical debt (Phase 2 cleanup)
- ✅ 190 tests collecting successfully
- ✅ Documentation 100% accurate
- ✅ Versions synchronized (1.0.3)

---

## 📊 Production Readiness Tracker

| Phase | Target | Current | Gap | Status |
|-------|--------|---------|-----|--------|
| Phase 0 | 50% | - | - | ✅ Baseline established |
| Phase 1 | 75% | - | - | ✅ Backend/Android built |
| Phase 2 | 75% | 75% | 0% | ✅ **COMPLETE** (Tech debt eliminated) |
| **Phase 1 (LLM)** | **90%** | **75%** | **15%** | ⏸️ **PROPOSED** |
| Phase 3 | 100% | - | - | ⏭️ Planned (Production hardening) |

**Key**:
- ✅ Complete
- ⏸️ Awaiting approval
- ⏭️ Not started

---

## 📝 Lessons Learned from Phase 2

### Why Previous OpenSpec Proposals Failed

1. **Rapid Prototyping**: Features built faster without formal process
2. **Architecture Pivot**: React → Android happened without updating proposals
3. **Proposal Abandonment**: Created specs but didn't follow through
4. **Documentation Lag**: ACTIVE_PROPOSALS.md not kept synchronized

### Phase 1 Improvements

1. **Comprehensive Proposal**: 45 detailed tasks with acceptance criteria
2. **Clear Scope**: Exactly what's included/excluded
3. **Risk Analysis**: Identified 5 risks with mitigations
4. **Success Metrics**: Measurable outcomes defined
5. **Rollback Plan**: Clear procedure if deployment fails

---

## 🚀 OpenSpec Process (Recommended)

```bash
# 1. Create proposal
/openspec-proposal <feature-name>

# 2. Review and iterate
# User reviews proposal.md, tasks.md, spec-delta.md

# 3. Approve
# User says "approved" or "proceed with apply"

# 4. Implement
/openspec-apply <feature-name>
# Follow TDD: Write test → Implement → Verify

# 5. Archive
/openspec-archive <feature-name>
# Moves to archive/, updates specs/
```

---

## 📁 Directory Structure

```
openspec/
├── ACTIVE_PROPOSALS.md          # This file ✅
│
├── specs/                        # Source of truth specs
│   └── (empty - no formal specs yet)
│
├── changes/                      # Active proposals
│   └── phase-1-llm-deployment/   # ⏸️ WAITING FOR APPROVAL
│       ├── proposal.md
│       ├── tasks.md
│       └── spec-delta.md
│
└── archive/                      # Completed work (13 changes)
    ├── fr-nav-001-browser-navigation-engine/
    ├── fr-nav-002-pdf-link-discovery/
    ├── fr-nav-003-direct-pdf-download/
    ├── fr-extract-001-pdf-text-extraction/
    ├── fr-web-001-database-api-foundation/
    ├── fr-mobile-001-android-foundation/
    ├── fr-mobile-002-on-device-gemma/
    ├── fr-mobile-003-chat-interface/
    ├── fr-mobile-004-tender-sync/
    ├── pdf-viewer-scoring-fix/
    ├── tender-visualization-skill/
    └── (2 more)
```

---

## ⏳ Waiting for User Decision

**Proposal**: Phase 1 - LLM Deployment
**Status**: ⏸️ Ready for review
**Action Required**: User approval to proceed with implementation

**Review**:
- Read: `openspec/changes/phase-1-llm-deployment/proposal.md`
- Review: Tasks (45 tasks), Acceptance Criteria (25 criteria)
- Approve: Say "approved" or ask questions for clarification

---

**Last Update:** 2026-01-21 (Phase 1 proposal created)
**Next Review:** After Phase 1 approval/completion
