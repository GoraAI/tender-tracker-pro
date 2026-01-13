import { useState, useEffect } from 'react';
import { Box, Typography } from '@mui/material';
import Grid from '@mui/material/Grid';
import { PipelineControl } from '../components/pipeline/PipelineControl';
import { JobHistory } from '../components/pipeline/JobHistory';
import { JobLogs } from '../components/pipeline/JobLogs';
import { usePipeline, useJob } from '../hooks/usePipeline';
import { useWebSocket } from '../hooks/useWebSocket';
import type { Job } from '../types';

export const PipelinePage = () => {
  const { jobs, refetchJobs } = usePipeline();
  const [selectedJob, setSelectedJob] = useState<Job | null>(null);
  const [activeJob, setActiveJob] = useState<Job | null>(null);

  // Find any running job
  useEffect(() => {
    const running = jobs.find((j) => j.status === 'pending' || j.status === 'running');
    setActiveJob(running ?? null);
    if (running && !selectedJob) {
      setSelectedJob(running);
    }
  }, [jobs, selectedJob]);

  // Get detailed job info if selected
  const { data: jobDetail } = useJob(selectedJob?.id ?? '');

  // WebSocket for real-time updates
  const { logs: wsLogs, status: wsStatus } = useWebSocket(
    activeJob?.id ?? null,
    {
      onStatusUpdate: () => {
        refetchJobs();
      },
      onComplete: () => {
        refetchJobs();
      },
    }
  );

  const handleJobSelect = (job: Job) => {
    setSelectedJob(job);
  };

  const handleJobStart = (job: Job) => {
    setSelectedJob(job);
    setActiveJob(job);
  };

  // Combine logs from API and WebSocket
  const displayLogs = selectedJob?.id === activeJob?.id
    ? wsLogs
    : jobDetail?.logs?.map((l) => ({
        id: l.id,
        level: l.level,
        message: l.message,
        timestamp: l.created_at,
      })) ?? [];

  // Get current job status from WebSocket or API - ensure full Job type
  const currentJob: Job | null = selectedJob && wsStatus && selectedJob.id === activeJob?.id
    ? {
        ...selectedJob,
        status: wsStatus.status,
        progress: wsStatus.progress,
        total_items: wsStatus.total_items,
        processed_items: wsStatus.processed_items,
        error_message: wsStatus.error_message,
      }
    : selectedJob;

  return (
    <Box className="page-transition">
      <Typography variant="h4" sx={{ mb: 4, fontWeight: 700 }}>
        Pipeline
      </Typography>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <PipelineControl
            activeJob={currentJob}
            onJobStart={handleJobStart}
          />

          <Box sx={{ mt: 3 }}>
            <JobLogs logs={displayLogs} maxHeight={350} />
          </Box>
        </Grid>

        <Grid size={{ xs: 12, lg: 4 }}>
          <JobHistory
            jobs={jobs}
            onJobSelect={handleJobSelect}
            selectedJobId={selectedJob?.id}
          />
        </Grid>
      </Grid>
    </Box>
  );
};
