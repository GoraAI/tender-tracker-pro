# Active OpenSpec Proposals

**Last Updated:** 2026-01-13

---

## Backend Complete

### FR-NAV-001: Browser Navigation Engine
- **Status:** COMPLETE
- **Implemented:** 2026-01-12

### FR-NAV-002: PDF Link Discovery
- **Status:** COMPLETE
- **Implemented:** 2026-01-12

### FR-NAV-003: Direct PDF Download
- **Status:** COMPLETE
- **Implemented:** 2026-01-12

### FR-EXTRACT-001: PDF Text Extraction
- **Status:** COMPLETE
- **Implemented:** 2026-01-12

### FR-LLM-001: Gemma 3n Model Management
- **Status:** COMPLETE
- **Implemented:** 2026-01-12

### FR-LLM-002: Tender Information Extraction
- **Status:** COMPLETE
- **Implemented:** 2026-01-12

### FR-LLM-003: Tender Relevance Scoring
- **Status:** COMPLETE
- **Implemented:** 2026-01-12

**Backend P0 Features:** 7/7 Complete (100%)

---

## Web Frontend Complete

### FR-WEB-001: Database and API Foundation
- **Status:** COMPLETE
- **Priority:** P0 (Critical)
- **Implemented:** 2026-01-12
- **Summary:** PostgreSQL database with SQLAlchemy ORM, FastAPI REST API with JWT authentication

### FR-WEB-002: React Frontend Core
- **Status:** COMPLETE
- **Priority:** P0 (Critical)
- **Implemented:** 2026-01-12
- **Summary:** React + Material UI + Glassmorphism theme, auth pages, dashboard, tender list

### FR-WEB-003: Pipeline and Settings
- **Status:** COMPLETE
- **Priority:** P1 (High)
- **Implemented:** 2026-01-12
- **Summary:** Pipeline control UI, WebSocket job streaming, criteria editor, source manager

**Web Frontend P0/P1 Features:** 3/3 Complete (100%)

---

## Mobile App - PROPOSED (Awaiting Approval)

### FR-MOBILE-001: Android App Foundation
- **Status:** PROPOSED
- **Priority:** P0 (Critical)
- **Created:** 2026-01-13
- **Location:** `openspec/changes/fr-mobile-001-android-foundation/`
- **Summary:** Android project with Kotlin, Jetpack Compose, Clean Architecture, MVI, Room database, Hilt DI
- **Tasks:** 36 tasks

### FR-MOBILE-002: On-Device Gemma Integration
- **Status:** PROPOSED
- **Priority:** P0 (Critical)
- **Created:** 2026-01-13
- **Location:** `openspec/changes/fr-mobile-002-on-device-gemma/`
- **Dependencies:** FR-MOBILE-001
- **Summary:** MediaPipe LLM integration for Gemma 3n (E2B 2B params, E4B 4B params) on-device inference
- **Tasks:** 30 tasks

### FR-MOBILE-003: Conversational Chat Interface
- **Status:** PROPOSED
- **Priority:** P0 (Critical)
- **Created:** 2026-01-13
- **Location:** `openspec/changes/fr-mobile-003-chat-interface/`
- **Dependencies:** FR-MOBILE-001, FR-MOBILE-002
- **Summary:** Natural language chat with AI for tender queries, summaries, and score explanations
- **Tasks:** 26 tasks

### FR-MOBILE-004: Tender Sync and Offline Database
- **Status:** PROPOSED
- **Priority:** P0 (Critical)
- **Created:** 2026-01-13
- **Location:** `openspec/changes/fr-mobile-004-tender-sync/`
- **Dependencies:** FR-MOBILE-001
- **Summary:** Backend sync, offline-first database, WorkManager background sync, FCM push notifications
- **Tasks:** 36 tasks

**Mobile App Proposals:** 4 proposals, 128 total tasks

---

## Overall Progress

| Track | Features | Complete | Proposed | Progress |
|-------|----------|----------|----------|----------|
| Backend | 7 | 7 | 0 | 100% |
| Web Frontend | 3 | 3 | 0 | 100% |
| Mobile App | 4 | 0 | 4 | 0% |
| **Total** | **14** | **10** | **4** | **71%** |

---

## Project Status

### Completed Platforms

#### Web Application (Production Ready)
- Python 3.12 with FastAPI
- PostgreSQL 15 with SQLAlchemy 2.0 async
- JWT authentication with bcrypt
- React 18 with TypeScript
- Material UI v7 with Glassmorphism design
- React Query + Zustand
- WebSocket for real-time updates

**Features:**
- User authentication (login/register/profile)
- Dashboard with stats and recent tenders
- Tender list with filters and pagination
- Tender detail with score breakdown
- Pipeline control (discover, download, extract, score)
- Scoring criteria management
- Source management

### Proposed Platforms

#### Android Mobile App
**Architecture:**
- Kotlin + Jetpack Compose
- Clean Architecture (Presentation, Domain, Data)
- MVI state management
- Room + SQLite FTS5 for offline database
- Hilt dependency injection
- MediaPipe for on-device Gemma LLM
- WorkManager for background sync
- Firebase Cloud Messaging for push

**Features (Proposed):**
- On-device AI with Gemma 3n (E2B/E4B)
- Conversational chat interface
- Offline-first tender database
- Background sync with server
- Push notifications for new tenders
- Natural language search
- AI-powered summaries and Q&A

---

## Next Steps

1. **Review Mobile Proposals**
   - FR-MOBILE-001: Android Foundation
   - FR-MOBILE-002: On-Device Gemma
   - FR-MOBILE-003: Chat Interface
   - FR-MOBILE-004: Tender Sync

2. **Approve or Iterate**
   - Say "approved" to start implementation
   - Or provide feedback for iteration

3. **Implementation Order** (if approved)
   1. FR-MOBILE-001 (Foundation) - 36 tasks
   2. FR-MOBILE-004 (Sync) - 36 tasks (parallel with Gemma)
   3. FR-MOBILE-002 (Gemma) - 30 tasks
   4. FR-MOBILE-003 (Chat) - 26 tasks

---

**Tracking:** This file is updated as proposals are created, approved, implemented, and archived.
