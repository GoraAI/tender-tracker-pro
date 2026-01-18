# Tender Tracker Pro

**Version:** 1.0.0
**Architecture:** Full-Stack Web Application
**AI Engine:** Gemma 3n (Local Inference) with Keyword Fallback

---

## Overview

Tender Tracker Pro is an intelligent, privacy-first tender discovery and analysis platform that automatically navigates South African government procurement websites, downloads tender PDFs, and uses on-device AI to extract, score, and prioritize opportunities.

### Key Features

- **Navigation-First Architecture**: Playwright-based browser automation mimics human behavior
- **PDF as Source of Truth**: All data extracted from PDF documents
- **Local AI Processing**: Gemma 3n models for zero-cloud dependency (with keyword fallback)
- **Smart Scoring**: Relevance scoring 0-100 based on user criteria
- **Modern Web UI**: React + Material UI with Glassmorphism design
- **Real-time Updates**: WebSocket-based job progress streaming
- **User Management**: JWT authentication with personalized scoring

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (React + MUI)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │Dashboard │ │ Tenders  │ │ Pipeline │ │ Settings │ │   Auth   │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ REST + WebSocket
┌──────────────────────────────┼──────────────────────────────────────┐
│                         API (FastAPI)                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │  /auth   │ │/tenders  │ │/pipeline │ │ /config  │ │/dashboard│  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
│         └────────────────────┴────────────────────┘                 │
│                    SQLAlchemy ORM (PostgreSQL/SQLite)               │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────┼──────────────────────────────────────┐
│                    BACKEND PROCESSING                                │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐       │
│  │ Navigator  │ │ Downloader │ │ Extractor  │ │  Scorer    │       │
│  │(Playwright)│ │   (PDF)    │ │(pdfplumber)│ │(LLM/kw)    │       │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Quick Start

### Prerequisites

- Python 3.10+
- Node.js 18+
- Docker & Docker Compose (for production)

### Development Setup

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

# Install frontend dependencies
cd frontend && npm install && cd ..

# Copy and configure environment
cp .env.example .env
# Edit .env with your settings

# Seed the database with sources
python scripts/seed_sources.py

# Start backend (terminal 1)
uvicorn src.api.main:app --reload --port 8000

# Start frontend (terminal 2)
cd frontend && npm run dev
```

Open http://localhost:5173 in your browser.

### API Setup (Detailed)

#### 1. Database Setup

```bash
# Start PostgreSQL container
docker-compose up -d db

# Verify database is running
docker-compose ps

# Expected output:
# NAME                 SERVICE   STATUS    PORTS
# tender-tracker-db    db        running   0.0.0.0:5432->5432/tcp
```

#### 2. Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env and set these variables:
# - DATABASE_URL=postgresql+asyncpg://postgres:password@localhost:5432/tender_tracker
# - JWT_SECRET_KEY=<generate-a-secure-random-key>
# - CORS_ORIGINS=http://localhost:5173,http://localhost:3000
```

Generate a secure JWT secret:
```bash
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

#### 3. Run Database Migrations

```bash
# Install dependencies if not done already
pip install -r requirements.txt

# Run Alembic migrations to create tables
alembic upgrade head

# Verify migrations
alembic current

# Expected output: head (current revision)
```

#### 4. Seed Initial Data

```bash
# Populate sources table with configured tender sources
python scripts/seed_sources.py

# Expected output: Created/Updated 3 sources
```

#### 5. Start the API

```bash
# Development mode with auto-reload
uvicorn src.api.main:app --reload --port 8000

# Production mode
uvicorn src.api.main:app --host 0.0.0.0 --port 8000 --workers 4
```

The API will be available at:
- **API Root**: http://localhost:8000/api/v1/
- **Interactive Docs**: http://localhost:8000/docs
- **OpenAPI JSON**: http://localhost:8000/openapi.json
- **Health Check**: http://localhost:8000/health

#### 6. Test the API

```bash
# Create a test user
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123",
    "full_name": "Test User",
    "company_name": "Test Company"
  }'

# Login and get token
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123"
  }'

# Use the token from login response
export TOKEN="your-jwt-token-here"

# Test authenticated endpoint
curl http://localhost:8000/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

#### 7. Run API Tests

```bash
# Run all API tests
pytest tests/api/ -v

# Run with coverage
pytest tests/api/ --cov=src.api --cov-report=html

# View coverage report
open htmlcov/index.html
```

#### Database Management

```bash
# Create a new migration
alembic revision --autogenerate -m "description of changes"

# Apply migrations
alembic upgrade head

# Rollback one migration
alembic downgrade -1

# View migration history
alembic history

# Reset database (development only)
alembic downgrade base
alembic upgrade head
```

