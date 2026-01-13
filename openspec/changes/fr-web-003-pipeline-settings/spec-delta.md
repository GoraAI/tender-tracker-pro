# Spec Delta: FR-WEB-003 Pipeline Control & Settings

## ADDED

### src/api/routers/pipeline.py
```python
"""
Pipeline router for running tender discovery/extraction pipeline.

Endpoints:
- POST /pipeline/discover - Start PDF discovery job
- POST /pipeline/download - Start PDF download job
- POST /pipeline/extract - Start text extraction job
- POST /pipeline/score - Start relevance scoring job
- POST /pipeline/full - Run full pipeline
- GET /pipeline/jobs - List user's jobs
- GET /pipeline/jobs/{id} - Get job details with logs
"""
```

### src/api/routers/config.py
```python
"""
Config router for user scoring criteria and sources.

Endpoints:
- GET /config/criteria - List user's scoring criteria
- POST /config/criteria - Create new criteria
- PUT /config/criteria/{id} - Update criteria
- DELETE /config/criteria/{id} - Delete criteria
- GET /config/sources - List tender sources
- PUT /config/sources/{id}/toggle - Toggle source active state
"""
```

### src/api/websocket/job_stream.py
```python
"""
WebSocket endpoint for real-time job log streaming.

- Connect: /ws/pipeline/jobs/{job_id}/stream
- Receives: JobLogMessage with level, message, timestamp
- Handles reconnection and backpressure
"""
```

### src/api/services/pipeline_service.py
```python
"""
Pipeline orchestration service.

Functions:
- create_job(user_id, job_type, source_id) -> Job
- run_discover(job_id) - Background task for discovery
- run_download(job_id) - Background task for downloads
- run_extract(job_id) - Background task for extraction
- run_score(job_id) - Background task for scoring
- run_full_pipeline(job_id) - Orchestrate all steps
- get_job_logs(job_id) -> List[JobLog]
- stream_logs(job_id) - Generator for WebSocket
"""
```

### frontend/src/api/pipeline.ts
```typescript
export const pipelineApi = {
  runDiscover: (sourceId?: string) => Promise<Job>,
  runDownload: (sourceId?: string) => Promise<Job>,
  runExtract: (sourceId?: string) => Promise<Job>,
  runScore: (sourceId?: string) => Promise<Job>,
  runFull: (sourceId?: string) => Promise<Job>,
  getJobs: () => Promise<Job[]>,
  getJob: (id: string) => Promise<JobDetail>,
};
```

### frontend/src/api/config.ts
```typescript
export const configApi = {
  getCriteria: () => Promise<Criteria[]>,
  createCriteria: (data: CriteriaCreate) => Promise<Criteria>,
  updateCriteria: (id: string, data: CriteriaUpdate) => Promise<Criteria>,
  deleteCriteria: (id: string) => Promise<void>,
  getSources: () => Promise<Source[]>,
  toggleSource: (id: string) => Promise<Source>,
};
```

### frontend/src/hooks/useWebSocket.ts
```typescript
export const useWebSocket = (url: string) => {
  // State: connected, messages, error
  // Methods: connect(), disconnect(), send()
  // Auto-reconnect on disconnect
};
```

### frontend/src/components/pipeline/PipelineSteps.tsx
```typescript
/**
 * Visual step indicator for pipeline phases.
 * Shows: Discover → Download → Extract → Score
 * States: pending | running | complete | error
 */
```

### frontend/src/components/pipeline/JobProgress.tsx
```typescript
/**
 * Progress bar with:
 * - Percentage complete
 * - Items processed / total
 * - Current item name
 * - Elapsed time
 */
```

### frontend/src/components/pipeline/JobLogs.tsx
```typescript
/**
 * Real-time log viewer:
 * - Auto-scroll to bottom
 * - Level-based coloring (info=white, warn=yellow, error=red)
 * - Timestamp prefix
 * - Copy button
 */
```

### frontend/src/components/settings/CriteriaEditor.tsx
```typescript
/**
 * Form for scoring criteria:
 * - Name input
 * - Capabilities (tag input)
 * - Preferred sectors (tag input)
 * - Preferred locations (tag input)
 * - Value range (min/max sliders)
 * - Keywords (tag input)
 * - Save/Cancel buttons
 */
```

### frontend/src/pages/PipelinePage.tsx
```typescript
/**
 * Pipeline control page:
 * - Source selector dropdown
 * - 4-step pipeline indicator
 * - Run Full Pipeline button
 * - Individual step buttons
 * - Active job progress (if running)
 * - Job history list
 */
```

### frontend/src/pages/SettingsPage.tsx
```typescript
/**
 * Settings page with tabs:
 * - Scoring Criteria tab (CriteriaEditor)
 * - Sources tab (SourceManager)
 * - Profile tab (placeholder)
 */
```

## MODIFIED

### frontend/src/routes.tsx
```diff
+ import { PipelinePage } from './pages/PipelinePage';
+ import { SettingsPage } from './pages/SettingsPage';

  children: [
    // ... existing routes
+   {
+     path: 'pipeline',
+     element: <PipelinePage />,
+   },
+   {
+     path: 'settings',
+     element: <SettingsPage />,
+   },
  ],
```

### frontend/src/components/layout/Sidebar.tsx
```diff
  const menuItems = [
    { text: 'Dashboard', icon: DashboardIcon, path: '/dashboard' },
    { text: 'Tenders', icon: DescriptionIcon, path: '/tenders' },
+   { text: 'Pipeline', icon: PlayArrowIcon, path: '/pipeline' },
+   { text: 'Settings', icon: SettingsIcon, path: '/settings' },
  ];
```

### src/api/main.py
```diff
+ from .routers import pipeline, config
+ from .websocket import job_stream

  app.include_router(auth.router, prefix="/api/v1/auth", tags=["auth"])
  app.include_router(tenders.router, prefix="/api/v1/tenders", tags=["tenders"])
  app.include_router(dashboard.router, prefix="/api/v1/dashboard", tags=["dashboard"])
+ app.include_router(pipeline.router, prefix="/api/v1/pipeline", tags=["pipeline"])
+ app.include_router(config.router, prefix="/api/v1/config", tags=["config"])

+ # WebSocket endpoint
+ app.add_api_websocket_route("/ws/pipeline/jobs/{job_id}/stream", job_stream.websocket_endpoint)
```

## REMOVED

None

## Types Added

### frontend/src/types/index.ts
```typescript
// Add to existing types

export interface Job {
  id: string;
  job_type: 'discover' | 'download' | 'extract' | 'score' | 'full_pipeline';
  source_id: string | null;
  status: 'pending' | 'running' | 'completed' | 'failed';
  progress: number;
  total_items: number | null;
  processed_items: number;
  error_message: string | null;
  started_at: string | null;
  completed_at: string | null;
  created_at: string;
}

export interface JobLog {
  id: string;
  job_id: string;
  level: 'debug' | 'info' | 'warning' | 'error';
  message: string;
  metadata: Record<string, unknown>;
  created_at: string;
}

export interface Criteria {
  id: string;
  name: string;
  capabilities: string[];
  preferred_sectors: string[];
  preferred_locations: string[];
  value_range: { min: number; max: number | null };
  keywords: string[];
  weights: Record<string, number>;
  is_default: boolean;
}

export interface Source {
  id: string;
  name: string;
  url: string;
  is_active: boolean;
  last_scraped_at: string | null;
}
```
