import { Box, Chip, Typography } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty';
import SyncIcon from '@mui/icons-material/Sync';
import { GlassCard } from '../common/GlassCard';
import type { Job, JobStatus, JobType } from '../../types';

interface JobHistoryProps {
  jobs: Job[];
  onJobSelect?: (job: Job) => void;
  selectedJobId?: string;
}

const getStatusIcon = (status: JobStatus) => {
  switch (status) {
    case 'completed':
      return <CheckCircleIcon sx={{ fontSize: 18, color: '#22C55E' }} />;
    case 'failed':
      return <ErrorIcon sx={{ fontSize: 18, color: '#EF4444' }} />;
    case 'running':
      return <SyncIcon sx={{ fontSize: 18, color: '#8B5CF6', animation: 'spin 2s linear infinite' }} />;
    default:
      return <HourglassEmptyIcon sx={{ fontSize: 18, color: '#64748B' }} />;
  }
};

const getJobTypeLabel = (type: JobType) => {
  switch (type) {
    case 'full_pipeline':
      return 'Full Pipeline';
    case 'discover':
      return 'Discovery';
    case 'download':
      return 'Download';
    case 'extract':
      return 'Extraction';
    case 'score':
      return 'Scoring';
    default:
      return type;
  }
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('en-ZA', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export const JobHistory = ({ jobs, onJobSelect, selectedJobId }: JobHistoryProps) => {
  return (
    <GlassCard>
      <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
        Job History
      </Typography>

      <Box
        sx={{
          maxHeight: 400,
          overflow: 'auto',
          '&::-webkit-scrollbar': {
            width: 8,
          },
          '&::-webkit-scrollbar-track': {
            background: 'rgba(255, 255, 255, 0.05)',
          },
          '&::-webkit-scrollbar-thumb': {
            background: 'rgba(255, 255, 255, 0.2)',
            borderRadius: 4,
          },
        }}
      >
        {jobs.length === 0 ? (
          <Typography
            variant="body2"
            sx={{ color: 'text.secondary', textAlign: 'center', py: 4 }}
          >
            No jobs yet. Run the pipeline to get started.
          </Typography>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {jobs.map((job) => (
              <Box
                key={job.id}
                onClick={() => onJobSelect?.(job)}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 2,
                  p: 2,
                  borderRadius: '12px',
                  background:
                    selectedJobId === job.id
                      ? 'rgba(139, 92, 246, 0.2)'
                      : 'rgba(255, 255, 255, 0.05)',
                  border:
                    selectedJobId === job.id
                      ? '1px solid rgba(139, 92, 246, 0.3)'
                      : '1px solid transparent',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  '&:hover': {
                    background: 'rgba(255, 255, 255, 0.08)',
                  },
                  '@keyframes spin': {
                    from: { transform: 'rotate(0deg)' },
                    to: { transform: 'rotate(360deg)' },
                  },
                }}
              >
                {getStatusIcon(job.status)}

                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography variant="body2" sx={{ fontWeight: 500 }}>
                    {getJobTypeLabel(job.job_type)}
                  </Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                    {formatDate(job.created_at)}
                    {job.source_id && ` • ${job.source_id}`}
                  </Typography>
                </Box>

                <Chip
                  label={job.status}
                  size="small"
                  sx={{
                    textTransform: 'capitalize',
                    backgroundColor:
                      job.status === 'completed'
                        ? 'rgba(34, 197, 94, 0.2)'
                        : job.status === 'failed'
                          ? 'rgba(239, 68, 68, 0.2)'
                          : job.status === 'running'
                            ? 'rgba(139, 92, 246, 0.2)'
                            : 'rgba(100, 116, 139, 0.2)',
                    color:
                      job.status === 'completed'
                        ? '#22C55E'
                        : job.status === 'failed'
                          ? '#EF4444'
                          : job.status === 'running'
                            ? '#8B5CF6'
                            : '#64748B',
                  }}
                />
              </Box>
            ))}
          </Box>
        )}
      </Box>
    </GlassCard>
  );
};
