# Spec Delta: FR-WEB-001 Database and API Foundation

## ADDED

### api-database-schema.md

```markdown
# Database Schema Specification

## Overview
PostgreSQL 15 database with 7 core tables for tender tracking.

## Tables

### users
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password_hash | VARCHAR(255) | NOT NULL |
| full_name | VARCHAR(255) | |
| company_name | VARCHAR(255) | |
| is_active | BOOLEAN | DEFAULT true |
| is_admin | BOOLEAN | DEFAULT false |
| created_at | TIMESTAMP | DEFAULT NOW() |
| updated_at | TIMESTAMP | DEFAULT NOW() |

### user_criteria
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PRIMARY KEY |
| user_id | UUID | FK users(id) ON DELETE CASCADE |
| name | VARCHAR(100) | NOT NULL |
| capabilities | JSONB | DEFAULT '[]' |
| preferred_sectors | JSONB | DEFAULT '[]' |
| preferred_locations | JSONB | DEFAULT '[]' |
| value_range | JSONB | DEFAULT '{"min": 0, "max": null}' |
| keywords | JSONB | DEFAULT '[]' |
| weights | JSONB | DEFAULT '{}' |
| is_default | BOOLEAN | DEFAULT false |
| created_at | TIMESTAMP | DEFAULT NOW() |
| updated_at | TIMESTAMP | DEFAULT NOW() |

**Unique Constraint:** (user_id, name)

### sources
| Column | Type | Constraints |
|--------|------|-------------|
| id | VARCHAR(50) | PRIMARY KEY |
| name | VARCHAR(255) | NOT NULL |
| url | VARCHAR(500) | NOT NULL |
| config_path | VARCHAR(500) | |
| is_active | BOOLEAN | DEFAULT true |
| last_scraped_at | TIMESTAMP | |
| created_at | TIMESTAMP | DEFAULT NOW() |

### tenders
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PRIMARY KEY |
| source_id | VARCHAR(50) | FK sources(id) |
| pdf_url | VARCHAR(1000) | NOT NULL |
| pdf_path | VARCHAR(500) | |
| title | VARCHAR(500) | |
| reference | VARCHAR(255) | |
| deadline | TIMESTAMP | |
| estimated_value | DECIMAL(15,2) | |
| requirements_summary | TEXT | |
| contact_name | VARCHAR(255) | |
| contact_email | VARCHAR(255) | |
| extraction_confidence | FLOAT | |
| raw_text | TEXT | |
| discovered_at | TIMESTAMP | DEFAULT NOW() |
| extracted_at | TIMESTAMP | |

**Unique Constraint:** (source_id, reference)
**Indexes:** source_id, deadline

### scores
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PRIMARY KEY |
| tender_id | UUID | FK tenders(id) ON DELETE CASCADE |
| user_id | UUID | FK users(id) ON DELETE CASCADE |
| criteria_id | UUID | FK user_criteria(id) |
| score | INTEGER | CHECK (0-100) |
| explanation | TEXT | |
| relevance_category | VARCHAR(20) | |
| scored_at | TIMESTAMP | DEFAULT NOW() |

**Unique Constraint:** (tender_id, user_id, criteria_id)
**Indexes:** user_id, tender_id

### jobs
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PRIMARY KEY |
| user_id | UUID | FK users(id) |
| job_type | VARCHAR(50) | NOT NULL |
| source_id | VARCHAR(50) | FK sources(id) |
| status | VARCHAR(20) | DEFAULT 'pending' |
| progress | INTEGER | DEFAULT 0 |
| total_items | INTEGER | |
| processed_items | INTEGER | DEFAULT 0 |
| error_message | TEXT | |
| started_at | TIMESTAMP | |
| completed_at | TIMESTAMP | |
| created_at | TIMESTAMP | DEFAULT NOW() |

**Indexes:** user_id, status

### job_logs
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PRIMARY KEY |
| job_id | UUID | FK jobs(id) ON DELETE CASCADE |
| level | VARCHAR(20) | DEFAULT 'info' |
| message | TEXT | NOT NULL |
| metadata | JSONB | |
| created_at | TIMESTAMP | DEFAULT NOW() |
```

### api-endpoints.md

```markdown
# API Endpoints Specification

## Base URL
`/api/v1`

## Authentication
All endpoints except `/auth/register` and `/auth/login` require Bearer token.

## Endpoints

### Auth
| Method | Path | Description |
|--------|------|-------------|
| POST | /auth/register | Create new user account |
| POST | /auth/login | Authenticate and get token |
| POST | /auth/refresh | Refresh access token |
| GET | /auth/me | Get current user info |

### Tenders
| Method | Path | Description |
|--------|------|-------------|
| GET | /tenders | List tenders (paginated) |
| GET | /tenders/{id} | Get tender detail with score |

**Query Parameters for GET /tenders:**
- `page` (int): Page number, default 1
- `per_page` (int): Items per page, default 20, max 100
- `search` (str): Search in title/reference
- `min_score` (int): Filter by minimum score
- `source_id` (str): Filter by source
- `deadline_from` (date): Filter by deadline start
- `deadline_to` (date): Filter by deadline end
- `sort_by` (str): score, deadline, discovered_at
- `sort_order` (str): asc, desc

### Dashboard
| Method | Path | Description |
|--------|------|-------------|
| GET | /dashboard/stats | Get aggregate statistics |
| GET | /dashboard/recent | Get recent high-priority tenders |

**Response for GET /dashboard/stats:**
```json
{
  "total_tenders": 127,
  "high_priority_count": 23,
  "avg_score": 67.4,
  "tenders_this_week": 12,
  "sources_active": 3
}
```

### Health
| Method | Path | Description |
|--------|------|-------------|
| GET | /health | Health check |
```

### api-authentication.md

```markdown
# Authentication Specification

## Token Type
JWT (JSON Web Token)

## Token Payload
```json
{
  "sub": "<user_id>",
  "email": "<user_email>",
  "exp": "<expiration_timestamp>",
  "iat": "<issued_at_timestamp>"
}
```

## Token Lifetime
- Access token: 24 hours
- Refresh token: 7 days (future enhancement)

## Password Requirements
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit

## Security
- Passwords hashed with bcrypt (12 rounds)
- Tokens signed with HS256 algorithm
- Secret key from environment variable
```

## MODIFIED

### project-structure.md

```diff
tender_tracker_pro/
  src/
    navigator/
    extractor/
    llm/
    extraction/
    scoring/
+   api/
+     __init__.py
+     main.py
+     config.py
+     database.py
+     dependencies.py
+     models/
+     schemas/
+     routers/
+     services/
+ alembic/
+   env.py
+   versions/
+ docker-compose.yml
+ alembic.ini
```

## REMOVED

- None
