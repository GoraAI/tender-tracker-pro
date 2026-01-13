import { useState } from 'react';
import {
  Box,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Typography,
} from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import { GlassCard } from '../common/GlassCard';
import { PipelineSteps } from './PipelineSteps';
import { JobProgress } from './JobProgress';
import { usePipeline } from '../../hooks/usePipeline';
import { useSources } from '../../hooks/useConfig';
import type { Job, JobType } from '../../types';

interface PipelineControlProps {
  activeJob?: Job | null;
  onJobStart?: (job: Job) => void;
}

export const PipelineControl = ({ activeJob, onJobStart }: PipelineControlProps) => {
  const [selectedSource, setSelectedSource] = useState<string>('');
  const { sources, isLoading: loadingSources } = useSources();
  const { runFull, runDiscover, runDownload, runExtract, runScore, cancelJob, isRunning } =
    usePipeline();

  const activeSources = sources.filter((s) => s.is_active);
  const hasActiveJob = activeJob && (activeJob.status === 'pending' || activeJob.status === 'running');

  const handleRunFull = async () => {
    try {
      const job = await runFull(selectedSource || undefined);
      onJobStart?.(job);
    } catch (error) {
      console.error('Failed to start pipeline:', error);
    }
  };

  const handleRunStep = async (step: 'discover' | 'download' | 'extract' | 'score') => {
    const runners = { discover: runDiscover, download: runDownload, extract: runExtract, score: runScore };
    try {
      const job = await runners[step](selectedSource || undefined);
      onJobStart?.(job);
    } catch (error) {
      console.error(`Failed to start ${step}:`, error);
    }
  };

  const handleCancel = async () => {
    if (activeJob) {
      try {
        await cancelJob(activeJob.id);
      } catch (error) {
        console.error('Failed to cancel job:', error);
      }
    }
  };

  return (
    <GlassCard>
      <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
        Pipeline Control
      </Typography>

      <PipelineSteps
        currentStep={activeJob?.job_type as JobType}
        status={activeJob?.status}
      />

      {activeJob && (
        <Box sx={{ mb: 3 }}>
          <JobProgress job={activeJob} />
        </Box>
      )}

      <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 3 }}>
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel>Source</InputLabel>
          <Select
            value={selectedSource}
            onChange={(e) => setSelectedSource(e.target.value)}
            label="Source"
            disabled={hasActiveJob || loadingSources}
          >
            <MenuItem value="">All Sources</MenuItem>
            {activeSources.map((source) => (
              <MenuItem key={source.id} value={source.id}>
                {source.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        {hasActiveJob ? (
          <Button
            variant="outlined"
            color="error"
            startIcon={<StopIcon />}
            onClick={handleCancel}
          >
            Cancel Job
          </Button>
        ) : (
          <Button
            variant="contained"
            startIcon={<PlayArrowIcon />}
            onClick={handleRunFull}
            disabled={isRunning}
            sx={{
              background: 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)',
              '&:hover': {
                background: 'linear-gradient(135deg, #7C3AED 0%, #DB2777 100%)',
              },
            }}
          >
            Run Full Pipeline
          </Button>
        )}
      </Box>

      <Typography variant="subtitle2" sx={{ mb: 2, color: 'text.secondary' }}>
        Or run individual steps:
      </Typography>

      <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
        {(['discover', 'download', 'extract', 'score'] as const).map((step) => (
          <Button
            key={step}
            variant="outlined"
            size="small"
            onClick={() => handleRunStep(step)}
            disabled={hasActiveJob || isRunning}
            sx={{ textTransform: 'capitalize' }}
          >
            {step}
          </Button>
        ))}
      </Box>
    </GlassCard>
  );
};
