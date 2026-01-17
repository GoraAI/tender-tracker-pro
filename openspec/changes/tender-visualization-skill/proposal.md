# OpenSpec Proposal: Tender Visualisation Skill

**Change ID**: `tender-visualization-skill`
**Status**: IMPLEMENTED
**Created**: 2026-01-17
**Implemented**: 2026-01-17
**Author**: Claude (Product Architect Mode)

---

## 1. Definition of the Tender Visualisation Skill

### 1.1 Skill Definition

The Tender Visualisation Skill is an integrated capability spanning:

| Domain | Capability |
|--------|------------|
| **Data Modelling** | Transform raw tender/compliance data into decision-grade structures |
| **Visual Encoding** | Map data dimensions to perceptual channels (position, color, size, shape) |
| **UI Composition** | Assemble visual components into coherent dashboard surfaces |
| **UX Navigation** | Enable fluid movement between visual contexts without losing state |

### 1.2 What Applicants Can Now SEE (Previously Opaque)

| Previously Opaque | Now Visible | Research Source |
|-------------------|-------------|-----------------|
| Win-rate by category/value range | Personal funnel analytics showing Discovery → Qualification → Submission → Award conversion | Research B: "No platform exposes win-rate metrics" |
| Cost-per-successful-bid | ROI dashboard aggregating subscription + certification + preparation + opportunity cost | Research A: "Cost per successful bid = R50,000-R250,000" |
| Competitive density per tender | Bid competition gauge showing historical bidder counts for similar tenders | Research B: "Average Bids Per Tender - might discourage bidding" |
| Payment delay risk by department | Heat map of historical payment timelines by procuring entity | Research A: "100+ day delays in some provinces" |
| Compliance expiration cascade | Timeline showing upcoming expirations across CSD/CIDB/B-BBEE/SARS/UIF/COID | Research A: "10+ registration systems" |
| Disqualification patterns | Root cause visualization of rejection reasons from debriefs | Research A: "considerable litigation over bid disqualification" |

### 1.3 Decisions Accelerated or De-Risked

| Decision | Acceleration Mechanism | Risk Reduction |
|----------|----------------------|----------------|
| **Bid/No-Bid** | Competitive density + historical win-rate + margin calculator | Avoid low-probability, high-effort submissions |
| **Price Setting** | Payment delay cost modeling + awarded tender price patterns | Price in true cost of capital during delays |
| **Resource Allocation** | Parallel tender Gantt with deadline clustering | Prevent team overcommitment |
| **Compliance Timing** | Expiration timeline with renewal lead times | Prevent administrative disqualification |
| **Cash Flow Planning** | Payment delay distribution by department | Reserve adequate working capital |

### 1.4 Actions Triggered Directly from Visuals

| Visual | Direct Action | Implementation |
|--------|---------------|----------------|
| Tender card with urgency indicator | One-click "Start Bid" → pre-populated checklist | Opens bid preparation flow |
| Compliance gauge in red zone | "Renew Now" button → opens registration system in new tab | Direct link to CSD/CIDB/SARS |
| Pipeline calendar showing conflict | "Defer" or "Delegate" action on tender card | Updates bid status, notifies team |
| Payment risk warning on tender | "Factor This" button → opens invoice factoring quote flow | Integration with financing partner |
| Win-rate below threshold | "Request Debrief" for recent losses | Generates debrief request template |

---

## 2. Visual Models → UI Components

### 2.1 Opportunity Funnel Visualisation

**Visual Model**: Sankey/Funnel diagram showing tender flow through lifecycle stages

**Research Basis**: "Funnel Analysis: Discovery → Qualification → Submission → Award - Identify conversion bottlenecks" (Research B)

**UI Realisation**:
```
┌─────────────────────────────────────────────────────────────────┐
│  OPPORTUNITY FUNNEL                              [Period ▼]     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ████████████████████████████████████████  Discovered (45)      │
│  ▼                                                              │
│  ██████████████████████████                Qualified (28)       │
│  ▼                                         ↳ 17 filtered out    │
│  ████████████████                          Submitted (18)       │
│  ▼                                         ↳ 10 no-bid decision │
│  ██████                                    Awarded (6)          │
│                                            ↳ 12 unsuccessful    │
│                                                                 │
│  Win Rate: 33%    Cost/Win: R42,500    Avg Days to Award: 47   │
└─────────────────────────────────────────────────────────────────┘
```

