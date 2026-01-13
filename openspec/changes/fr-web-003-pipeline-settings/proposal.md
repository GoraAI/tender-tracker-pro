# Proposal: FR-WEB-003 Pipeline Control & Settings

## Summary

Add Pipeline Control UI for running the tender discovery/extraction pipeline and Settings pages for managing user scoring criteria and tender sources.

## Motivation

The frontend core is complete (FR-WEB-002), but users cannot:
1. Run the pipeline to discover and process new tenders
2. Configure their scoring criteria for relevance matching
3. Manage which tender sources are active

This feature completes the web frontend by adding operational control.

## Scope

### Included
- Pipeline control page with step-by-step progress
- Real-time job status via WebSocket
- Job history and log viewing
- User scoring criteria CRUD editor
- Source management (enable/disable sources)
- Model status display (download/load state)
- Settings page navigation

### Not Included
- Model download/management UI (deferred)
- Advanced pipeline scheduling
- Multi-user job management

## Acceptance Criteria

- [ ] Pipeline page shows 4-step progress (Discover → Download → Extract → Score)
- [ ] Users can run full pipeline or individual steps
- [ ] Job progress updates in real-time via WebSocket
- [ ] Job logs are viewable in-page
- [ ] Settings page allows creating/editing scoring criteria
- [ ] Settings page allows toggling source active status
- [ ] All new pages accessible via sidebar navigation

## Technical Approach

### Backend Extensions (API)
- Add WebSocket endpoint for job streaming: `/api/v1/pipeline/jobs/{id}/stream`
- Add pipeline endpoints for individual steps
- Add criteria CRUD endpoints
- Add source list/toggle endpoints

### Frontend Components
- PipelineControl with step indicators
- JobProgress with progress bar
- JobLogs with real-time updates
- CriteriaEditor with JSON field support
- SourceManager with toggle switches
- Settings page layout

### State Management
- WebSocket hook for real-time job updates
- React Query for criteria/sources data
- Local state for pipeline step control

## References

- FR-WEB-001: Database models for jobs, job_logs, user_criteria, sources
- FR-WEB-002: React/MUI frontend foundation
- Architecture plan: /home/goraai/.claude/plans/glistening-jumping-bunny.md
