import { Box, LinearProgress, Typography } from '@mui/material';
import type { Job } from '../../types';

interface JobProgressProps {
  job: Job;
}

export const JobProgress = ({ job }: JobProgressProps) => {
  const progress = job.progress || 0;
  const processed = job.processed_items || 0;
  const total = job.total_items;

  const getStatusColor = () => {
    switch (job.status) {
      case 'completed':
        return '#22C55E';
      case 'failed':
        return '#EF4444';
      case 'running':
        return '#8B5CF6';
      default:
        return '#64748B';
    }
  };

  const getStatusText = () => {
    switch (job.status) {
      case 'pending':
        return 'Waiting to start...';
      case 'running':
        return total ? `Processing ${processed} of ${total} items` : 'Processing...';
      case 'completed':
        return `Completed: ${processed} items processed`;
      case 'failed':
        return job.error_message || 'Job failed';
      default:
        return 'Unknown status';
    }
  };

  const formatDuration = () => {
    if (!job.started_at) return '';
    const start = new Date(job.started_at).getTime();
    const end = job.completed_at
      ? new Date(job.completed_at).getTime()
      : Date.now();
    const seconds = Math.floor((end - start) / 1000);

    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}m ${remainingSeconds}s`;
  };

  return (
    <Box sx={{ width: '100%' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {getStatusText()}
        </Typography>
        <Box sx={{ display: 'flex', gap: 2 }}>
          {job.started_at && (
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              {formatDuration()}
            </Typography>
          )}
          <Typography
            variant="body2"
            sx={{ fontWeight: 600, color: getStatusColor() }}
          >
            {progress}%
          </Typography>
        </Box>
      </Box>

      <LinearProgress
        variant={job.status === 'running' && progress === 0 ? 'indeterminate' : 'determinate'}
        value={progress}
        sx={{
          height: 8,
          borderRadius: 4,
          backgroundColor: 'rgba(255, 255, 255, 0.1)',
          '& .MuiLinearProgress-bar': {
            borderRadius: 4,
            background:
              job.status === 'failed'
                ? 'linear-gradient(90deg, #EF4444 0%, #DC2626 100%)'
                : job.status === 'completed'
                  ? 'linear-gradient(90deg, #22C55E 0%, #16A34A 100%)'
                  : 'linear-gradient(90deg, #8B5CF6 0%, #EC4899 100%)',
          },
        }}
      />

      {job.status === 'failed' && job.error_message && (
        <Typography
          variant="caption"
          sx={{
            display: 'block',
            mt: 1,
            color: '#EF4444',
          }}
        >
          Error: {job.error_message}
        </Typography>
      )}
    </Box>
  );
};