**Spatial Layout**:
- Vertical flow, top-to-bottom
- Width encodes volume at each stage
- Drop-off annotations on right margin
- Summary metrics in footer bar

**User Interaction**:
| Action | Result |
|--------|--------|
| Click funnel stage | Filters tender list below to that stage |
| Hover stage | Shows breakdown by category/value range |
| Click drop-off annotation | Opens list of filtered/rejected tenders with reasons |
| Drag period selector | Animates funnel over time |

---

### 2.2 Compliance Health Dashboard

**Visual Model**: Multi-gauge dashboard with timeline overlay

**Research Basis**: "Gauge/KPI Dashboard: Real-time compliance status across registrations - Prevent expiration-based disqualification" (Research B); "10+ registration and certification systems" (Research A)

**UI Realisation**:
```
┌─────────────────────────────────────────────────────────────────┐
│  COMPLIANCE STATUS                                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │   CSD   │ │  CIDB   │ │ B-BBEE  │ │  SARS   │ │   UIF   │   │
│  │  [████] │ │  [██░░] │ │  [█░░░] │ │  [████] │ │  [███░] │   │
│  │  Valid  │ │ 45 days │ │ URGENT  │ │  Valid  │ │ 21 days │   │
│  │         │ │ [Renew] │ │ [Renew] │ │         │ │         │   │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘   │
│                                                                 │
│  EXPIRATION TIMELINE                                            │
│  ─────┬─────────┬─────────┬─────────┬─────────┬─────────────   │
│  Today    +30d      +60d      +90d      +120d      +180d        │
│       ▲B-BBEE   ▲CIDB              ▲UIF                         │
│       (12 days) (45 days)          (98 days)                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Spatial Layout**:
- Horizontal gauge array at top (5-6 registrations)
- Timeline below showing future expirations
- Color gradient: Green (>90 days) → Yellow (30-90) → Orange (7-30) → Red (<7)

**User Interaction**:
| Action | Result |
|--------|--------|
| Click gauge | Opens registration details panel with renewal requirements |
| Click "Renew" button | Opens external system in new tab OR in-app renewal flow |
| Hover timeline marker | Shows exact expiration date and renewal lead time |
| Toggle "Show All" | Includes optional registrations (NHBRC, etc.) |

---

### 2.3 Tender Pipeline Calendar

**Visual Model**: Gantt-style timeline with deadline clustering detection

**Research Basis**: "Gantt/Timeline: Parallel tender preparation with milestone tracking - Workload balancing for bid teams" (Research B); "Time-to-submit varies by tender complexity" (Research A)

**UI Realisation**:
```
┌─────────────────────────────────────────────────────────────────┐
│  PIPELINE CALENDAR                           [Week ▼] [+ Add]   │
├─────────────────────────────────────────────────────────────────┤
│        Mon 20   Tue 21   Wed 22   Thu 23   Fri 24   Sat 25     │
│  ├─────────┼─────────┼─────────┼─────────┼─────────┼─────────┤  │
│                                                                 │
│  DOH Medical Supplies ████████████████████████▶ [Due]          │
│  Prepare ──────────── Review ────── Submit                      │
│                                                                 │
│  CoJ Road Maintenance     ████████████████████████████▶        │
│                           Prepare ────────── Review ── Submit   │
│                                                                 │
│  ⚠️ CONFLICT: 2 deadlines on Fri 24                             │
│                                                                 │
│  Eskom Cable Supply           [░░░░░░░░░░░░░░░░░░░░░░░]        │
│                               (Considering - not started)       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Spatial Layout**:
- Horizontal time axis (configurable: day/week/month)
- Each tender as horizontal bar with phase segments
- Conflict warnings highlighted inline
- "Considering" tenders shown as ghosts

