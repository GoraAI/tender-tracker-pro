# Implementation Tasks: Zero Mock Data - Production Readiness

**Change ID**: `zero-mock-data-production`
**Status**: PENDING APPROVAL

---

## Phase 1: Database & Backend Foundation

### 1.1 Database Models
- [ ] Create `CompanyProfile` model in `src/api/models/company_profile.py`
- [ ] Create `ComplianceRegistration` model in `src/api/models/compliance.py`
- [ ] Create `CostItem` model in `src/api/models/cost_item.py`
- [ ] Create `TenderOutcome` model in `src/api/models/tender_outcome.py`
- [ ] Create `PipelineTender` model in `src/api/models/pipeline_tender.py`
- [ ] Update `src/api/models/__init__.py` with new exports

### 1.2 Database Migrations
- [ ] Create Alembic migration for all new tables
- [ ] Test migration up/down
- [ ] Add seed data loading script

### 1.3 Backend Services
- [ ] Create `AnalyticsService` class in `src/api/services/analytics_service.py`
- [ ] Implement `get_funnel_analytics()` - aggregate from tender_outcomes
- [ ] Implement `get_cost_analytics()` - aggregate from cost_items
- [ ] Implement `get_compliance_status()` - query compliance_registrations
- [ ] Implement `get_payment_delays()` - calculate from tender_outcomes
- [ ] Implement `get_pipeline_status()` - query pipeline_tenders
- [ ] Implement `get_outcome_analytics()` - aggregate from tender_outcomes

### 1.4 Analytics Router Rewrite
- [ ] Replace `GET /analytics/funnel` - remove mock, use service
- [ ] Replace `GET /analytics/costs` - remove mock, use service
- [ ] Replace `POST /analytics/costs` - persist to cost_items table
- [ ] Replace `GET /analytics/compliance` - remove mock, use service
- [ ] Replace `PUT /analytics/compliance/{id}` - persist to compliance_registrations
- [ ] Replace `GET /analytics/payment-delays` - use service
- [ ] Replace `GET /analytics/pipeline` - remove mock, use service
- [ ] Replace `POST /analytics/pipeline` - persist to pipeline_tenders
- [ ] Replace `PATCH /analytics/pipeline/{id}` - persist update
- [ ] Replace `DELETE /analytics/pipeline/{id}` - persist delete
- [ ] Replace `POST /analytics/outcomes` - persist to tender_outcomes
- [ ] Replace `GET /analytics/outcomes` - use service
- [ ] Remove ALL "user_001" hardcoded references
- [ ] Remove ALL TODO comments about mock/production

### 1.5 Tenders Router Updates
- [ ] Add `pdf_url` to `TenderListItem` schema
- [ ] Include `pdf_url` in `GET /tenders` response

### 1.6 Pipeline Integration - Auto Scoring on Sync
- [ ] Update tender sync to trigger scoring automatically
- [ ] Load user's company profile for scoring criteria
- [ ] Store scores in Score table
- [ ] Handle scoring failures gracefully (mark as "pending")
- [ ] Re-score when user updates profile

### 1.7 Payment Delay Reference Data
- [ ] Create endpoint to serve reference data from `data/reference/payment_delays_sa_treasury.json`
- [ ] This is REAL National Treasury data, not mock data
- [ ] Over time, blend with user's actual payment experiences

### 1.8 Empty State Handling
- [ ] Return empty arrays (not fake data) when user has no compliance registrations
- [ ] Return empty arrays when user has no cost items
- [ ] Return empty arrays when user has no pipeline tenders
- [ ] Return zeros for funnel when user has no outcome history
- [ ] Include `isEmpty: true` flag in responses for UI empty states

---

## Phase 2: Android Data Layer

