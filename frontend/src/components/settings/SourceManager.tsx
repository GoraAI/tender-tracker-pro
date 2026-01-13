import { Box, Skeleton, Typography } from '@mui/material';
import { SourceCard } from './SourceCard';
import { useSources } from '../../hooks/useConfig';

export const SourceManager = () => {
  const { sources, isLoading, toggleSource, isToggling } = useSources();

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        {[1, 2].map((i) => (
          <Skeleton key={i} variant="rectangular" height={100} sx={{ borderRadius: 4 }} />
        ))}
      </Box>
    );
  }

  if (sources.length === 0) {
    return (
      <Box
        sx={{
          textAlign: 'center',
          py: 8,
          background: 'rgba(30, 41, 59, 0.4)',
          borderRadius: '16px',
          border: '1px solid rgba(255, 255, 255, 0.1)',
        }}
      >
        <Typography variant="h6" sx={{ color: 'text.secondary', mb: 1 }}>
          No sources configured
        </Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          Sources are configured in the backend. Contact your administrator to add new sources.
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {sources.map((source) => (
        <SourceCard
          key={source.id}
          source={source}
          onToggle={toggleSource}
          isToggling={isToggling}
        />
      ))}
    </Box>
  );
};
