# Active OpenSpec Proposals

**Last Updated:** 2026-01-21
**Status:** All active proposals cleaned up

---

## 📋 Summary

**Completed & Archived:** 13 changes
**Active Proposals:** 0
**Obsolete Proposals Removed:** 7

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

The following proposals were removed because they describe work that was either:
- Implemented differently than proposed
- Made obsolete by architecture changes
- Superseded by actual implementation

### Removed LLM Proposals (Code implemented differently)
- **fr-llm-001-model-management**: Code exists in `src/llm/` but wasn't built following this proposal
- **fr-llm-002-information-extraction**: Code exists but not deployed/tested
- **fr-llm-003-relevance-scoring**: Code exists but not deployed/tested

**Note**: LLM code was built ad-hoc without following OpenSpec process. While functional code exists, it's not deployed to production yet.

### Removed Frontend Proposals (Android replaced React web)
- **fr-web-002-react-frontend-core**: Never built - Android app built instead
- **fr-web-003-pipeline-settings**: Never built - Android app has these features

**Note**: Original plan was React web frontend. Actual implementation is native Android app with Kotlin + Jetpack Compose.

### Removed Mobile Proposals (Already implemented outside OpenSpec)
- **fr-mobile-005-production-theme**: Android app already has Material Design 3 theme
- **zero-mock-data-production**: Android app already uses real backend data

---

## 🎯 Current Project State

### What Actually Exists (Outside OpenSpec)

**Backend (Python/FastAPI)**:
- ✅ Browser automation (Playwright)
- ✅ PDF scraping & extraction
- ✅ Database & API (PostgreSQL + SQLAlchemy)
- ✅ Authentication (JWT)
- ✅ Analytics endpoints
- ⚠️ LLM code (not deployed)

**Android App (Kotlin/Compose)**:
- ✅ Full native app (92 Kotlin files)
- ✅ Material Design 3 UI
- ✅ Clean Architecture (MVI)
- ✅ Room database + FTS5 search
- ✅ Hilt dependency injection
- ✅ Retrofit API client
- ⚠️ On-device Gemma (code exists, not bundled)

**Deployment**:
- ✅ Railway production (healthy)
- ✅ GitHub Actions CI/CD
- ✅ PostgreSQL database

---

## 📝 Lessons Learned

### Why OpenSpec Proposals Became Obsolete

1. **Rapid Prototyping**: Some features were built faster without formal OpenSpec process
2. **Architecture Pivot**: React web → Android native happened without updating proposals
3. **Proposal Abandonment**: LLM proposals created but implementation deviated
4. **Documentation Lag**: ACTIVE_PROPOSALS.md not kept in sync with reality

### Recommendations for Future

1. **Archive or Delete**: Don't leave stale proposals in `changes/`
2. **Update ACTIVE_PROPOSALS.md**: Keep this file synchronized with actual work
3. **Enforce OpenSpec**: Either use the process or don't create proposals
4. **Clean Up Regularly**: Monthly review of `openspec/changes/` directory

---

## 🚀 Next Steps

### If You Want to Add New Features

Use the OpenSpec process:

```bash
# 1. Create proposal
/openspec-proposal <feature-name>

# 2. Wait for approval

# 3. Implement
/openspec-apply <feature-name>

# 4. Archive when complete
/openspec-archive <feature-name>
```

### If You Want to Document Existing Work

For features already built (like the Android app):

1. **Option A**: Create retroactive OpenSpec docs in `openspec/archive/` as historical record
2. **Option B**: Document in README.md and PROJECT_STATUS.md only (simpler)

**Current approach**: Option B (already done in Phase 2)

---

## 📁 Directory Structure

```
openspec/
├── ACTIVE_PROPOSALS.md          # This file (up to date ✅)
│
├── specs/                        # Source of truth specs
│   └── (empty - no formal specs written)
│
├── changes/                      # Active proposals
│   └── (empty - all cleaned up ✅)
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

## ✨ Status: Clean Slate

**All obsolete proposals removed.**
**All completed work archived.**
**Documentation synchronized with reality.**

Ready for Phase 1 (LLM Deployment) or any new features.

---

**Last Cleanup:** 2026-01-21 (Phase 2 Quality & Documentation)
**Next Review:** After Phase 1 completion
