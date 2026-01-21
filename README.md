# Tender Tracker Pro

**Version:** 1.0.3
**Architecture:** Backend API + Native Android App
**AI Engine:** On-Device Gemma 3n (MediaPipe)

---

## Overview

Tender Tracker Pro is an intelligent, privacy-first tender discovery and analysis platform that automatically navigates South African government procurement websites, downloads tender PDFs, and uses on-device AI to extract, score, and prioritize opportunities.

### Key Features

- **Navigation-First Architecture**: Playwright-based browser automation mimics human behavior
- **PDF as Source of Truth**: All data extracted from PDF documents
- **On-Device AI Processing**: Gemma 3n models for zero-cloud dependency
- **Smart Scoring**: Relevance scoring 0-100 based on user criteria
- **Native Android App**: Material Design 3 UI with Jetpack Compose
- **Real-time Sync**: Background tender synchronization with push notifications
- **User Management**: JWT authentication with personalized scoring
- **Offline-First**: Local database with full-text search

---

## Architecture

### Dual-Platform System

```
┌─────────────────────────────────────────────────────────────────────┐
│              ANDROID APP (Kotlin + Jetpack Compose)                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │  Login   │ │Dashboard │ │ Tenders  │ │   Chat   │ │ Profile  │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │         Gemma 3n On-Device LLM (MediaPipe)                    │  │
│  │         Room Database + FTS5 Search (Offline-First)           │  │
│  └───────────────────────────────────────────────────────────────┘  │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ REST API + WorkManager Sync
┌──────────────────────────────┼──────────────────────────────────────┐
│                    BACKEND API (FastAPI)                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │  /auth   │ │/tenders  │ │/pipeline │ │/analytics│ │/dashboard│  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
│         └────────────────────┴────────────────────┘                 │
│                    SQLAlchemy ORM (PostgreSQL)                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────┼──────────────────────────────────────┐
│                    BACKEND PROCESSING                                │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐       │
│  │ Navigator  │ │ Downloader │ │ Extractor  │ │  Analyzer  │       │
│  │(Playwright)│ │   (PDF)    │ │(pdfplumber)│ │  (Gemma)   │       │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘       │
└─────────────────────────────────────────────────────────────────────┘
```

### Component Breakdown

**Backend (Python + FastAPI)**
- REST API with JWT authentication
- PostgreSQL database with SQLAlchemy
- Playwright browser automation
- PDF extraction with pdfplumber
- Scheduled scraping with APScheduler
- Deployed on Railway

**Android App (Kotlin + Compose)**
- Native Android (minSdk 31, targetSdk 35)
- Jetpack Compose Material Design 3 UI
- Clean Architecture (Presentation/Domain/Data)
- MVI state management
- Room database with FTS5 search
- Hilt dependency injection
- MediaPipe for on-device Gemma inference
- WorkManager for background sync

---

## Quick Start

### Backend API Setup

#### Prerequisites
- Python 3.12+
- PostgreSQL 15+
- Docker (optional)

#### Development

```bash
# Clone and enter directory
cd tender_tracker_pro

# Create Python virtual environment
python3 -m venv venv
source venv/bin/activate

# Install Python dependencies
pip install -r requirements.txt

# Install Playwright browsers
playwright install chromium

# Copy and configure environment
cp .env.example .env
# Edit .env with your DATABASE_URL and JWT_SECRET_KEY

# Run database migrations
alembic upgrade head

# Seed the database with sources
python scripts/seed_sources.py

# Start backend
uvicorn src.api.main:app --reload --port 8000
```

API available at:
- **API Root**: http://localhost:8000/api/v1/
- **Interactive Docs**: http://localhost:8000/docs
- **Health Check**: http://localhost:8000/health

#### Production (Railway)

The backend is deployed on Railway at:
- **Production URL**: https://tender-tracker-pro-production.up.railway.app/api/v1/

Configuration:
- Uses Dockerfile builder
- PostgreSQL linked service
- Playwright with `--with-deps` flag
- Uvicorn with `--loop asyncio` (DNS fix)
- Healthcheck timeout: 300s

See `Dockerfile` and `railway.json` for full configuration.

---

### Android App Setup

#### Prerequisites
- Android Studio Ladybug or later
- JDK 21
- Android SDK 35
- Physical device or emulator (Android 12+)

#### Development

