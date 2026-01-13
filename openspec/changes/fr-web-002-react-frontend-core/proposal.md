# Proposal: FR-WEB-002 React Frontend Core

## Summary

Build a React frontend with Material UI and glassmorphism design, including authentication pages, dashboard with statistics, and tender list/detail views.

## Motivation

FR-WEB-001 established the FastAPI backend with PostgreSQL database. Now we need a modern, visually appealing frontend that:
- Provides user authentication flow
- Displays dashboard with tender statistics
- Allows browsing and searching tenders
- Shows tender details with score information

The glassmorphism design creates a premium, modern aesthetic with frosted glass effects on a dark background.

## Scope

### Included
- Vite + React + TypeScript project setup
- Material UI with custom glassmorphism theme
- React Router for navigation
- React Query for data fetching
- Zustand for auth state
- Authentication pages (Login, Register)
- Dashboard with stat widgets
- Tender list with search/filter
- Tender detail view
- Protected routes

### NOT Included
- Pipeline control UI (FR-WEB-003)
- Settings/criteria editor (FR-WEB-003)
- WebSocket job streaming (FR-WEB-003)
- User profile editing
- Email verification

## Acceptance Criteria

- [ ] `npm run dev` starts frontend on localhost:5173
- [ ] Login page authenticates against API and stores JWT
- [ ] Register page creates account and auto-logs in
- [ ] Dashboard displays stat widgets with live data
- [ ] Tender list supports search, filtering, pagination
- [ ] Tender detail shows full info with score
- [ ] Protected routes redirect to login when unauthenticated
- [ ] Glassmorphism theme applied consistently
- [ ] Responsive design works on mobile/desktop

## Technical Approach

### Project Structure
```
frontend/
├── src/
│   ├── api/           # Axios client + API functions
│   ├── components/    # Reusable UI components
│   ├── hooks/         # Custom React hooks
│   ├── pages/         # Page components
│   ├── store/         # Zustand stores
│   ├── theme/         # MUI theme config
│   ├── App.tsx
│   ├── main.tsx
│   └── routes.tsx
├── package.json
├── vite.config.ts
└── tsconfig.json
```

### Glassmorphism Theme
```typescript
// Dark mode base with purple/pink accents
palette: {
  mode: 'dark',
  primary: { main: '#8B5CF6' },     // Purple
  secondary: { main: '#EC4899' },   // Pink
  background: {
    default: '#0F172A',              // Dark slate
    paper: 'rgba(30, 41, 59, 0.7)', // Glass
  }
}

// Glass card effect
MuiCard: {
  background: 'rgba(30, 41, 59, 0.6)',
  backdropFilter: 'blur(12px)',
  border: '1px solid rgba(255, 255, 255, 0.1)',
  borderRadius: '16px',
}
```

### State Management
- **React Query**: Server state (tenders, stats, user)
- **Zustand**: Client state (auth token, user info)

## Dependencies

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.21.0",
    "@mui/material": "^5.15.0",
    "@mui/icons-material": "^5.15.0",
    "@emotion/react": "^11.11.0",
    "@emotion/styled": "^11.11.0",
    "@tanstack/react-query": "^5.17.0",
    "axios": "^1.6.0",
    "zustand": "^4.4.0"
  },
  "devDependencies": {
    "typescript": "^5.3.0",
    "vite": "^5.0.0",
    "@types/react": "^18.2.0",
    "@vitejs/plugin-react": "^4.2.0"
  }
}
```

## References

- Approved plan: ~/.claude/plans/glistening-jumping-bunny.md
- FR-WEB-001 (completed): src/api/
- Related: FR-WEB-003 (Pipeline & Settings)