**User Interaction**:
| Action | Result |
|--------|--------|
| Drag tender bar | Reschedules internal milestones (adjusts review/submit phases) |
| Click conflict warning | Opens resolution panel with defer/delegate options |
| Click tender bar | Expands to show detailed task checklist |
| Right-click tender | Context menu: Abandon, Defer, Mark Complete |
| Click "+ Add" | Opens tender search to add to pipeline |

---

### 2.4 Competitive Intelligence Radar

**Visual Model**: Radar/spider chart for bid strength self-assessment

**Research Basis**: "Radar/Spider Chart: Bid strength assessment across evaluation criteria - Self-assessment before submission" (Research B); "80/20 and 90/10 tender scoring" (Research A)

**UI Realisation**:
```
┌─────────────────────────────────────────────────────────────────┐
│  BID STRENGTH ASSESSMENT: DOH Medical Supplies                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                        Price (20%)                              │
│                           ▲                                     │
│                          /█\                                    │
│                         / █ \                                   │
│         B-BBEE (20%) ◄──█████──► Functionality (60%)           │
│                         \ █ /                                   │
│                          \█/                                    │
│                           ▼                                     │
│                     Track Record                                │
│                                                                 │
│  Overall Score: 72/100              [Compare to Winners ▼]      │
│                                                                 │
│  ⚠️ Weak: Track Record (no prior DOH contracts)                 │
│  ✓ Strong: B-BBEE Level 1, Competitive pricing                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Spatial Layout**:
- Radar chart centered, axes labeled with evaluation criteria
- Filled area shows self-assessed strength
- Overlay option to show historical winner profiles
- Weakness/strength callouts below

**User Interaction**:
| Action | Result |
|--------|--------|
| Drag axis point | Adjusts self-assessment score for that criterion |
| Click "Compare to Winners" | Overlays historical winner profile (if data available) |
| Click weakness callout | Opens suggestion panel for improvement actions |
| Toggle "Show Minimum" | Shows threshold lines for each criterion |

---

### 2.5 Payment Risk Heat Map

**Visual Model**: Geographic/categorical heat map of payment delays

**Research Basis**: "Payment delays exceed 100 days in some provinces"; "North West Province: delays exceed 100 days"; "Department of Defence: 78% of all late national payments" (Research A)

**UI Realisation**:
```
┌─────────────────────────────────────────────────────────────────┐
│  PAYMENT DELAY RISK                              [By Dept ▼]    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  BY PROVINCE (Avg Days to Payment)                              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                     Limpopo [52]                        │   │
│  │     North West [98]  ████████████████                   │   │
│  │     ██████████████████████████                          │   │
│  │                       Gauteng [34]                      │   │
│  │     Free State [67]   ████████                          │   │
│  │     █████████████████  Mpumalanga [45]                  │   │
│  │                        ████████████                     │   │
│  │     Northern Cape [41] KZN [38]  Eastern Cape [71]      │   │
│  │     ██████████   ██████████  ████████████████████       │   │
│  │                   Western Cape [29]                     │   │
│  │                   ██████                                │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Color: ██ <30d  ████ 30-60d  ██████ 60-90d  ████████ >90d     │
│                                                                 │
│  Selected Tender: DOH Gauteng → Expected payment: 34-45 days   │
│  Cash flow impact: R125,000 tied up @ 12% = R1,850 cost        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Spatial Layout**:
- Geographic map or categorical grid view (toggle)
- Color intensity encodes delay severity
- Selected tender highlights relevant entity
- Cash flow impact calculator in footer

**User Interaction**:
| Action | Result |
|--------|--------|
| Hover region/department | Shows detailed payment statistics |
| Click region | Filters tender list to that procuring entity |
| Toggle "By Dept" | Switches between geographic and departmental view |
| Input contract value | Updates cash flow impact calculation |

---

### 2.6 Cost-Per-Bid Calculator

**Visual Model**: Stacked bar chart with drill-down capability

**Research Basis**: "Cost per successful bid = (Annual platform costs + certification costs + preparation costs + submission costs) ÷ Number of won contracts" (Research A); "R50,000-R250,000 before counting subscription and compliance overhead" (Research A)

