# OpenSpec Proposal: Zero Mock Data - Production Readiness

**Change ID**: `zero-mock-data-production`
**Status**: PENDING APPROVAL
**Created**: 2026-01-17
**Priority**: P0 (Critical)

---

## 1. Executive Summary

This proposal addresses a critical finding: **The app contains extensive mock data that makes it unusable for production**. Despite the TenderDetailScreen having a BidStrengthRadar component, users never see it because scores are always `null` (scoring doesn't run automatically). Similarly, compliance gauges, funnel analytics, cost tracking, and payment delays all show hardcoded fake data.

**Objective**: Eliminate ALL mock data. The app will:
- Start EMPTY - users enter their OWN real data
- Run scoring AUTOMATICALLY on every tender sync
- Use REAL National Treasury research data for payment delays (not made-up numbers)
- Cache ALL data offline
- Calculate analytics from user's ACTUAL data over time

At completion, a verification report will confirm zero hardcoded mock values remain.

---

## 2. Problem Analysis

### 2.1 Current State

| Feature | Android | Backend | Status |
|---------|---------|---------|--------|
| Tender List | Real data from API | Real DB queries | OK |
| Tender Detail | Real data | Real DB queries | OK |
| Relevance Score | Shows if present | Not auto-generated | BROKEN |
| Bid Strength Radar | Code exists | Needs score to show | HIDDEN |
| Compliance Gauges | 100% hardcoded | 100% hardcoded | MOCK |
| Funnel Analytics | 100% hardcoded | 100% hardcoded | MOCK |
| Cost Tracking | 100% hardcoded | No persistence | MOCK |
| Payment Delays | 100% hardcoded | 100% hardcoded | MOCK |
| Pipeline Gantt | 100% hardcoded | No persistence | MOCK |
| PDF Viewing | No button | URL exists | MISSING |

### 2.2 Mock Data Inventory

**Total Mock Data Points Found: 87**

| Category | Android Files | Backend Files | Mock Points |
|----------|--------------|---------------|-------------|
| Funnel Numbers | 2 | 1 | 12 |
| Compliance Items | 1 | 1 | 18 |
| Cost Categories | 1 | 1 | 15 |
| Payment Delays | 1 | 1 | 28 |
| Pipeline Status | 1 | 1 | 8 |
| User IDs | 4 | 8 | 6 |

### 2.3 Why BidStrengthRadar Doesn't Show

The TenderDetailScreen.kt contains this code:
```kotlin
tender.score?.let { score ->
    // BidStrengthRadar only renders when score is not null
    BidStrengthRadar(...)
}
```

Since scoring is not integrated into the discovery pipeline, `tender.score` is always `null`, so the radar chart never renders.

---

## 3. Solution Architecture

### 3.1 Data Flow Changes

**Current (Broken):**
```
Discovery → Download → Extract → Store (no score) → Android shows null
```

**Proposed (Working):**
```
Discovery → Download → Extract → Score → Store (with score) → Android shows real data
                                   ↓
                         Load company profile for scoring criteria
```

### 3.2 Database Schema Additions

```sql
-- Company profile table (for scoring criteria)
CREATE TABLE company_profiles (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    legal_name VARCHAR(255),
    trading_name VARCHAR(255),
    industry VARCHAR(100),
    capabilities JSONB,
    preferred_sectors JSONB,
    preferred_provinces JSONB,
    value_range_min DECIMAL,
    value_range_max DECIMAL,
    keywords JSONB,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Compliance registrations table
CREATE TABLE compliance_registrations (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    type VARCHAR(20),  -- csd, cidb, bbbee, sars, uif, coid
    name VARCHAR(255),
    short_name VARCHAR(20),
    registration_number VARCHAR(100),
    issue_date DATE,
    expiry_date DATE,
    expiry_type VARCHAR(20),  -- annual, perpetual, continuous
    renewal_url VARCHAR(500),
    documents JSONB,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Cost tracking table
CREATE TABLE cost_items (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    tender_id UUID REFERENCES tenders(id) NULL,
    category VARCHAR(50),  -- platform, compliance, preparation, submission, opportunity
    description VARCHAR(500),
    amount DECIMAL(12,2),
    date DATE,
    recurring BOOLEAN DEFAULT FALSE,
    frequency VARCHAR(20),
    created_at TIMESTAMP
);

-- Tender outcomes table
CREATE TABLE tender_outcomes (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    tender_id UUID REFERENCES tenders(id),
    outcome VARCHAR(20),  -- won, lost, withdrawn, cancelled
    reason_code VARCHAR(50),  -- price, functionality, bbbee, disqualified, other
    reason_details TEXT,
    award_date DATE,
    award_amount DECIMAL(15,2),
    payment_received_date DATE,
    days_to_payment INTEGER,
    created_at TIMESTAMP
);

-- Pipeline tenders table
CREATE TABLE pipeline_tenders (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    tender_id UUID REFERENCES tenders(id),
    status VARCHAR(20),  -- discovered, qualified, preparation, review, submission, submitted
    start_date DATE,
    phases JSONB,
    assigned_to VARCHAR(255),
    notes TEXT,
    has_conflict BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### 3.3 API Endpoint Changes

| Endpoint | Current | Proposed |
|----------|---------|----------|
| `GET /analytics/funnel` | Returns hardcoded 45/28/18/6 | Query tender_outcomes + tenders tables |
| `GET /analytics/costs` | Returns hardcoded R187,500 | Aggregate from cost_items table |
| `POST /analytics/costs` | Returns fake, doesn't save | INSERT into cost_items table |
| `GET /analytics/compliance` | Returns 6 fake items | Query compliance_registrations table |
| `PUT /analytics/compliance/{id}` | Returns fake | UPDATE compliance_registrations |
| `GET /analytics/payment-delays` | Returns hardcoded provinces | Calculate from tender_outcomes |
| `GET /analytics/pipeline` | Returns empty list | Query pipeline_tenders table |
| `POST /analytics/pipeline` | Returns fake, doesn't save | INSERT into pipeline_tenders |
| `PATCH /analytics/pipeline/{id}` | Returns fake | UPDATE pipeline_tenders |
| `POST /analytics/outcomes` | Returns fake, doesn't save | INSERT into tender_outcomes |
| `GET /analytics/outcomes` | Returns hardcoded stats | Aggregate from tender_outcomes |

### 3.4 Android Changes

**Replace hardcoded calculations with API calls:**

| ViewModel | Current Mock | Proposed Real |
|-----------|--------------|---------------|
| DashboardViewModel | Calculates fake funnel from tender count | Fetch `/analytics/funnel` |
| AnalyticsViewModel | Hardcoded costs, delays | Fetch `/analytics/costs`, `/analytics/payment-delays` |
| ComplianceViewModel | 6 hardcoded items | Fetch `/analytics/compliance` |
| PipelineViewModel | Generates fake from tender list | Fetch `/analytics/pipeline` |

**Add PDF viewing:**
| Screen | Current | Proposed |
|--------|---------|----------|
| TenderDetailScreen | No PDF button | Add "View PDF" button, "Download PDF" button |

---

## 4. Empty State Strategy (No Seed Data)

**Principle**: The app starts EMPTY. Users enter their OWN real data.

### 4.1 Company Profile
- User enters on first login via setup flow
- Required: company name, industry, capabilities
- Optional: preferred sectors, provinces, value ranges, keywords
- Used for automatic tender scoring

### 4.2 Compliance Registrations
- User adds each registration manually
- Shows "Add your first registration" empty state
- Supported types: CSD, CIDB, B-BBEE, SARS Tax Clearance, UIF, COID
- Tracks: registration number, issue date, expiry date, renewal URL
- Sends reminders as expiry approaches

### 4.3 Cost Tracking
- User logs costs as they incur them
- Shows "No costs recorded yet" empty state
- Categories: platform, compliance, preparation, submission, opportunity
- Can be linked to specific tenders or general

### 4.4 Tender Outcomes
- User records outcome when tender is awarded/rejected
- Shows "Record your first outcome to see analytics" empty state
- Tracks: won/lost/withdrawn, reason code, award amount, payment date
- Over time, builds funnel analytics from REAL data

### 4.5 Pipeline
- User adds tenders they want to track
- Shows "Add tenders to your pipeline" empty state
- User sets status, phases, deadlines
- Conflict detection based on user's actual schedule

### 4.6 Payment Delays (Reference Data)
- Uses REAL data from National Treasury reports
- NOT made-up numbers - actual government statistics
- Source: `data/reference/payment_delays_sa_treasury.json`
- Over time, supplemented by user's actual payment experiences

**Reference Data Sources**:
- [National Treasury Annual Report 2024/25](https://treasury.gov.za/legislation/pfma/Compliance%20on%20Payment%20of%20Suppliers/)
- [National Treasury Q2 2025/26 Report](https://dfa.co.za/south-african/2025-12-24-national-treasurys-plan-to-tackle-late-payments-six-provinces-under-scrutiny/)
- [Accounting Weekly](https://www.accountingweekly.com/trending-news/government-late-on-r61-billion-in-supplier-payments)

**Key Real Statistics (from National Treasury)**:
- R43.6 billion paid late in 2024/25 (28% increase YoY)
- R18.2 billion unpaid and overdue
- Provincial departments: 75% of late payments, 97% of unpaid
- Worst provinces: KwaZulu-Natal (R3.32bn late), Gauteng (R4.31bn unpaid), Eastern Cape
- Worst national depts: Defence, Correctional Services, Public Works
- Best performer: Western Cape (not in top 6 offenders)

---

## 5. Implementation Phases

### Phase 1: Database & Backend (Day 1)
1. Create database migrations for new tables
2. Create seed data loading endpoint
3. Update analytics endpoints to use real queries
4. Integrate scoring into discovery pipeline
5. Add PDF URL to tender list response

### Phase 2: Android - Data Layer (Day 2)
1. Add API service methods for new endpoints
2. Add DTOs for compliance, costs, pipeline
3. Add Room entities for offline caching
4. Update repository implementations

### Phase 3: Android - UI Layer (Day 3)
1. Update ViewModels to fetch real data
2. Add PDF view/download buttons to TenderDetailScreen
3. Update compliance screen to persist changes
4. Update pipeline screen to persist changes
5. Add cost entry form

### Phase 4: Verification (Day 4)
1. Search entire codebase for remaining mock data
2. Generate verification report
3. Test all features with real data
4. Document any legitimate defaults vs mock data

---

## 6. Acceptance Criteria

### 6.1 Core Features Working

- [ ] Tender list shows real scores (not null)
- [ ] BidStrengthRadar visible on tender detail when score exists
- [ ] Compliance gauges show data from database
- [ ] Funnel shows real conversion rates from tender history
- [ ] Costs show data from cost_items table
- [ ] Pipeline shows user's tracked tenders
- [ ] PDF can be viewed/downloaded

### 6.2 Zero Mock Data Verification

- [ ] `grep -r "user_001" src/` returns 0 results
- [ ] `grep -r "TODO.*mock" src/` returns 0 results
- [ ] `grep -r "TODO.*production" src/` returns 0 results
- [ ] `grep -ri "sample tender" android/` returns 0 results
- [ ] All hardcoded statistics replaced with queries
- [ ] All "In production" comments removed
- [ ] Verification report generated and attached

### 6.3 Data Persistence

- [ ] Adding a cost item persists to database
- [ ] Updating compliance expiry persists to database
- [ ] Adding tender to pipeline persists to database
- [ ] Recording outcome persists to database

---

## 7. Files to Modify

### Backend
| File | Changes |
|------|---------|
| `src/api/models/` | Add company_profile.py, compliance.py, cost_item.py, tender_outcome.py, pipeline_tender.py |
| `src/api/schemas/analytics.py` | Already exists, verify completeness |
| `src/api/routers/analytics.py` | Replace ALL mock data with DB queries |
| `src/api/routers/tenders.py` | Add pdf_url to TenderListItem response |
| `src/api/routers/pipeline.py` | Integrate scoring after extraction |
| `src/api/services/` | Add analytics_service.py |
| `alembic/versions/` | Add migration for new tables |

### Android
| File | Changes |
|------|---------|
| `data/.../remote/TenderApi.kt` | Add analytics endpoints |
| `data/.../remote/dto/` | Add ComplianceDto, CostDto, PipelineDto |
| `data/.../local/entity/` | Add entities for offline caching |
| `data/.../repository/` | Add AnalyticsRepository |
| `app/.../screens/dashboard/DashboardViewModel.kt` | Fetch real funnel data |
| `app/.../screens/analytics/AnalyticsViewModel.kt` | Fetch real costs, delays |
| `app/.../screens/compliance/ComplianceViewModel.kt` | Fetch & persist real compliance |
| `app/.../screens/pipeline/PipelineViewModel.kt` | Fetch & persist real pipeline |
| `app/.../screens/tender/TenderDetailScreen.kt` | Add PDF buttons |
| `app/.../screens/tender/TenderDetailViewModel.kt` | Add openPdf(), downloadPdf() |

---

## 8. Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Empty data on first install | Expected | Low | Clear empty states with CTAs to add data |
| Scoring fails (no model) | Medium | High | Graceful fallback, show "Score pending" |
| Large migration | Low | Medium | Test migrations thoroughly |
| API compatibility | Low | Low | Version API, maintain backwards compat |
| User doesn't enter profile | Medium | Medium | Prompt on first login, require for scoring |

---

## 9. Verification Report Template

```
ZERO MOCK DATA VERIFICATION REPORT
==================================
Date: [DATE]
Change ID: zero-mock-data-production

CODEBASE SEARCH RESULTS:
------------------------
Search: grep -r "user_001"
Result: 0 matches

Search: grep -ri "mock" --include="*.kt" --include="*.py"
Result: 0 matches in production code

Search: grep -ri "TODO.*production"
Result: 0 matches

Search: grep -ri "hardcoded"
Result: 0 matches in production code

Search: grep -ri "sample tender"
Result: 0 matches

FEATURE VERIFICATION:
---------------------
[ ] Tender scores displayed from database: PASS/FAIL
[ ] BidStrengthRadar visible when score exists: PASS/FAIL
[ ] Compliance data from compliance_registrations table: PASS/FAIL
[ ] Funnel data from tender_outcomes table: PASS/FAIL
[ ] Cost data from cost_items table: PASS/FAIL
[ ] Pipeline data from pipeline_tenders table: PASS/FAIL
[ ] PDF view button works: PASS/FAIL
[ ] All CRUD operations persist: PASS/FAIL

CONCLUSION:
-----------
Mock data eliminated: YES/NO
Production ready: YES/NO

Signed: _____________
Date: _____________
```

---

## 10. Confirmed Requirements

Based on user feedback:

1. **No seed data** - App starts EMPTY. User enters their own real data.

2. **Score on sync** - Scoring runs AUTOMATICALLY every time tenders sync from API.

3. **Payment delays** - Uses REAL National Treasury research data initially. Over time, supplemented/overridden by user's actual payment experiences from their recorded outcomes.

4. **Offline cache** - YES. All data (compliance, costs, pipeline, tenders) cached offline.

---

**Status**: PENDING APPROVAL

This is a comprehensive change that will transform the app from demo to production-ready. Awaiting approval before implementation.
