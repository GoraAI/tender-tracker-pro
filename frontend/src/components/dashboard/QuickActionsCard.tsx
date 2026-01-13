import { Box, Button, Typography } from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import AddIcon from '@mui/icons-material/Add';
import TuneIcon from '@mui/icons-material/Tune';
import { GlassCard } from '../common/GlassCard';

export const QuickActionsCard = () => {
  return (
    <GlassCard sx={{ height: '100%' }}>
      <Typography variant="h6" sx={{ mb: 2 }}>
        Quick Actions
      </Typography>

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <Button
          variant="contained"
          startIcon={<PlayArrowIcon />}
          disabled
          fullWidth
          sx={{
            justifyContent: 'flex-start',
            py: 1.5,
          }}
        >
          Run Pipeline (Soon)
        </Button>

        <Button
          variant="outlined"
          startIcon={<AddIcon />}
          disabled
          fullWidth
          sx={{
            justifyContent: 'flex-start',
            py: 1.5,
          }}
        >
          Add Source (Soon)
        </Button>

        <Button
          variant="outlined"
          startIcon={<TuneIcon />}
          disabled
          fullWidth
          sx={{
            justifyContent: 'flex-start',
            py: 1.5,
          }}
        >
          Edit Criteria (Soon)
        </Button>
      </Box>

      <Typography variant="caption" sx={{ display: 'block', mt: 2, color: 'text.secondary' }}>
        Pipeline controls coming in FR-WEB-003
      </Typography>
    </GlassCard>
  );
};