**UI Realisation**:
```
┌─────────────────────────────────────────────────────────────────┐
│  COST PER SUCCESSFUL BID                         [12 months]    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Total Spent: R187,500    Won: 5    Cost/Win: R37,500          │
│                                                                 │
│  ████████████████████████████████████████████████████████████  │
│  │ Platform │ Compliance │  Preparation  │ Submission │ Opp   │ │
│  │  R17,100 │   R24,400  │    R98,000    │   R18,000  │R30,000│ │
│  │   (9%)   │   (13%)    │     (52%)     │    (10%)   │ (16%) │ │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ BREAKDOWN: Preparation (R98,000)                         │  │
│  │ • Staff time (120 hrs @ R500/hr)      R60,000            │  │
│  │ • Consultant fees (3 bids)            R25,000            │  │
│  │ • Site visits & briefings             R8,000             │  │
│  │ • Document production                 R5,000             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Break-even: Win 1 tender @ R1.5M with 2.5% margin             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Spatial Layout**:
- Stacked horizontal bar as primary visualization
- Percentage and absolute values labeled
- Drill-down panel appears on segment click
- Break-even calculation in footer

**User Interaction**:
| Action | Result |
|--------|--------|
| Click bar segment | Expands drill-down with line-item breakdown |
| Edit cost inputs | Recalculates totals and cost-per-win |
| Toggle period | Adjusts time window for analysis |
| Click "Add Tender Cost" | Opens form to log specific tender expenses |

---

## 3. UX Navigation Model

### 3.1 Initial Landing State

```
┌─────────────────────────────────────────────────────────────────┐
│  TENDER TRACKER PRO                    [Search] [+] [Profile]   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────┐  ┌─────────────────────────────────┐  │
│  │  COMPLIANCE STATUS  │  │  ACTIVE PIPELINE                │  │
│  │  ●●●●○ 4/5 Valid    │  │  ████░░ 3 in progress           │  │
│  │  [View Details]     │  │  2 due this week                │  │
│  └─────────────────────┘  └─────────────────────────────────┘  │
│                                                                 │
│  ┌────────────────────────────────────────────────────────────┐│
│  │  OPPORTUNITIES REQUIRING ATTENTION                         ││
│  │  ┌─────────────────────────────────────────────────────┐  ││
│  │  │ ⚠️ DOH Medical Supplies   Due: 3 days   Score: 82   │  ││
│  │  │    Missing: Site visit attendance                   │  ││
│  │  │    [Complete Checklist] [View Details]              │  ││
│  │  └─────────────────────────────────────────────────────┘  ││
│  │  ┌─────────────────────────────────────────────────────┐  ││
│  │  │ 🔴 B-BBEE Expires in 12 days                        │  ││
│  │  │    Affects: 4 active tenders                        │  ││
│  │  │    [Start Renewal]                                  │  ││
│  │  └─────────────────────────────────────────────────────┘  ││
│  └────────────────────────────────────────────────────────────┘│
│                                                                 │
│  NEW OPPORTUNITIES (3 matching your criteria)      [View All]   │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐                           │
│  │ Tender1 │ │ Tender2 │ │ Tender3 │                           │
│  │ R2.5M   │ │ R850K   │ │ R4.1M   │                           │
│  │ Score:74│ │ Score:91│ │ Score:68│                           │
│  └─────────┘ └─────────┘ └─────────┘                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Design Principles**:
- Attention-prioritized: Urgent items surface first
- Compliance status always visible (prevents surprise disqualification)
- Pipeline summary reduces anxiety about active work
- New opportunities shown only after attention items addressed

### 3.2 Progressive Disclosure of Complexity

| Layer | Content | User Readiness |
|-------|---------|----------------|
| **L1: Glance** | Status indicators, counts, urgency flags | Any user, any context |
| **L2: Summary** | Key metrics, aggregated visuals, actionable cards | Engaged user with 30 seconds |
| **L3: Detail** | Full tender information, document checklists, timelines | User working on specific tender |
| **L4: Analysis** | Funnel analytics, cost breakdowns, competitive intelligence | Power user or review session |
| **L5: Configuration** | Scoring criteria, notification settings, registration management | Admin or setup session |

