# Tasks: FR-WEB-002 React Frontend Core

## Phase 1: Project Setup

- [x] **Task 1.1**: Initialize Vite + React + TypeScript project
  - Acceptance: `npm create vite@latest frontend -- --template react-ts` works
  - Files: frontend/package.json, frontend/tsconfig.json, frontend/vite.config.ts

- [x] **Task 1.2**: Install Material UI and dependencies
  - Acceptance: MUI components render correctly
  - Files: frontend/package.json (updated)

- [x] **Task 1.3**: Install React Query and Axios
  - Acceptance: QueryClient configured, Axios instance created
  - Files: frontend/src/api/client.ts

- [x] **Task 1.4**: Install React Router
  - Acceptance: Routes defined and working
  - Files: frontend/src/routes.tsx

- [x] **Task 1.5**: Install Zustand for state management
  - Acceptance: Auth store created
  - Files: frontend/src/store/authStore.ts

## Phase 2: Theme & Layout

- [x] **Task 2.1**: Create glassmorphism MUI theme
  - Acceptance: Dark mode with purple/pink accents, glass effects
  - Files: frontend/src/theme/glassmorphism.ts

- [x] **Task 2.2**: Create GlassCard component
  - Acceptance: Frosted glass card with blur effect
  - Files: frontend/src/components/common/GlassCard.tsx

- [x] **Task 2.3**: Create AppLayout with sidebar
  - Acceptance: Persistent sidebar, header with user menu
  - Files: frontend/src/components/layout/AppLayout.tsx

- [x] **Task 2.4**: Create Sidebar navigation
  - Acceptance: Links to Dashboard, Tenders, Settings
  - Files: frontend/src/components/layout/Sidebar.tsx

- [x] **Task 2.5**: Create Header with user menu
  - Acceptance: Shows user name, logout button
  - Files: frontend/src/components/layout/Header.tsx

## Phase 3: API Integration

- [x] **Task 3.1**: Create Axios client with auth interceptor
  - Acceptance: Automatically adds JWT to requests
  - Files: frontend/src/api/client.ts

- [x] **Task 3.2**: Create auth API functions
  - Acceptance: login(), register(), getMe() work
  - Files: frontend/src/api/auth.ts

- [x] **Task 3.3**: Create tenders API functions
  - Acceptance: getTenders(), getTender() with filters
  - Files: frontend/src/api/tenders.ts

- [x] **Task 3.4**: Create dashboard API functions
  - Acceptance: getStats(), getRecentTenders() work
  - Files: frontend/src/api/dashboard.ts

- [x] **Task 3.5**: Create React Query hooks
  - Acceptance: useTenders(), useDashboard() hooks
  - Files: frontend/src/hooks/useTenders.ts, frontend/src/hooks/useDashboard.ts

## Phase 4: Authentication

- [x] **Task 4.1**: Create auth Zustand store
  - Acceptance: token, user, login(), logout() actions
  - Files: frontend/src/store/authStore.ts

- [x] **Task 4.2**: Create useAuth hook
  - Acceptance: Access auth state and actions
  - Files: frontend/src/hooks/useAuth.ts

- [x] **Task 4.3**: Create LoginPage
  - Acceptance: Email/password form, error handling
  - Files: frontend/src/pages/LoginPage.tsx

- [x] **Task 4.4**: Create RegisterPage
  - Acceptance: Registration form with validation
  - Files: frontend/src/pages/RegisterPage.tsx

- [x] **Task 4.5**: Create ProtectedRoute component
  - Acceptance: Redirects to /login if not authenticated
  - Files: frontend/src/components/auth/ProtectedRoute.tsx

## Phase 5: Dashboard

- [x] **Task 5.1**: Create StatWidget component
  - Acceptance: Glass card with icon, value, label
  - Files: frontend/src/components/common/StatWidget.tsx

- [x] **Task 5.2**: Create DashboardPage
  - Acceptance: 4 stat widgets in grid layout
  - Files: frontend/src/pages/DashboardPage.tsx

- [x] **Task 5.3**: Create RecentTendersCard
  - Acceptance: List of top 5 tenders with scores
  - Files: frontend/src/components/dashboard/RecentTendersCard.tsx

- [x] **Task 5.4**: Create QuickActionsCard
  - Acceptance: Buttons for common actions
  - Files: frontend/src/components/dashboard/QuickActionsCard.tsx

## Phase 6: Tender Views

- [x] **Task 6.1**: Create ScoreBadge component
  - Acceptance: Colored badge based on score category
  - Files: frontend/src/components/common/ScoreBadge.tsx

- [x] **Task 6.2**: Create TenderCard component
  - Acceptance: Card with title, reference, deadline, score
  - Files: frontend/src/components/tenders/TenderCard.tsx

- [x] **Task 6.3**: Create TenderFilters component
  - Acceptance: Search, score filter, source filter
  - Files: frontend/src/components/tenders/TenderFilters.tsx

- [x] **Task 6.4**: Create TendersPage
  - Acceptance: Filter bar + paginated tender list
  - Files: frontend/src/pages/TendersPage.tsx

- [x] **Task 6.5**: Create TenderDetailPage
  - Acceptance: Full tender info with score explanation
  - Files: frontend/src/pages/TenderDetailPage.tsx

## Phase 7: App Assembly

- [x] **Task 7.1**: Create routes configuration
  - Acceptance: All routes defined with protection
  - Files: frontend/src/routes.tsx

- [x] **Task 7.2**: Create App.tsx with providers
  - Acceptance: Theme, QueryClient, Router wrapped
  - Files: frontend/src/App.tsx

- [x] **Task 7.3**: Update main.tsx entry point
  - Acceptance: App renders correctly
  - Files: frontend/src/main.tsx

- [x] **Task 7.4**: Configure Vite proxy for API
  - Acceptance: /api/* proxied to localhost:8000
  - Files: frontend/vite.config.ts

## Phase 8: Polish

- [x] **Task 8.1**: Add loading spinners
  - Acceptance: LoadingSpinner component used throughout
  - Files: frontend/src/components/common/LoadingSpinner.tsx

- [x] **Task 8.2**: Add error handling
  - Acceptance: Error boundaries, toast notifications
  - Files: frontend/src/components/common/ErrorBoundary.tsx

- [x] **Task 8.3**: Add responsive styles
  - Acceptance: Mobile-friendly sidebar, cards
  - Files: Various component updates

- [x] **Task 8.4**: Add page transitions
  - Acceptance: Smooth fade transitions between pages
  - Files: frontend/src/App.tsx (update)

## Documentation

- [ ] **Doc 1**: Create frontend README
  - Files: frontend/README.md

- [ ] **Doc 2**: Update root README with frontend instructions
  - Files: README.md

## Progress Tracking

| Phase | Tasks | Completed | Progress |
|-------|-------|-----------|----------|
| 1. Setup | 5 | 5 | 100% |
| 2. Theme & Layout | 5 | 5 | 100% |
| 3. API Integration | 5 | 5 | 100% |
| 4. Authentication | 5 | 5 | 100% |
| 5. Dashboard | 4 | 4 | 100% |
| 6. Tender Views | 5 | 5 | 100% |
| 7. App Assembly | 4 | 4 | 100% |
| 8. Polish | 4 | 4 | 100% |
| Documentation | 2 | 0 | 0% |
| **Total** | **39** | **37** | **95%** |

## Completion Notes

FR-WEB-002 React Frontend Core is functionally complete. All code tasks are done.
Documentation tasks (README files) deferred to project cleanup phase.

**Build Status**: Passing (npm run build succeeds)
**Dev Server**: Working (npm run dev on port 5173)
