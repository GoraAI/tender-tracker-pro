# Tasks: FR-WEB-003 Pipeline Control & Settings

## Phase 1: Backend API Extensions

- [x] **Task 1.1**: Create pipeline router with run endpoints
  - Acceptance: POST /pipeline/discover, /pipeline/download, /pipeline/extract, /pipeline/score, /pipeline/full
  - Files: src/api/routers/pipeline.py

- [x] **Task 1.2**: Create pipeline service for job orchestration
  - Acceptance: Jobs created, background tasks launched
  - Files: src/api/services/pipeline_service.py

- [x] **Task 1.3**: Add WebSocket endpoint for job streaming
  - Acceptance: Real-time log messages via WebSocket
  - Files: src/api/websocket/job_stream.py

- [x] **Task 1.4**: Create config router for criteria CRUD
  - Acceptance: GET/POST/PUT/DELETE /config/criteria endpoints
  - Files: src/api/routers/config.py

- [x] **Task 1.5**: Add source list and toggle endpoints
  - Acceptance: GET /config/sources, PUT /config/sources/{id}/toggle
  - Files: src/api/routers/config.py (update)

## Phase 2: Frontend API & Hooks

- [x] **Task 2.1**: Create pipeline API functions
  - Acceptance: runPipeline(), runStep(), getJobs(), getJob()
  - Files: frontend/src/api/pipeline.ts

- [x] **Task 2.2**: Create config API functions
  - Acceptance: getCriteria(), createCriteria(), updateCriteria(), deleteCriteria()
  - Files: frontend/src/api/config.ts

- [x] **Task 2.3**: Create usePipeline hook
  - Acceptance: Access job state, run mutations
  - Files: frontend/src/hooks/usePipeline.ts

- [x] **Task 2.4**: Create useWebSocket hook
  - Acceptance: Connect to job stream, handle messages
  - Files: frontend/src/hooks/useWebSocket.ts

- [x] **Task 2.5**: Create useConfig hook
  - Acceptance: Access criteria/sources, CRUD mutations
  - Files: frontend/src/hooks/useConfig.ts

## Phase 3: Pipeline UI Components

- [x] **Task 3.1**: Create PipelineSteps component
  - Acceptance: 4-step indicator with status (pending/running/complete/error)
  - Files: frontend/src/components/pipeline/PipelineSteps.tsx

- [x] **Task 3.2**: Create JobProgress component
  - Acceptance: Progress bar with percentage, item count
  - Files: frontend/src/components/pipeline/JobProgress.tsx

- [x] **Task 3.3**: Create JobLogs component
  - Acceptance: Scrollable log viewer with level coloring
  - Files: frontend/src/components/pipeline/JobLogs.tsx

- [x] **Task 3.4**: Create PipelineControl component
  - Acceptance: Run buttons, step status, active job display
  - Files: frontend/src/components/pipeline/PipelineControl.tsx

- [x] **Task 3.5**: Create JobHistory component
  - Acceptance: List of recent jobs with status
  - Files: frontend/src/components/pipeline/JobHistory.tsx

## Phase 4: Settings UI Components

- [x] **Task 4.1**: Create CriteriaCard component
  - Acceptance: Display criteria with edit/delete actions
  - Files: frontend/src/components/settings/CriteriaCard.tsx

- [x] **Task 4.2**: Create CriteriaEditor component
  - Acceptance: Form for creating/editing criteria
  - Files: frontend/src/components/settings/CriteriaEditor.tsx

- [x] **Task 4.3**: Create SourceCard component
  - Acceptance: Source with active toggle switch
  - Files: frontend/src/components/settings/SourceCard.tsx

- [x] **Task 4.4**: Create SourceManager component
  - Acceptance: List of sources with toggle controls
  - Files: frontend/src/components/settings/SourceManager.tsx

## Phase 5: Pages

- [x] **Task 5.1**: Create PipelinePage
  - Acceptance: Pipeline control + job history
  - Files: frontend/src/pages/PipelinePage.tsx

- [x] **Task 5.2**: Create SettingsPage with tabs
  - Acceptance: Tabs for Criteria, Sources, Profile
  - Files: frontend/src/pages/SettingsPage.tsx

- [x] **Task 5.3**: Update routes with new pages
  - Acceptance: /pipeline and /settings routes added
  - Files: frontend/src/routes.tsx (update)

- [x] **Task 5.4**: Update Sidebar with new navigation
  - Acceptance: Pipeline and Settings links in nav
  - Files: frontend/src/components/layout/Sidebar.tsx (update)

## Phase 6: Integration

- [x] **Task 6.1**: Wire pipeline to existing backend
  - Acceptance: Pipeline triggers actual navigator/extractor
  - Files: src/api/services/pipeline_service.py (update)

- [ ] **Task 6.2**: Test end-to-end pipeline flow
  - Acceptance: Can discover, download, extract, score tenders
  - Files: Manual testing

- [ ] **Task 6.3**: Test criteria editing flow
  - Acceptance: Can create/edit/delete scoring criteria
  - Files: Manual testing

## Progress Tracking

| Phase | Tasks | Completed | Progress |
|-------|-------|-----------|----------|
| 1. Backend API | 5 | 5 | 100% |
| 2. Frontend API & Hooks | 5 | 5 | 100% |
| 3. Pipeline UI | 5 | 5 | 100% |
| 4. Settings UI | 4 | 4 | 100% |
| 5. Pages | 4 | 4 | 100% |
| 6. Integration | 3 | 1 | 33% |
| **Total** | **26** | **24** | **92%** |

## Completion Notes

FR-WEB-003 Pipeline & Settings is functionally complete. All code tasks are done.
Manual testing tasks deferred to integration testing phase.

**Build Status**: Passing (npm run build succeeds)
**New Routes**: /pipeline, /settings
**New Components**: 9 pipeline/settings components