```bash
# Open Android Studio
# File -> Open -> /path/to/tender_tracker_pro/android

# Configure backend URL
# Edit: android/app/src/main/java/com/tendertracker/app/di/NetworkModule.kt
# For local testing:
# - Emulator: http://10.0.2.2:8000/api/v1/
# - Physical: http://127.0.0.1:8000/api/v1/ (with adb reverse)

# Build and run
# Click "Run" button in Android Studio
```

#### Production Build (GitHub Actions)

Push to repository triggers CI/CD:

```bash
git add -A
git commit -m "feat: your changes"
git push

# Monitor build
gh run list --limit 3
gh run watch

# Download APK
gh run download <run-id> -n app-debug
adb install -r app-debug.apk
```

**Important**: Never build Android locally on ARM64/aarch64 machines. Always use GitHub Actions CI.

---

## Tech Stack

### Backend
- **Language**: Python 3.12
- **Framework**: FastAPI + Uvicorn
- **Database**: PostgreSQL 15 with SQLAlchemy 2.0 (async)
- **ORM**: Alembic for migrations
- **Browser Automation**: Playwright
- **PDF Processing**: pdfplumber, PyPDF2
- **Authentication**: JWT with python-jose, bcrypt
- **Scheduling**: APScheduler
- **Deployment**: Railway (Docker)

### Android
- **Language**: Kotlin 1.9+
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: Clean Architecture (MVI pattern)
- **Database**: Room + SQLite FTS5
- **DI**: Hilt (Dagger)
- **Networking**: Retrofit + OkHttp
- **AI**: MediaPipe Tasks GenAI (Gemma 3n)
- **Background Work**: WorkManager
- **Build**: Gradle 8+ with Kotlin DSL

---

## Project Structure

```
tender_tracker_pro/
├── src/                      # Python backend
│   ├── api/                  # FastAPI endpoints
│   │   ├── main.py          # App entry
│   │   ├── config.py        # Settings with DATABASE_URL fix
│   │   ├── routers/         # API routes (auth, tenders, pipeline, etc.)
│   │   ├── models/          # SQLAlchemy models
│   │   ├── schemas/         # Pydantic schemas
│   │   └── services/        # Business logic
│   ├── navigator/           # Playwright browser automation
│   ├── extraction/          # PDF extraction (pdfplumber)
│   ├── llm/                 # LLM model management
│   ├── scoring/             # Tender relevance scoring
│   └── scrapers/            # Tender scraping logic
│
├── android/                 # Android app
│   └── app/src/main/java/com/tendertracker/app/
│       ├── di/              # Hilt DI (NetworkModule.kt has BASE_URL)
│       ├── ui/              # Compose screens
│       │   ├── auth/        # Login/Register
│       │   ├── dashboard/   # Dashboard
│       │   ├── tenders/     # Tender list/detail
│       │   └── chat/        # AI chat interface
│       ├── data/            # Repository layer
│       ├── domain/          # Business logic
│       └── gemma/           # On-device Gemma LLM
│
├── config/                  # YAML scraper configs
│   └── sources/             # Tender source configurations
│
├── tests/                   # Python tests (190 tests)
│   ├── api/                 # API endpoint tests
│   ├── integration/         # Integration tests
│   └── *.py                 # Unit tests
│
├── openspec/                # OpenSpec change management
│   ├── changes/            # Active changes
│   ├── specs/              # Merged specs
│   └── archive/            # Completed changes
│
├── Dockerfile               # Railway deployment
├── railway.json             # Railway config
├── requirements-prod.txt    # Production deps
├── requirements.txt         # Dev deps
├── .env.example             # Environment template
├── CLAUDE.md               # Claude Code knowledge base
└── LESSONS_LEARNED.md      # Development patterns
```

