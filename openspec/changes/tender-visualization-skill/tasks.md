# Implementation Tasks: Tender Visualisation Skill

**Change ID**: `tender-visualization-skill`
**Status**: IMPLEMENTED

---

## Phase 1: Foundation (MVP)

### 1.1 Dashboard Infrastructure
- [ ] Create dashboard layout component with attention-prioritized zones
- [ ] Implement nav rail with badge support for compliance status
- [ ] Build detail panel slide-in/out mechanism
- [ ] Set up URL-based state persistence for filters
- [ ] Implement keyboard navigation shortcuts

### 1.2 Compliance Status Gauges
- [ ] Create gauge component with configurable thresholds
- [ ] Build compliance dashboard with 5-6 registration gauges
- [ ] Implement expiration timeline visualization
- [ ] Add "Renew" action buttons with external links
- [ ] Connect to user compliance data from backend

### 1.3 Tender List Enhancement
- [ ] Add relevance score column with visual bar
- [ ] Implement urgency color-coding on deadline
- [ ] Add competitive density indicator (placeholder for future data)
- [ ] Create tender card component with action buttons
- [ ] Implement filter persistence across navigation

### 1.4 Pipeline Calendar
- [ ] Build Gantt-style timeline component
- [ ] Implement tender bar with phase segments
- [ ] Add deadline conflict detection and warning
- [ ] Create drag-to-reschedule interaction
- [ ] Integrate with existing tender sync

### 1.5 Cost Tracking Input
- [ ] Create cost logging form (manual entry)
- [ ] Build cost category taxonomy (platform, compliance, preparation, submission)
- [ ] Implement backend storage for tender costs
- [ ] Add cost summary display on tender detail

---

## Phase 2: Analytics

### 2.1 Opportunity Funnel
- [ ] Create Sankey/funnel visualization component
- [ ] Implement stage filtering interaction
- [ ] Calculate conversion rates from tender status history
- [ ] Add period selector with animation
- [ ] Display drop-off annotations

### 2.2 Cost-Per-Bid Calculator
- [ ] Build stacked bar chart with drill-down
- [ ] Implement cost aggregation logic
- [ ] Create break-even calculator
- [ ] Add period comparison view
- [ ] Display cost trend over time

### 2.3 Payment Risk Heat Map
- [ ] Create geographic/categorical heat map component
- [ ] Source payment delay data (Treasury API when available)
- [ ] Implement cash flow impact calculator
- [ ] Add department/province toggle
- [ ] Connect to tender selection for context

### 2.4 Win/Loss Tracking
- [ ] Add win/loss outcome recording to tender
- [ ] Implement reason code taxonomy
- [ ] Build outcome analytics dashboard
- [ ] Create debrief request generator
- [ ] Calculate historical win rate by category

---

## Phase 3: Intelligence

### 3.1 Competitive Intelligence
- [ ] Research data sources for bidder counts
- [ ] Implement competitive density estimation model
- [ ] Display on tender cards and detail views
- [ ] Build similar tender outcome comparison

### 3.2 Bid Strength Assessment
- [ ] Create radar/spider chart component
- [ ] Define evaluation criteria axes
- [ ] Implement self-assessment interaction
- [ ] Add winner profile overlay (when data available)
- [ ] Generate improvement suggestions

### 3.3 AI-Enhanced Matching
- [ ] Integrate with existing LLM scoring
- [ ] Add outcome tracking feedback loop
- [ ] Validate AI recommendations against outcomes
- [ ] Build confidence display for recommendations
- [ ] Implement dismissal with reason tracking

---

## Technical Infrastructure

### Data Layer
- [ ] Create analytics aggregation endpoints
- [ ] Implement cost tracking database models
- [ ] Build outcome recording and retrieval
- [ ] Add compliance reminder scheduling
- [ ] Create payment delay data cache

### UI Component Library
- [ ] Gauge component with configurable semantics
- [ ] Timeline/Gantt component
- [ ] Funnel/Sankey component
- [ ] Heat map component
- [ ] Radar/spider chart component
- [ ] Stacked bar with drill-down

### State Management
- [ ] URL state persistence for filters
- [ ] Cross-view selection state
- [ ] Panel open/close state
- [ ] User preference persistence

---

## Dependencies

| Task | Depends On |
|------|------------|
| Compliance gauges | User compliance data entry |
| Funnel visualization | Tender status history |
| Cost calculator | Cost logging implementation |
| Payment heat map | Treasury API access (external) |
| Competitive density | Bid count data (external or estimated) |
| Win rate analytics | Outcome recording adoption |

---

## Estimated Effort

| Phase | Components | Complexity |
|-------|------------|------------|
| Phase 1 | 5 major features | Medium - foundational UI work |
| Phase 2 | 4 analytics views | High - visualization engineering |
| Phase 3 | 3 intelligence features | High - data dependencies |

---

**Status**: PENDING APPROVAL

Tasks will be marked in-progress and completed as implementation proceeds after proposal approval.
