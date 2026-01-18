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
