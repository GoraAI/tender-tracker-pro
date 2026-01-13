import { Box, Step, StepLabel, Stepper, Typography } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import CloudDownloadIcon from '@mui/icons-material/CloudDownload';
import TextSnippetIcon from '@mui/icons-material/TextSnippet';
import StarIcon from '@mui/icons-material/Star';
import type { JobType, JobStatus } from '../../types';

interface PipelineStepsProps {
  currentStep?: JobType | null;
  status?: JobStatus;
}

const steps = [
  { key: 'discover', label: 'Discover', icon: SearchIcon, description: 'Find PDF links' },
  { key: 'download', label: 'Download', icon: CloudDownloadIcon, description: 'Download PDFs' },
  { key: 'extract', label: 'Extract', icon: TextSnippetIcon, description: 'Extract text' },
  { key: 'score', label: 'Score', icon: StarIcon, description: 'Score relevance' },
];

const getStepIndex = (step: JobType | null): number => {
  if (!step) return -1;
  if (step === 'full_pipeline') return 0;
  return steps.findIndex((s) => s.key === step);
};

export const PipelineSteps = ({ currentStep, status }: PipelineStepsProps) => {
  const activeStep = getStepIndex(currentStep ?? null);

  return (
    <Box sx={{ width: '100%', mb: 4 }}>
      <Stepper activeStep={activeStep} alternativeLabel>
        {steps.map((step, index) => {
          const Icon = step.icon;
          const isActive = index === activeStep;
          const isCompleted = index < activeStep;
          const isFailed = isActive && status === 'failed';

          return (
            <Step key={step.key} completed={isCompleted}>
              <StepLabel
                StepIconComponent={() => (
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: '50%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      background: isCompleted
                        ? 'linear-gradient(135deg, #22C55E 0%, #16A34A 100%)'
                        : isActive
                          ? isFailed
                            ? 'linear-gradient(135deg, #EF4444 0%, #DC2626 100%)'
                            : 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)'
                          : 'rgba(255, 255, 255, 0.1)',
                      border: isActive
                        ? 'none'
                        : '1px solid rgba(255, 255, 255, 0.2)',
                      transition: 'all 0.3s ease',
                      animation: isActive && status === 'running' ? 'pulse 2s infinite' : 'none',
                      '@keyframes pulse': {
                        '0%': { boxShadow: '0 0 0 0 rgba(139, 92, 246, 0.4)' },
                        '70%': { boxShadow: '0 0 0 10px rgba(139, 92, 246, 0)' },
                        '100%': { boxShadow: '0 0 0 0 rgba(139, 92, 246, 0)' },
                      },
                    }}
                  >
                    <Icon
                      sx={{
                        fontSize: 24,
                        color: isCompleted || isActive ? 'white' : 'text.secondary',
                      }}
                    />
                  </Box>
                )}
              >
                <Typography
                  variant="body2"
                  sx={{
                    fontWeight: isActive ? 600 : 400,
                    color: isActive ? 'text.primary' : 'text.secondary',
                  }}
                >
                  {step.label}
                </Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                  {step.description}
                </Typography>
              </StepLabel>
            </Step>
          );
        })}
      </Stepper>
    </Box>
  );
};
