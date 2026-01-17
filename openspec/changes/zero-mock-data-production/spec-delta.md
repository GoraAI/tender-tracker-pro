# Spec Delta: Zero Mock Data - Production Readiness

**Change ID**: `zero-mock-data-production`

---

## Database Schema Additions

### New Tables

```sql
-- Company profile for scoring criteria
company_profiles (
    id UUID PK,
    user_id UUID FK -> users,
    legal_name, trading_name, industry,
    capabilities JSONB,
    preferred_sectors JSONB,
    preferred_provinces JSONB,
    value_range_min DECIMAL,
    value_range_max DECIMAL,
    keywords JSONB
)

-- Compliance registrations
compliance_registrations (
    id UUID PK,
    user_id UUID FK -> users,
    type VARCHAR(20),  -- csd, cidb, bbbee, sars, uif, coid
    name, short_name, registration_number,
    issue_date DATE,
    expiry_date DATE,
    expiry_type VARCHAR(20),  -- annual, perpetual, continuous
    renewal_url, documents JSONB
)

-- Cost tracking
cost_items (
    id UUID PK,
    user_id UUID FK -> users,
    tender_id UUID FK -> tenders NULL,
    category VARCHAR(50),  -- platform, compliance, preparation, submission, opportunity
    description, amount DECIMAL, date DATE,
    recurring BOOLEAN, frequency VARCHAR(20)
)

-- Tender outcomes
tender_outcomes (
    id UUID PK,
    user_id UUID FK -> users,
    tender_id UUID FK -> tenders,
    outcome VARCHAR(20),  -- won, lost, withdrawn, cancelled
    reason_code VARCHAR(50),  -- price, functionality, bbbee, disqualified
    reason_details TEXT,
    award_date DATE,
    award_amount DECIMAL,
    payment_received_date DATE,
    days_to_payment INTEGER
)

-- Pipeline tracking
pipeline_tenders (
    id UUID PK,
    user_id UUID FK -> users,
    tender_id UUID FK -> tenders,
    status VARCHAR(20),  -- discovered, qualified, preparation, review, submission
    start_date DATE,
    phases JSONB,
    assigned_to VARCHAR(255),
    notes TEXT,
    has_conflict BOOLEAN
)
```

---

## API Changes

### Modified Endpoints

```diff
GET /analytics/funnel
- Returns hardcoded: discovered=45, qualified=28, submitted=18, awarded=6
+ Returns: SELECT from tender_outcomes aggregated by user

GET /analytics/costs
- Returns hardcoded: platform=17100, compliance=24400, etc.
+ Returns: SELECT SUM(amount) FROM cost_items GROUP BY category WHERE user_id = current_user

POST /analytics/costs
- Returns fake object without saving
+ INSERT INTO cost_items, returns saved object

GET /analytics/compliance
- Returns 6 hardcoded items with user_id="user_001"
+ Returns: SELECT * FROM compliance_registrations WHERE user_id = current_user

PUT /analytics/compliance/{id}
- Returns fake object
+ UPDATE compliance_registrations SET ... WHERE id = ? AND user_id = current_user

GET /analytics/payment-delays
- Returns hardcoded province/department data
+ Returns: REAL National Treasury statistics from data/reference/payment_delays_sa_treasury.json
+ Supplemented by user's actual payment experiences from tender_outcomes over time

GET /analytics/pipeline
- Returns empty list
+ Returns: SELECT * FROM pipeline_tenders WHERE user_id = current_user

POST /analytics/pipeline
- Returns fake with title="Sample Tender"
+ INSERT INTO pipeline_tenders with actual tender details

PATCH /analytics/pipeline/{id}
- Returns fake object
+ UPDATE pipeline_tenders SET status = ? WHERE id = ? AND user_id = current_user

DELETE /analytics/pipeline/{id}
- Returns status only
+ DELETE FROM pipeline_tenders WHERE id = ? AND user_id = current_user

POST /analytics/outcomes
- Returns fake with id="outcome_001"
+ INSERT INTO tender_outcomes, returns saved object

GET /analytics/outcomes
- Returns hardcoded: total_submitted=18, won=6, lost=12
+ Returns: SELECT COUNT(*), outcome FROM tender_outcomes GROUP BY outcome
```

### TenderListItem Schema Change

```diff
class TenderListItem(BaseModel):
    id: UUID
    source_id: str
    title: Optional[str]
    reference: Optional[str]
    deadline: Optional[datetime]
    estimated_value: Optional[Decimal]
    discovered_at: datetime
    score: Optional[int]
    relevance_category: Optional[str]
+   pdf_url: Optional[str]
+   score_explanation: Optional[str]
```

---

## Android Changes

### ViewModels - Data Sources