**Disclosure Triggers**:
- L1 → L2: Click any summary card
- L2 → L3: Click specific tender
- L3 → L4: Click "Analytics" tab within tender view
- L4 → L5: Click "Settings" from any view

### 3.3 Visual-to-Visual Navigation (Not Page-Based)

**Panel Architecture**:
```
┌──────────────────────────────────────────────────────────────────┐
│  [Context Bar - Current filters, breadcrumb, search]            │
├────────────────┬─────────────────────────────────────────────────┤
│                │                                                 │
│  NAV RAIL      │           PRIMARY CANVAS                        │
│  ────────────  │                                                 │
│  Dashboard     │   Visual content occupies this space            │
│  Tenders       │                                                 │
│  Pipeline      │   Multiple visuals can stack or tile            │
│  Analytics     │                                                 │
│  Compliance    │                                                 │
│  Settings      │                                                 │
│                │                                                 │
│                ├─────────────────────────────────────────────────┤
│                │  DETAIL PANEL (slides in from right)            │
│                │                                                 │
│                │  Context-specific details without losing        │
│                │  canvas view                                    │
│                │                                                 │
└────────────────┴─────────────────────────────────────────────────┘
```

**Navigation Patterns**:
| Gesture/Action | Result | State Preserved |
|----------------|--------|-----------------|
| Click nav rail item | Replaces primary canvas | Filters, search preserved |
| Click data point in visual | Opens detail panel | Canvas remains visible |
| Escape / click outside panel | Closes detail panel | All state preserved |
| Deep link / bookmark | Restores full state | Filters, selections, panel |
| Browser back | Returns to previous canvas view | Partial state |

### 3.4 State Persistence Across Views

**Persisted State**:
- Filter selections (category, value range, province, date range)
- Search terms
- Sort order
- Selected tender (highlighted across views)
- Time period for analytics
- Expanded/collapsed panel states

**Reset Triggers**:
- Explicit "Clear Filters" action
- Logout
- Session expiry (configurable)

### 3.5 Expert Fast Paths vs Novice Safety Paths

| Need | Novice Path | Expert Path |
|------|-------------|-------------|
| Find relevant tender | Dashboard → New Opportunities → Browse | `/` keyboard search → instant filter |
| Check compliance | Dashboard → Compliance card → View Details | Glance at nav rail badge showing status |
| Start bid preparation | Tender detail → "Start Bid" button → Checklist wizard | `Cmd+N` from anywhere → Quick-add modal |
| View analytics | Dashboard → Analytics nav → Select report | Direct deep link / bookmark |
| Log tender cost | Tender detail → Costs tab → Add Cost → Form | `Cmd+$` → Quick cost entry modal |

**Keyboard Navigation** (for experts):
- `/` - Global search
- `G D` - Go to Dashboard
- `G T` - Go to Tenders
- `G P` - Go to Pipeline
- `G A` - Go to Analytics
- `Cmd+N` - New tender (quick add)
- `Esc` - Close panel / clear selection

---

## 4. Metrics → Visual → Action Mapping

| Metric | Visual Encoding | UI Placement | Direct Action |
|--------|----------------|--------------|---------------|
| **Win Rate** | Percentage badge + trend arrow | Funnel footer; Dashboard summary | Click → View contributing factors |
| **Cost Per Successful Bid** | Currency with stacked bar sparkline | Analytics dashboard; Cost calculator | Click → Drill into cost components |
| **Days to Deadline** | Color-coded countdown (green→red) | Tender card; Pipeline calendar | Click deadline → Open submission checklist |
| **Compliance Days Remaining** | Gauge fill level + color | Compliance dashboard; Nav rail badge | Click gauge → Initiate renewal |
| **Relevance Score** | 0-100 numeric + filled bar | Tender card; List view column | Click → View scoring breakdown |
| **Payment Delay Risk** | Days estimate + color indicator | Tender detail; Heat map selection | Click → Show historical data + cash impact |
| **Competitive Density** | "~X bidders" estimate + icon | Tender card (if data available) | Click → View similar tender outcomes |
| **Pipeline Utilization** | Progress bar of active/capacity | Dashboard summary | Click → Open pipeline calendar |
| **Submission Completeness** | Checklist progress (X/Y items) | Tender detail header | Click → Jump to incomplete item |
| **Document Expiry** | Icon + days remaining | Tender checklist items | Click → Upload or renew |

