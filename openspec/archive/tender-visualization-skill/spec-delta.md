# Specification Delta: Tender Visualisation Skill

**Change ID**: `tender-visualization-skill`
**Status**: PENDING APPROVAL

---

## New Specifications Added

### Visual Models (6 new)

| ID | Name | Purpose |
|----|------|---------|
| VIS-001 | Opportunity Funnel | Visualize tender conversion from discovery to award |
| VIS-002 | Compliance Health Dashboard | Real-time registration status with expiration timeline |
| VIS-003 | Pipeline Calendar | Gantt-style timeline for parallel tender preparation |
| VIS-004 | Competitive Intelligence Radar | Bid strength self-assessment against criteria |
| VIS-005 | Payment Risk Heat Map | Geographic/categorical payment delay visualization |
| VIS-006 | Cost-Per-Bid Calculator | ROI analysis with drill-down breakdown |

### UI Components (10 new)

| ID | Component | Description |
|----|-----------|-------------|
| UI-001 | Gauge | Configurable radial gauge for compliance status |
| UI-002 | Timeline | Horizontal timeline for expiration/milestone display |
| UI-003 | Gantt Bar | Draggable timeline bar with phase segments |
| UI-004 | Funnel Chart | Vertical flow visualization with stage annotations |
| UI-005 | Heat Map | Color-coded geographic or categorical grid |
| UI-006 | Radar Chart | Multi-axis spider/radar for criteria comparison |
| UI-007 | Stacked Bar | Horizontal bar with clickable segments |
| UI-008 | Attention Card | Elevated card for urgent items |
| UI-009 | Detail Panel | Slide-in panel for context details |
| UI-010 | Nav Rail Badge | Status indicator on navigation items |

### UX Patterns (5 new)

| ID | Pattern | Description |
|----|---------|-------------|
| UX-001 | Attention-Prioritized Layout | Critical items surface first |
| UX-002 | Progressive Disclosure | 5-layer complexity model |
| UX-003 | Visual-to-Visual Navigation | Panel-based, not page-based |
| UX-004 | State Persistence | Filters persist across view changes |
| UX-005 | Expert Fast Paths | Keyboard shortcuts for power users |

### Data Models (4 new)

| ID | Model | Purpose |
|----|-------|---------|
| DM-001 | TenderCost | Log tender preparation expenses |
| DM-002 | TenderOutcome | Record win/loss with reason codes |
| DM-003 | ComplianceStatus | Track registration expiry dates |
| DM-004 | AnalyticsCache | Precomputed metrics for dashboard |

### API Endpoints (6 new)

| ID | Endpoint | Method | Purpose |
|----|----------|--------|---------|
| API-001 | `/api/v1/analytics/funnel` | GET | Opportunity funnel data |
| API-002 | `/api/v1/analytics/costs` | GET/POST | Cost tracking |
| API-003 | `/api/v1/analytics/outcomes` | GET/POST | Win/loss recording |
| API-004 | `/api/v1/compliance/status` | GET | User compliance summary |
| API-005 | `/api/v1/tenders/{id}/pipeline` | GET/PATCH | Pipeline status |
| API-006 | `/api/v1/analytics/payments` | GET | Payment delay data |

---

## Modified Specifications

### Tender Detail View
- Add: Relevance score visualization
- Add: Cost tracking tab
- Add: Bid strength assessment
- Add: Payment risk indicator

### Tender List View
- Add: Urgency color-coding
- Add: Relevance score column
- Add: Quick-action buttons
- Modify: Sort options to include score/deadline

### Dashboard
- Replace: Simple list with attention-prioritized layout
- Add: Compliance status summary widget
- Add: Pipeline summary widget
- Add: New opportunities carousel

### Navigation
- Modify: Add nav rail with Analytics section
- Add: Keyboard shortcut system
- Add: URL state persistence

---

## Unchanged Specifications

- Authentication flow
- Tender sync mechanism
- LLM scoring logic
- Source/criteria management
- Pipeline trigger functionality

---

## Design Rules Added

### Color Semantics
| Color | Hex | Meaning |
|-------|-----|---------|
| Critical | #DC2626 | Risk, urgent, failure |
| Warning | #EA580C | Attention needed |
| Caution | #CA8A04 | Review, partial |
| Success | #16A34A | Valid, complete, won |
| Information | #2563EB | Interactive, new |
| Neutral | #6B7280 | Disabled, historical |

### Uncertainty Encoding
| State | Visual |
|-------|--------|
| Missing | Dashed outline, "—" |
| Estimated | Tilde prefix, italic |
| Stale | Timestamp, amber outline |
| AI-inferred | "AI" badge |

---

## Metrics Tracked

| Metric | Source | Aggregation |
|--------|--------|-------------|
| Win Rate | Tender outcomes | Count won / count submitted |
| Cost Per Win | Cost logs | Sum costs / count won |
| Compliance Health | Registrations | Min days to expiry |
| Pipeline Utilization | Active tenders | Active / capacity |
| Conversion Rate | Tender stages | Stage N+1 / Stage N |

---

## Integration Points

### With Existing Systems
- Tender sync: Add outcome recording
- LLM scoring: Surface in bid strength radar
- User management: Store compliance data
- Notifications: Compliance reminders

### With External Systems
- Treasury API: Payment delay data (when available)
- CSD/CIDB: Registration verification links
- SARS eFiling: Tax compliance links

---

**Status**: PENDING APPROVAL

These specifications will be merged to `openspec/specs/` after implementation and validation.