```diff
DashboardViewModel:
- complianceValidCount = 5 (hardcoded)
- complianceTotalCount = 6 (hardcoded)
- funnel calculated with fake percentages
+ All data fetched from AnalyticsRepository

AnalyticsViewModel:
- CostCategory with hardcoded amounts
- PaymentDelayData with hardcoded provinces
+ All data fetched from AnalyticsRepository

ComplianceViewModel:
- 6 hardcoded ComplianceItem objects
+ Data fetched from AnalyticsRepository
+ Updates persist via API

PipelineViewModel:
- Pipeline items generated from tender list with fake phases
+ Data fetched from AnalyticsRepository
+ Add/remove/update persist via API
```

### TenderDetailScreen - New UI

```diff
TenderDetailScreen:
  // After Requirements Summary card
+ // PDF Actions Row
+ Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
+     OutlinedButton(onClick = { viewModel.openPdf() }) {
+         Icon(Icons.Default.PictureAsPdf, "PDF")
+         Text("View PDF")
+     }
+     OutlinedButton(onClick = { viewModel.downloadPdf() }) {
+         Icon(Icons.Default.Download, "Download")
+         Text("Download")
+     }
+ }
```

### TenderDetailViewModel - New Functions

```diff
TenderDetailViewModel:
+ sealed class TenderDetailEvent {
+     data class OpenUrl(val url: String) : TenderDetailEvent()
+     data class ShowError(val message: String) : TenderDetailEvent()
+ }
+
+ private val _events = MutableSharedFlow<TenderDetailEvent>()
+ val events: SharedFlow<TenderDetailEvent> = _events.asSharedFlow()
+
+ fun openPdf() {
+     state.value.tender?.pdfUrl?.let { url ->
+         viewModelScope.launch {
+             _events.emit(TenderDetailEvent.OpenUrl(url))
+         }
+     } ?: run {
+         viewModelScope.launch {
+             _events.emit(TenderDetailEvent.ShowError("PDF not available"))
+         }
+     }
+ }
```

---

## Reference Data (Real, Not Mock)

Created in `data/reference/`:

| File | Contents |
|------|----------|
| `payment_delays_sa_treasury.json` | REAL National Treasury payment delay statistics by province and department |

**Sources**:
- [National Treasury Annual Report 2024/25](https://treasury.gov.za/legislation/pfma/Compliance%20on%20Payment%20of%20Suppliers/)
- [National Treasury Q2 2025/26 Report](https://dfa.co.za/south-african/2025-12-24-national-treasurys-plan-to-tackle-late-payments-six-provinces-under-scrutiny/)

**Key Statistics**:
- R43.6 billion paid late in 2024/25
- KwaZulu-Natal: R3.32bn late (worst)
- Gauteng: R4.31bn unpaid (worst)
- Western Cape: Best performer (not in top 6 offenders)
- Provincial depts: 75% of late payments, 97% of unpaid

## Empty State Strategy

**NO seed data**. App starts empty. User enters their own:
- Company profile (required for scoring)
- Compliance registrations
- Cost items
- Pipeline tenders
- Tender outcomes

Each section shows helpful empty state with CTA to add data.

---

## Mock Data Removed

| Location | Mock Data | Replacement |
|----------|-----------|-------------|
| analytics.py:48-53 | discovered=45, qualified=28, etc. | DB query |
| analytics.py:86 | cost_per_win=42500 | Calculated |
| analytics.py:87 | avg_days_to_award=47 | Calculated |
| analytics.py:112-142 | Cost categories | DB query |
| analytics.py:146 | tenders_won=5 | DB query |
| analytics.py:168 | user_id="user_001" | current_user.id |
| analytics.py:193-273 | 6 compliance items | DB query |
| analytics.py:326-344 | Payment delays | DB/reference |
| analytics.py:387-397 | Sample Tender | DB query |
| analytics.py:467-481 | Outcome stats | DB query |
| DashboardViewModel.kt:35-37 | Compliance defaults | API fetch |
| DashboardViewModel.kt:104-115 | Fake attention | API fetch |
| DashboardViewModel.kt:133-137 | Fake funnel | API fetch |
| AnalyticsViewModel.kt:60-64 | Fake funnel | API fetch |
| AnalyticsViewModel.kt:93-143 | Fake costs | API fetch |
| AnalyticsViewModel.kt:149-160 | Fake delays | API fetch |
| ComplianceViewModel.kt:49-104 | 6 fake items | API fetch |
| PipelineViewModel.kt:56-78 | Fake pipeline | API fetch |

---

## Verification Criteria

```bash
# All must return 0 matches:
grep -r "user_001" src/api/
grep -r "cost_001" src/api/
grep -r "outcome_001" src/api/
grep -ri "sample tender" src/api/
grep -ri "TODO.*production" src/api/
grep -ri "// In production" android/app/
grep -ri "mock" android/app/src/main/java/ (excluding imports)
```

---

**Status**: PENDING APPROVAL
