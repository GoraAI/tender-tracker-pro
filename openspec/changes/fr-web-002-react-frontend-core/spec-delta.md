# Spec Delta: FR-WEB-002 React Frontend Core

## ADDED

### frontend-structure.md

```markdown
# Frontend Project Structure

## Overview
React 18 + TypeScript + Vite frontend with Material UI and glassmorphism design.

## Directory Structure

frontend/
├── public/
│   └── favicon.ico
├── src/
│   ├── api/
│   │   ├── client.ts       # Axios instance with auth
│   │   ├── auth.ts         # Auth API functions
│   │   ├── tenders.ts      # Tender API functions
│   │   └── dashboard.ts    # Dashboard API functions
│   │
│   ├── components/
│   │   ├── auth/
│   │   │   └── ProtectedRoute.tsx
│   │   ├── common/
│   │   │   ├── GlassCard.tsx
│   │   │   ├── StatWidget.tsx
│   │   │   ├── ScoreBadge.tsx
│   │   │   ├── LoadingSpinner.tsx
│   │   │   └── ErrorBoundary.tsx
│   │   ├── dashboard/
│   │   │   ├── RecentTendersCard.tsx
│   │   │   └── QuickActionsCard.tsx
│   │   ├── layout/
│   │   │   ├── AppLayout.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   └── Header.tsx
│   │   └── tenders/
│   │       ├── TenderCard.tsx
│   │       └── TenderFilters.tsx
│   │
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   ├── useTenders.ts
│   │   └── useDashboard.ts
│   │
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   ├── DashboardPage.tsx
│   │   ├── TendersPage.tsx
│   │   └── TenderDetailPage.tsx
│   │
│   ├── store/
│   │   └── authStore.ts
│   │
│   ├── theme/
│   │   └── glassmorphism.ts
│   │
│   ├── types/
│   │   └── index.ts
│   │
│   ├── App.tsx
│   ├── main.tsx
│   └── routes.tsx
│
├── index.html
├── package.json
├── tsconfig.json
├── tsconfig.node.json
└── vite.config.ts
```

### frontend-theme.md

```markdown
# Glassmorphism Theme Specification

## Color Palette

| Color | Hex | Usage |
|-------|-----|-------|
| Primary | #8B5CF6 | Buttons, links, accents |
| Primary Light | #A78BFA | Hover states |
| Primary Dark | #7C3AED | Active states |
| Secondary | #EC4899 | Badges, highlights |
| Background | #0F172A | Page background |
| Paper | rgba(30, 41, 59, 0.7) | Card backgrounds |
| Text Primary | #F8FAFC | Main text |
| Text Secondary | #94A3B8 | Secondary text |

## Glass Effect CSS

```css
.glass-card {
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}
```

## Score Badge Colors

| Category | Score Range | Color |
|----------|-------------|-------|
| Excellent | 90-100 | #22C55E (green) |
| Good | 70-89 | #3B82F6 (blue) |
| Moderate | 50-69 | #F59E0B (amber) |
| Weak | 30-49 | #F97316 (orange) |
| Poor | 0-29 | #EF4444 (red) |
```

### frontend-api.md

```markdown
# Frontend API Integration

## Axios Client Configuration

```typescript
const client = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth token to requests
client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

## API Functions

### Auth
- `login(email, password)` → `{ access_token, token_type }`
- `register(data)` → `{ access_token, token_type }`
- `getMe()` → `User`

### Tenders
- `getTenders(filters)` → `{ items, total, page, pages }`
- `getTender(id)` → `Tender`

### Dashboard
- `getStats()` → `DashboardStats`
- `getRecentTenders()` → `RecentTender[]`
```

### frontend-routes.md

```markdown
# Frontend Routes

| Path | Component | Auth Required |
|------|-----------|---------------|
| `/login` | LoginPage | No |
| `/register` | RegisterPage | No |
| `/` | Redirect to /dashboard | Yes |
| `/dashboard` | DashboardPage | Yes |
| `/tenders` | TendersPage | Yes |
| `/tenders/:id` | TenderDetailPage | Yes |
| `/settings` | SettingsPage | Yes (FR-WEB-003) |
| `/pipeline` | PipelinePage | Yes (FR-WEB-003) |
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
    api/
+ frontend/
+   src/
+     api/
+     components/
+     hooks/
+     pages/
+     store/
+     theme/
+   package.json
+   vite.config.ts
```

## REMOVED

- None
