# Tasks: FR-WEB-001 Database and API Foundation

## Phase 1: Infrastructure Setup

- [x] **Task 1.1**: Create docker-compose.yml with PostgreSQL 15
  - Acceptance: `docker-compose up db` starts PostgreSQL on port 5432
  - Files: docker-compose.yml

- [x] **Task 1.2**: Add API dependencies to requirements.txt
  - Acceptance: `pip install -r requirements.txt` installs all packages
  - Files: requirements.txt

- [x] **Task 1.3**: Create API config module with environment settings
  - Acceptance: Config loads from .env file, has sensible defaults
  - Files: src/api/config.py, .env.example

## Phase 2: Database Layer

- [x] **Task 2.1**: Create database connection module with async support
  - Acceptance: `get_db()` yields async session, engine connects to PostgreSQL
  - Files: src/api/database.py

- [x] **Task 2.2**: Create User SQLAlchemy model
  - Acceptance: Model has all fields (id, email, password_hash, full_name, company_name, is_active, is_admin, timestamps)
  - Files: src/api/models/user.py

- [x] **Task 2.3**: Create UserCriteria SQLAlchemy model
  - Acceptance: Model has JSONB fields, foreign key to users, unique constraint on (user_id, name)
  - Files: src/api/models/criteria.py

- [x] **Task 2.4**: Create Source SQLAlchemy model
  - Acceptance: Model has all fields, string primary key for source_id
  - Files: src/api/models/source.py

- [x] **Task 2.5**: Create Tender SQLAlchemy model
  - Acceptance: Model has all fields, foreign key to sources, unique constraint on (source_id, reference)
  - Files: src/api/models/tender.py

- [x] **Task 2.6**: Create Score SQLAlchemy model
  - Acceptance: Model has foreign keys to tenders, users, criteria; unique constraint on (tender_id, user_id, criteria_id)
  - Files: src/api/models/score.py

- [x] **Task 2.7**: Create Job and JobLog SQLAlchemy models
  - Acceptance: Job has status enum, JobLog has cascade delete from Job
  - Files: src/api/models/job.py

- [x] **Task 2.8**: Create models __init__.py with Base export
  - Acceptance: All models importable from src.api.models
  - Files: src/api/models/__init__.py

- [x] **Task 2.9**: Initialize Alembic and create initial migration
  - Acceptance: `alembic upgrade head` creates all 7 tables with indexes
  - Files: alembic.ini, alembic/env.py, alembic/versions/001_initial_schema.py

## Phase 3: Pydantic Schemas

- [x] **Task 3.1**: Create auth schemas (UserCreate, UserLogin, Token, TokenData)
  - Acceptance: Schemas validate email format, password requirements
  - Files: src/api/schemas/auth.py

- [x] **Task 3.2**: Create user schemas (UserResponse, UserUpdate)
  - Acceptance: Response schema excludes password_hash
  - Files: src/api/schemas/user.py

- [x] **Task 3.3**: Create tender schemas (TenderResponse, TenderList, TenderFilters)
  - Acceptance: List schema includes pagination, filters include search/score/deadline
  - Files: src/api/schemas/tender.py

- [x] **Task 3.4**: Create score schemas (ScoreResponse, ScoreCreate)
  - Acceptance: Response includes relevance_category property
  - Files: src/api/schemas/score.py

- [x] **Task 3.5**: Create dashboard schemas (DashboardStats, RecentTender)
  - Acceptance: Stats schema has total_tenders, high_priority_count, avg_score
  - Files: src/api/schemas/dashboard.py

- [x] **Task 3.6**: Create schemas __init__.py
  - Acceptance: All schemas importable from src.api.schemas
  - Files: src/api/schemas/__init__.py

## Phase 4: Authentication

- [x] **Task 4.1**: Create auth service with password hashing
  - Acceptance: hash_password() and verify_password() work correctly
  - Files: src/api/services/auth_service.py

- [x] **Task 4.2**: Add JWT token creation and verification
  - Acceptance: create_access_token() returns valid JWT, decode_token() verifies
  - Files: src/api/services/auth_service.py (extend)