**Rejected Metrics** (don't drive action):
- Total tenders in database (vanity metric)
- Alert volume (no action correlation)
- Page views / session duration (platform metric, not user metric)
- "Tenders matching profile" without relevance score (false signal)

---

## 5. Visual Design Rules (Operational)

### 5.1 Hierarchy and Attention Control

| Priority | Treatment | Use Case |
|----------|-----------|----------|
| **Critical** | Red background, icon, prominent placement | Compliance expiring <7 days, deadline today |
| **Warning** | Orange/amber badge, elevated card | Deadline <7 days, compliance <30 days |
| **Information** | Blue accent, normal weight | New opportunities, status updates |
| **Success** | Green accent, checkmark icon | Completed items, won tenders |
| **Neutral** | Gray, subdued | Historical data, inactive items |

**Attention Budget**: Maximum 3 critical items shown at once. If more, group into "X critical items" summary.

### 5.2 Color Semantics

| Color | Semantic | Usage |
|-------|----------|-------|
| **Red (#DC2626)** | Risk / Urgent / Failure | Overdue, expired, rejected, critical warning |
| **Orange (#EA580C)** | Warning / Attention Needed | Approaching deadline, low score, incomplete |
| **Yellow (#CA8A04)** | Caution / Review | Medium priority, partial completion |
| **Green (#16A34A)** | Success / Valid / Safe | Won, compliant, complete, on track |
| **Blue (#2563EB)** | Information / Action / Link | Interactive elements, new items, selections |
| **Gray (#6B7280)** | Neutral / Disabled / Historical | Inactive, archived, secondary information |

**Semantic Consistency**: Never use red for non-risk, green for non-success contexts.

### 5.3 Text vs Icon Usage

| Context | Prefer | Rationale |
|---------|--------|-----------|
| Primary action buttons | Text label | Clarity over compactness |
| Status indicators | Icon + text | Recognition + accessibility |
| Navigation items | Icon + text | Scannable + learnable |
| Dense data tables | Icons with tooltip | Space efficiency |
| Inline warnings | Icon + text | Attention + explanation |
| Progress indicators | Visual + numeric | Precision + glanceability |

**Icon Library**: Use consistent, recognizable icons. Avoid decorative icons.

### 5.4 Encoding Uncertainty and Incomplete Data

| Data State | Visual Treatment |
|------------|------------------|
| **Missing data** | Dashed outline, "—" placeholder, gray fill |
| **Estimated data** | Tilde prefix (~R500K), italic text, dotted underline |
| **Stale data** | Timestamp shown, amber outline, refresh icon |
| **User-entered data** | Distinct style (e.g., editable field appearance) |
| **AI-inferred data** | "AI" badge, confidence indicator if available |
| **Conflicting sources** | Warning icon, "Sources differ" tooltip |

**Never**: Display placeholder data as real data. Always signal uncertainty.

---

## 6. Visualisation-Aware System Architecture

### 6.1 Data Inputs and Refresh Cadence

| Data Source | Refresh Cadence | Notes |
|-------------|-----------------|-------|
| eTenders Portal | Every 4 hours | API polling; OCDS format |
| CIDB i-Tender | Every 4 hours | Construction-specific |
| User compliance records | Real-time (user entry) | CSD/CIDB/B-BBEE expiry dates |
| Tender pipeline status | Real-time | User actions update immediately |
| Historical analytics | Daily aggregation | Win rate, cost calculations |
| Payment delay data | Weekly (Treasury API) | When available via OCDS |

### 6.2 Normalisation and Enrichment

| Raw Data | Normalisation | Enrichment |
|----------|---------------|------------|
| Tender categories | Map to unified taxonomy | Add relevance score based on user criteria |
| Procuring entities | Normalize names, link to CSD IDs | Add payment history if available |
| Contract values | Convert to ZAR, handle ranges | Add category benchmarks |
| Deadlines | Normalize to UTC, user timezone display | Add working-day calculation |
| B-BBEE levels | Standardize 1-8 + NC | Calculate point value for 80/20 vs 90/10 |

### 6.3 Handling and Signalling Unreliable Data

| Unreliability Type | Detection | Signal | User Action |
|--------------------|-----------|--------|-------------|
| Outdated source | Last-fetch timestamp > 24h | Amber badge on affected data | Manual refresh available |
| Parsing error | Failed extraction | Placeholder + "View Source" link | User can view original |
| Conflicting values | Multiple sources disagree | Warning icon + both values shown | User selects preferred |
| Incomplete tender | Required fields missing | Incomplete indicator | Cannot add to pipeline until resolved |
| External system down | API error on fetch | Error state + last-known data | Retry + cache fallback |

### 6.4 Visual Rendering Stack

```
┌─────────────────────────────────────────────────────────────┐
│                     React + TypeScript                       │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                 Component Library                        ││
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐       ││
│  │  │ Charts  │ │ Gauges  │ │ Tables  │ │ Cards   │       ││
│  │  │(Recharts│ │(Custom) │ │(TanStack│ │(Custom) │       ││
│  │  │ or Nivo)│ │         │ │ Table)  │ │         │       ││
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘       ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    State Layer                           ││
│  │  Zustand (global) + React Query (server) + URL (filters)││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    API Layer                             ││
│  │  REST endpoints + WebSocket for real-time updates       ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

**Technology Selection Rationale**:
- **Recharts/Nivo**: React-native charting, good for standard charts
- **Custom SVG**: For specialized visuals (gauges, funnels, Gantt)
- **TanStack Table**: High-performance data tables with sorting/filtering
- **Zustand**: Lightweight global state for UI preferences
- **React Query**: Server state with caching and refetching
- **URL State**: Filters/selections persist in URL for shareability

---

## 7. Human Capital Requirements

### 7.1 Minimum Skill Stack

| Role | Skills Required | Responsibility |
|------|-----------------|----------------|
| **Visualization Engineer** | D3.js/SVG, Recharts/Nivo, data transformation, performance optimization | Build custom visuals, optimize rendering |
| **UX Systems Designer** | Information architecture, navigation design, state management patterns | Design navigation model, interaction patterns |
| **Frontend Engineer** | React/TypeScript, state management, API integration | Implement components, integrate data |
| **Product Designer** | Visual design, accessibility, design systems | Color semantics, typography, component styling |
| **Data Engineer** | ETL pipelines, data normalization, caching strategies | Ensure data quality and freshness |

### 7.2 Failure Modes When Skills Absent

| Missing Skill | Failure Mode | Manifestation |
|---------------|--------------|---------------|
| Visualization Engineering | Cosmetic charts over functional | "Pretty" dashboards that don't enable decisions |
| UX Systems Design | Fragmented navigation | Users get lost, can't find data, abandon workflow |
| Data Engineering | Stale/incorrect data | Users lose trust, make wrong decisions |
| Product Design | Inconsistent visual language | Confusion about meaning of colors, icons |
| Domain Expertise | Wrong metrics prioritized | Platform solves wrong problems |

### 7.3 Contrast with Policy-Led Outcomes

**Research Finding**: "Industry-first leaders (L2B) build relationship-heavy, human-researched platforms commanding premium trust. Tech-first leaders build automation-focused platforms requiring outcome validation." (Research B)

| Approach | Outcome | Example |
|----------|---------|---------|
| **Policy-Led** | Compliance-focused, minimal UX investment | eTenders Portal - functional but low decision utility |
| **Consulting-Led** | Process documentation, no product | Training programs without tools |
| **Tech-Led without Domain** | Feature-rich but wrong features | AI matching without outcome validation |
| **Visualization-First with Domain** | Decision-enabling, outcome-measurable | Target state for this skill |

---

## 8. Failure Modes and Safeguards

### 8.1 Where Visuals Can Mislead

| Misleading Pattern | Example | Safeguard |
|--------------------|---------|-----------|
| **Vanity metrics prominence** | "48,000 tenders in database" | Only show metrics that drive action |
| **False precision** | "Win rate: 33.7%" based on 3 bids | Show confidence intervals or sample size |
| **Cherry-picked time periods** | Showing only good months | Default to meaningful periods, show full range |
| **Causation from correlation** | "Subscribers win more" without control | Avoid causal claims; show correlation disclaimers |
| **Survivorship bias** | Only showing won tenders | Include losses in analytics |
| **Anchoring on estimates** | Payment delay "usually 30 days" | Show distribution, not just central tendency |

### 8.2 Where UX Navigation Can Create False Confidence

| False Confidence Pattern | Example | Safeguard |
|--------------------------|---------|-----------|
| **Checklist completion = readiness** | All boxes checked but bid weak | Add strength assessment before submit |
| **Clean dashboard = no problems** | Compliance ok but low win rate | Surface analytics warnings proactively |
| **AI recommendation = endorsement** | High match score but poor fit | Show reasoning, not just score |
| **Submission = completion** | Submitted but not confirmed | Require confirmation tracking |
| **Pipeline busy = productive** | Many bids, no wins | Show conversion metrics prominently |

### 8.3 Safeguards Embedded in Design

| Safeguard | Implementation |
|-----------|----------------|
| **Uncertainty signaling** | All estimates show confidence or sample size |
| **Source attribution** | Data shows origin and freshness |
| **Contradictory data surfacing** | Conflicting sources shown side-by-side |
| **Negative outcome tracking** | Losses and rejections visible in analytics |
| **Assumption documentation** | Calculations show methodology in tooltip |
| **Manual override capability** | AI suggestions can be dismissed with reason |
| **Outcome feedback loops** | System asks for win/loss confirmation to improve |

---

## 9. Implementation Scope

### 9.1 Phase 1: Foundation (MVP)
- Dashboard landing page with attention-prioritized layout
- Compliance status gauges with expiration timeline
- Tender list with relevance scoring
- Basic pipeline calendar
- Cost tracking input (manual)

### 9.2 Phase 2: Analytics
- Opportunity funnel visualization
- Cost-per-bid calculator with drill-down
- Payment risk heat map (when data available)
- Win/loss tracking with reason codes

### 9.3 Phase 3: Intelligence
- Competitive density indicators (when data available)
- Bid strength radar/spider chart
- Historical winner profile comparison
- AI-enhanced matching with outcome validation

---

## 10. Acceptance Criteria

### Functional
- [ ] Dashboard loads in <2 seconds with current user data
- [ ] Compliance gauges accurately reflect registration expiry dates
- [ ] Pipeline calendar shows all active tenders with correct deadlines
- [ ] Funnel visualization accurately calculates conversion rates
- [ ] Cost calculator correctly aggregates logged expenses

### Visual
- [ ] Color semantics consistently applied across all views
- [ ] Uncertainty/incomplete data visually distinct from confirmed data
- [ ] Attention hierarchy guides user to most urgent items first
- [ ] Mobile-responsive for Android app integration

### UX
- [ ] Navigation state persists across view changes
- [ ] Detail panels don't lose primary canvas context
- [ ] Expert keyboard shortcuts functional
- [ ] Novice paths clearly marked with guidance

---

## STOP - Awaiting Approval

This proposal defines the Tender Visualisation Skill as an integrated capability across data modeling, visual encoding, UI composition, and UX navigation.

**Before implementation**:
1. Review visual models for completeness and accuracy
2. Confirm priority order for implementation phases
3. Validate metrics align with user research
4. Approve navigation model and state management approach

**Questions for approval**:
1. Should Phase 1 include any Phase 2/3 elements?
2. Are there additional metrics from the research to prioritize?
3. Any visual models to add or remove?
4. Integration approach with existing Android/Web frontends?
