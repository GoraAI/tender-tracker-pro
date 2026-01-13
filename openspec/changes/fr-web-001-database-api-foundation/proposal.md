# Proposal: FR-WEB-001 Database and API Foundation

## Summary

Implement PostgreSQL database layer with SQLAlchemy ORM and FastAPI REST API with JWT authentication to serve as the foundation for the Tender Tracker Pro web frontend.

## Motivation

The backend processing pipeline (navigation, PDF discovery, extraction, LLM scoring) is 100% complete, but there's no way to:
- Persist discovered tenders in a database
- Expose the functionality via HTTP API
- Authenticate users and manage their scoring criteria
- Provide data for a web frontend

This proposal creates the database and API foundation that bridges the existing backend modules with a future React frontend.

## Scope

### Included
- PostgreSQL database schema (7 tables: users, user_criteria, sources, tenders, scores, jobs, job_logs)
- SQLAlchemy ORM models with relationships
- Alembic database migrations
- FastAPI application with CORS configuration
- JWT-based authentication (register, login, refresh, me)
- Tender CRUD endpoints (list, detail, score)
- Dashboard statistics endpoint
- Docker Compose for PostgreSQL
- API tests with pytest

### NOT Included
- React frontend (FR-WEB-002)
- Pipeline control endpoints (FR-WEB-003)
- WebSocket job streaming (FR-WEB-003)
- User preferences/criteria editor (FR-WEB-003)
- Production deployment configuration

## Acceptance Criteria

- [ ] PostgreSQL runs via Docker Compose
- [ ] All 7 database tables created via Alembic migration
- [ ] User can register with email/password
- [ ] User can login and receive JWT token
- [ ] Authenticated user can list tenders with pagination
- [ ] Authenticated user can view tender detail
- [ ] Dashboard stats endpoint returns correct counts
- [ ] All API tests pass (pytest)
- [ ] API responds at http://localhost:8000/api/v1/

## Technical Approach

### Database Layer
1. Use PostgreSQL 15 in Docker container
2. SQLAlchemy 2.0 with async support (asyncpg driver)
3. Alembic for schema migrations
4. UUID primary keys for all entities
5. JSONB fields for flexible criteria storage

### API Layer
1. FastAPI with automatic OpenAPI docs
2. Pydantic v2 for request/response schemas
3. python-jose for JWT token handling
4. passlib with bcrypt for password hashing
5. Dependency injection for auth and DB sessions

### File Structure
```
src/api/
├── __init__.py
├── main.py              # FastAPI app
├── config.py            # Settings from env
├── database.py          # DB connection
├── dependencies.py      # Auth, session deps
├── models/              # SQLAlchemy models
│   ├── __init__.py
│   ├── user.py
│   ├── criteria.py
│   ├── source.py
│   ├── tender.py
│   ├── score.py
│   └── job.py
├── schemas/             # Pydantic schemas
│   ├── __init__.py
│   ├── auth.py
│   ├── user.py
│   ├── tender.py
│   ├── score.py
│   └── dashboard.py
├── routers/             # Route handlers
│   ├── __init__.py
│   ├── auth.py
│   ├── tenders.py
│   └── dashboard.py
└── services/            # Business logic
    ├── __init__.py
    ├── auth_service.py
    └── tender_service.py
```

## Dependencies

Add to requirements.txt:
```
fastapi==0.109.0
uvicorn[standard]==0.27.0
sqlalchemy==2.0.25
asyncpg==0.29.0
alembic==1.13.1
python-jose[cryptography]==3.3.0
passlib[bcrypt]==1.7.4
python-multipart==0.0.6
```

## References

- Approved plan: ~/.claude/plans/glistening-jumping-bunny.md
- Existing backend modules: src/navigator/, src/extractor/, src/llm/, src/extraction/, src/scoring/
- Related: FR-WEB-002 (React Frontend), FR-WEB-003 (Pipeline & Settings)