### Docker Deployment

```bash
# Copy environment configuration
cp .env.example .env
# Edit .env with production values (especially JWT_SECRET_KEY, POSTGRES_PASSWORD)

# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

Services:
- Frontend: http://localhost:3000
- API: http://localhost:8000
- API Docs: http://localhost:8000/docs
- Database: localhost:5432

---

## Feature Status

### Backend (100% Complete)

| Feature | Status | Description |
|---------|--------|-------------|
| FR-NAV-001 | ✅ Complete | Browser Navigation Engine |
| FR-NAV-002 | ✅ Complete | PDF Link Discovery |
| FR-NAV-003 | ✅ Complete | Direct PDF Download |
| FR-EXTRACT-001 | ✅ Complete | PDF Text Extraction |
| FR-LLM-001 | ✅ Complete | Gemma 3n Model Management |
| FR-LLM-002 | ✅ Complete | Tender Information Extraction |
| FR-LLM-003 | ✅ Complete | Relevance Scoring |

### Frontend (100% Complete)

| Feature | Status | Description |
|---------|--------|-------------|
| FR-WEB-001 | ✅ Complete | Database & API Foundation |
| FR-WEB-002 | ✅ Complete | React Frontend Core |
| FR-WEB-003 | ✅ Complete | Pipeline Control & Settings |

---

## Project Structure

```
tender_tracker_pro/
├── config/
│   └── sources/           # Source configuration YAML files
├── documents/             # Downloaded PDFs
├── frontend/              # React frontend
│   ├── src/
│   │   ├── api/           # API clients
│   │   ├── components/    # UI components
│   │   ├── hooks/         # Custom React hooks
│   │   ├── pages/         # Page components
│   │   └── theme/         # MUI theme (Glassmorphism)
│   ├── Dockerfile
│   └── package.json
├── src/
│   ├── api/               # FastAPI application
│   │   ├── models/        # SQLAlchemy models
│   │   ├── routers/       # API routes
│   │   ├── schemas/       # Pydantic schemas
│   │   ├── services/      # Business logic
│   │   └── websocket/     # WebSocket handlers
│   ├── navigator/         # Browser automation
│   ├── extraction/        # Text extraction
│   ├── scoring/           # Relevance scoring
│   └── llm/               # LLM management
├── tests/                 # Test suite
├── scripts/               # Utility scripts
├── docker-compose.yml     # Docker orchestration
├── Dockerfile.api         # Backend container
├── requirements.txt       # Python dependencies
└── .env.example           # Environment template
```

---

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` - Create account
- `POST /api/v1/auth/login` - Get JWT token
- `GET /api/v1/auth/me` - Current user info

### Tenders
- `GET /api/v1/tenders` - List tenders (paginated)
- `GET /api/v1/tenders/:id` - Tender detail

### Pipeline
- `POST /api/v1/pipeline/discover` - Start PDF discovery
- `POST /api/v1/pipeline/download` - Download PDFs
- `POST /api/v1/pipeline/extract` - Extract text
- `POST /api/v1/pipeline/score` - Score tenders
- `POST /api/v1/pipeline/full` - Run full pipeline
- `GET /api/v1/pipeline/jobs` - List jobs
- `WS /ws/pipeline/jobs/:id/stream` - Real-time job updates

### Configuration
- `GET /api/v1/config/criteria` - List scoring criteria
- `POST /api/v1/config/criteria` - Create criteria
- `GET /api/v1/config/sources` - List tender sources

### Dashboard
- `GET /api/v1/dashboard/stats` - Overview statistics
- `GET /api/v1/dashboard/recent` - Recent high-priority tenders

Full API documentation: http://localhost:8000/docs

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | Database connection string | SQLite (dev) |
| `JWT_SECRET_KEY` | JWT signing key | Change in production! |
| `CORS_ORIGINS` | Allowed CORS origins | localhost |
| `DEBUG` | Enable debug mode | true |

See `.env.example` for full list.

---

## Testing

```bash
# Run all tests
pytest tests/

# Run with coverage
pytest --cov=src tests/

# Run integration tests (requires network)
pytest tests/integration/ -v
```

---

## Tech Stack

### Backend
- Python 3.10+
- FastAPI + Uvicorn
- SQLAlchemy 2.0 (async)
- PostgreSQL / SQLite
- Playwright (browser automation)
- pdfplumber (PDF extraction)

### Frontend
- React 18 + TypeScript
- Material UI v7
- React Query (server state)
- Zustand (client state)
- Vite (build tool)

---

## License

Proprietary - Internal Use Only

---

**Built with:** Python | FastAPI | React | Material UI | Playwright | pdfplumber
