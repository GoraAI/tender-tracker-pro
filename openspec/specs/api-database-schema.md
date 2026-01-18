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