### 2.1 API Service Extensions
- [ ] Add `getComplianceStatus()` to TenderApi
- [ ] Add `updateComplianceItem()` to TenderApi
- [ ] Add `getCostAnalytics()` to TenderApi
- [ ] Add `addCostItem()` to TenderApi
- [ ] Add `getPipelineStatus()` to TenderApi
- [ ] Add `addToPipeline()` to TenderApi
- [ ] Add `updatePipelineStatus()` to TenderApi
- [ ] Add `removeFromPipeline()` to TenderApi
- [ ] Add `getFunnelAnalytics()` to TenderApi
- [ ] Add `getPaymentDelays()` to TenderApi
- [ ] Add `recordOutcome()` to TenderApi
- [ ] Add `getOutcomeAnalytics()` to TenderApi

### 2.2 DTOs
- [ ] Create `ComplianceStatusResponse` DTO
- [ ] Create `ComplianceItemDto` DTO
- [ ] Create `CostAnalyticsResponse` DTO
- [ ] Create `CostItemDto` DTO
- [ ] Create `FunnelAnalyticsResponse` DTO
- [ ] Create `PaymentDelayResponse` DTO
- [ ] Create `PipelineStatusResponse` DTO
- [ ] Create `PipelineTenderDto` DTO
- [ ] Create `OutcomeAnalyticsResponse` DTO
- [ ] Update `TenderDto` to include `pdfUrl` in list response

### 2.3 Room Entities (Offline Cache)
- [ ] Create `ComplianceItemEntity` for offline compliance data
- [ ] Create `CostItemEntity` for offline cost tracking
- [ ] Create `PipelineTenderEntity` for offline pipeline
- [ ] Create `FunnelCacheEntity` for cached funnel stats
- [ ] Add DAOs for each entity

### 2.4 Repository Layer
- [ ] Create `AnalyticsRepository` interface in domain
- [ ] Implement `AnalyticsRepositoryImpl` in data
- [ ] Add Hilt module bindings

---

## Phase 3: Android UI Layer

### 3.1 DashboardViewModel
- [ ] Remove hardcoded compliance summary defaults (lines 35-37)
- [ ] Remove fake attention item generation (lines 104-115)
- [ ] Remove fake funnel calculation (lines 133-137)
- [ ] Remove fake pipeline summary (lines 148-151)
- [ ] Fetch compliance summary from `AnalyticsRepository`
- [ ] Fetch funnel summary from `AnalyticsRepository`
- [ ] Fetch pipeline summary from `AnalyticsRepository`
- [ ] Generate attention items from real compliance data

### 3.2 AnalyticsViewModel
- [ ] Remove all hardcoded funnel numbers (lines 60-64)
- [ ] Remove all hardcoded cost categories (lines 93-143)
- [ ] Remove all hardcoded payment delay data (lines 149-160)
- [ ] Remove hardcoded avgDaysToAward (line 166)
- [ ] Fetch all data from `AnalyticsRepository`

### 3.3 ComplianceViewModel
- [ ] Remove all hardcoded compliance items (lines 49-104)
- [ ] Remove "In production" comments
- [ ] Fetch compliance from `AnalyticsRepository`
- [ ] Implement real persistence in `updateComplianceItem()`
- [ ] Add refresh/reload capability

### 3.4 PipelineViewModel
- [ ] Remove fake pipeline generation (lines 56-78)
- [ ] Remove hardcoded phase percentages (lines 115-117)
- [ ] Remove fake conflict detection (line 75)
- [ ] Remove "In production" comments (lines 149, 156, 163)
- [ ] Fetch pipeline from `AnalyticsRepository`
- [ ] Implement real persistence for status updates
- [ ] Implement real persistence for add/remove

### 3.5 Empty State UI Components
- [ ] Create `EmptyStateCard` composable for reuse
- [ ] ComplianceScreen: "Add your first registration" with CTA button
- [ ] AnalyticsScreen/Funnel: "Record outcomes to see your funnel"
- [ ] AnalyticsScreen/Costs: "Log costs to track your spend"
- [ ] PipelineScreen: "Add tenders to your pipeline"
- [ ] DashboardScreen: Graceful handling when all sections empty