---

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` - Create account
- `POST /api/v1/auth/login` - Get JWT token
- `GET /api/v1/auth/me` - Current user info

### Tenders
- `GET /api/v1/tenders` - List tenders (paginated, filtered)
- `GET /api/v1/tenders/:id` - Tender detail

### Pipeline
- `POST /api/v1/pipeline/discover` - Start PDF discovery
- `POST /api/v1/pipeline/download` - Download PDFs
- `POST /api/v1/pipeline/extract` - Extract text
- `POST /api/v1/pipeline/score` - Score tenders
- `POST /api/v1/pipeline/full` - Run full pipeline
- `GET /api/v1/pipeline/jobs` - List jobs

### Dashboard
- `GET /api/v1/dashboard/stats` - Overview statistics
- `GET /api/v1/dashboard/recent` - Recent high-priority tenders

### Analytics
- `GET /api/v1/analytics/funnel` - Opportunity funnel
- `GET /api/v1/analytics/costs` - Cost tracking
- `GET /api/v1/analytics/compliance` - Compliance status

Full API documentation: http://localhost:8000/docs

---

## Environment Variables

Create a `.env` file in the project root:

```bash
# Database
DATABASE_URL=postgresql+asyncpg://user:pass@localhost:5432/tender_tracker

# JWT Authentication
JWT_SECRET_KEY=<generate-secure-random-key>
JWT_ALGORITHM=HS256
JWT_EXPIRE_MINUTES=1440

# CORS
CORS_ORIGINS=http://localhost:3000,http://localhost:5173

# API Server
API_HOST=0.0.0.0
API_PORT=8000
DEBUG=false

# Application
APP_NAME=Tender Tracker Pro
APP_VERSION=1.0.3
```

Generate JWT secret:
```bash
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

---

## Testing

### Backend Tests

```bash
# Run all tests (190 tests)
cd tender_tracker_pro
source venv/bin/activate
pytest tests/ -v

# Run with coverage
pytest --cov=src tests/

# Run specific test suite
pytest tests/api/ -v
```

### Android Tests

```bash
# Unit tests
cd android
./gradlew test

# Instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest
```

**Note**: Android builds must run via GitHub Actions on ARM64 machines.

---

## Deployment

### Backend (Railway)

1. **Connect Repository**: Link GitHub repo to Railway
2. **Configure Service**:
   - Builder: `DOCKERFILE`
   - Root Directory: `/`
   - Dockerfile Path: `./Dockerfile`
3. **Link PostgreSQL**: Add PostgreSQL service
4. **Set Environment Variables**: Add `DATABASE_URL`, `JWT_SECRET_KEY`, etc.
5. **Deploy**: Push to GitHub triggers deployment

Health check: `https://your-app.up.railway.app/health`

### Android (GitHub Actions)

APK builds automatically on push:

```bash
# Trigger build
git push origin master

# Download APK
gh run download <run-id> -n app-debug

# Install on device
adb install -r app-debug.apk
```

---

## Configured Tender Sources

| Source | URL | Status |
|--------|-----|--------|
| JHB Water | https://www.johannesburgwater.co.za/tenders | ✅ Active |
| SARS | https://www.sars.gov.za/documents/procurement | ✅ Active |

Add more sources in `config/sources/`

---

## Development Workflow

### Adding a New Feature

1. **Create OpenSpec Proposal**:
   ```bash
   /openspec-proposal <feature-name>
   ```

2. **Wait for Approval**

3. **Implement with TDD**:
   - Write failing tests (RED)
   - Implement feature (GREEN)
   - Refactor (REFACTOR)

4. **Run Verification**:
   ```bash
   /verify  # Lint + tests
   ```

5. **Ship Changes**:
   ```bash
   /ship "feat: add <feature>"
   ```

6. **Archive Spec**:
   ```bash
   /openspec-archive <feature-name>
   ```

---

## Production Readiness

**Current Status: 75% Production Ready**

✅ **Complete**:
- Backend API deployed and healthy
- Android app building via CI/CD
- Database schema complete
- Authentication working
- PDF scraping and extraction functional

⚠️ **In Progress**:
- On-device Gemma LLM integration (code complete, deployment pending)
- Test suite health (190 tests, all collecting successfully)

See `PROJECT_STATUS.md` for detailed roadmap.

---

## Key Documentation

- **CLAUDE.md**: Claude Code knowledge base with mandatory protocols
- **LESSONS_LEARNED.md**: Development patterns and best practices
- **PROJECT_STATUS.md**: Feature status and next steps
- **ARCHITECTURE_ACTUAL.md**: (To be created) Detailed architecture guide

---

## License

Proprietary - Internal Use Only

---

## Support

For issues or questions:
- GitHub Issues: https://github.com/anthropics/claude-code/issues
- API Docs: http://localhost:8000/docs

---

**Built with:** Python | FastAPI | Kotlin | Jetpack Compose | PostgreSQL | Playwright | MediaPipe