- [x] **Task 4.3**: Create auth dependencies (get_current_user, get_db)
  - Acceptance: Protected routes return 401 without valid token
  - Files: src/api/dependencies.py

- [x] **Task 4.4**: Create auth router with register endpoint
  - Acceptance: POST /api/v1/auth/register creates user, returns token
  - Files: src/api/routers/auth.py

- [x] **Task 4.5**: Add login endpoint to auth router
  - Acceptance: POST /api/v1/auth/login returns JWT for valid credentials
  - Files: src/api/routers/auth.py (extend)

- [x] **Task 4.6**: Add refresh and me endpoints
  - Acceptance: GET /api/v1/auth/me returns current user info
  - Files: src/api/routers/auth.py (extend)

## Phase 5: Tender Endpoints

- [x] **Task 5.1**: Create tender service with list/detail methods
  - Acceptance: Service handles pagination, filtering, search
  - Files: src/api/services/tender_service.py

- [x] **Task 5.2**: Create tenders router with list endpoint
  - Acceptance: GET /api/v1/tenders returns paginated list with filters
  - Files: src/api/routers/tenders.py

- [x] **Task 5.3**: Add tender detail endpoint
  - Acceptance: GET /api/v1/tenders/{id} returns tender with score
  - Files: src/api/routers/tenders.py (extend)

## Phase 6: Dashboard

- [x] **Task 6.1**: Create dashboard router with stats endpoint
  - Acceptance: GET /api/v1/dashboard/stats returns aggregated metrics
  - Files: src/api/routers/dashboard.py

- [x] **Task 6.2**: Add recent high-priority tenders endpoint
  - Acceptance: GET /api/v1/dashboard/recent returns top 5 by score
  - Files: src/api/routers/dashboard.py (extend)

## Phase 7: FastAPI Application

- [x] **Task 7.1**: Create FastAPI main application
  - Acceptance: App starts with uvicorn, has /docs endpoint
  - Files: src/api/main.py

- [x] **Task 7.2**: Configure CORS for React dev server
  - Acceptance: localhost:3000 can make requests without CORS errors
  - Files: src/api/main.py (extend)

- [x] **Task 7.3**: Register all routers with /api/v1 prefix
  - Acceptance: All endpoints accessible under /api/v1/*
  - Files: src/api/main.py (extend)

- [x] **Task 7.4**: Add health check endpoint
  - Acceptance: GET /health returns {"status": "healthy"}
  - Files: src/api/main.py (extend)

## Testing Tasks

- [x] **Test 1**: Unit tests for auth service (password hashing, JWT)
  - Files: tests/api/test_auth_service.py
  - Status: 8/8 tests passing

- [x] **Test 2**: Unit tests for tender service (list, filter, paginate)
  - Files: tests/api/test_tender_service.py (created)

- [x] **Test 3**: Integration tests for auth endpoints
  - Files: tests/api/test_auth_endpoints.py

- [x] **Test 4**: Integration tests for tender endpoints
  - Files: tests/api/test_tender_endpoints.py

- [x] **Test 5**: Integration tests for dashboard endpoints
  - Files: tests/api/test_dashboard_endpoints.py

## Documentation Tasks

- [x] **Doc 1**: Create .env.example with all required variables
  - Files: .env.example

- [x] **Doc 2**: Update README with API setup instructions
  - Files: README.md
  - Status: Completed (added comprehensive API setup section with database, migrations, seeding, testing)

## Progress Tracking

| Phase | Tasks | Completed | Progress |
|-------|-------|-----------|----------|
| 1. Infrastructure | 3 | 3 | 100% |
| 2. Database | 9 | 9 | 100% |
| 3. Schemas | 6 | 6 | 100% |
| 4. Auth | 6 | 6 | 100% |
| 5. Tenders | 3 | 3 | 100% |
| 6. Dashboard | 2 | 2 | 100% |
| 7. Application | 4 | 4 | 100% |
| Testing | 5 | 5 | 100% |
| Documentation | 2 | 2 | 100% |
| **Total** | **40** | **40** | **100%** |