### 3.6 TenderDetailScreen - PDF Buttons
- [ ] Add "View PDF" OutlinedButton
- [ ] Add "Download PDF" OutlinedButton
- [ ] Add PDF icon imports
- [ ] Style buttons to match glassmorphism theme

### 3.7 TenderDetailViewModel - PDF Actions
- [ ] Add `sealed class TenderDetailEvent`
- [ ] Add `OpenUrl(url: String)` event
- [ ] Add `DownloadPdf(url: String, filename: String)` event
- [ ] Add `ShowError(message: String)` event
- [ ] Implement `openPdf()` function
- [ ] Implement `downloadPdf()` function
- [ ] Add SharedFlow for events
- [ ] Handle events in TenderDetailScreen

### 3.8 TenderDetailScreen - Event Handling
- [ ] Collect events from ViewModel
- [ ] Launch browser intent for OpenUrl
- [ ] Start download for DownloadPdf
- [ ] Show snackbar for errors

---

## Phase 4: Verification & Cleanup

### 4.1 Backend Mock Data Removal Verification
- [ ] Run: `grep -r "user_001" src/api/` - must return 0 results
- [ ] Run: `grep -r "cost_001" src/api/` - must return 0 results
- [ ] Run: `grep -r "outcome_001" src/api/` - must return 0 results
- [ ] Run: `grep -r "pipeline_001" src/api/` - must return 0 results
- [ ] Run: `grep -ri "TODO.*production" src/api/` - must return 0 results
- [ ] Run: `grep -ri "TODO.*mock" src/api/` - must return 0 results
- [ ] Run: `grep -ri "sample tender" src/api/` - must return 0 results

### 4.2 Android Mock Data Removal Verification
- [ ] Run: `grep -ri "user_001" android/app/` - must return 0 results
- [ ] Run: `grep -ri "// In production" android/app/` - must return 0 results
- [ ] Run: `grep -ri "mock" android/app/src/main/` - must return 0 (excluding imports/comments)
- [ ] Run: `grep -ri "hardcoded" android/app/` - must return 0 results
- [ ] Verify no inline `listOf()` with fake data in ViewModels

### 4.3 Feature Testing
- [ ] Test: Tender list shows real scores
- [ ] Test: BidStrengthRadar appears when score exists
- [ ] Test: Compliance gauges show database data
- [ ] Test: Updating compliance persists to database
- [ ] Test: Funnel shows real conversion rates
- [ ] Test: Costs show data from database
- [ ] Test: Adding cost item persists
- [ ] Test: Pipeline shows user's tracked tenders
- [ ] Test: Adding to pipeline persists
- [ ] Test: PDF view button opens browser
- [ ] Test: All attention items reflect real compliance state

### 4.4 Generate Verification Report
- [ ] Create `VERIFICATION_REPORT.md` with all grep results
- [ ] Document each feature pass/fail
- [ ] Sign off on zero mock data confirmation

---

## Dependencies

| Task | Depends On |
|------|------------|
| 1.4 Analytics Router Rewrite | 1.1 Models, 1.2 Migrations, 1.3 Services |
| 1.6 Auto Scoring | 1.5 Tenders Router |
| 2.1 API Service | 1.4 Analytics Router |
| 2.4 Repository | 2.1 API, 2.2 DTOs, 2.3 Entities |
| 3.1-3.4 ViewModels | 2.4 Repository |
| 3.5-3.7 PDF Feature | 1.5 Tenders Router |
| 4.1-4.4 Verification | All implementation tasks |

---

## Estimated Complexity

| Phase | Tasks | Complexity | Notes |
|-------|-------|------------|-------|
| Phase 1 | 31 | High | Database foundation |
| Phase 2 | 24 | Medium | Data layer wiring |
| Phase 3 | 30 | Medium | UI updates |
| Phase 4 | 15 | Low | Verification |
| **Total** | **100** | | |

---

**Status**: PENDING APPROVAL

Tasks will be marked complete as implementation proceeds after approval.
