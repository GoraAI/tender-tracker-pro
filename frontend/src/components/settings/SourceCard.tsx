import { Box, Switch, Typography } from '@mui/material';
import LinkIcon from '@mui/icons-material/Link';
import { GlassCard } from '../common/GlassCard';
import type { Source } from '../../types';

interface SourceCardProps {
  source: Source;
  onToggle?: (id: string) => void;
  isToggling?: boolean;
}

export const SourceCard = ({ source, onToggle, isToggling }: SourceCardProps) => {
  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return 'Never';
    return new Date(dateStr).toLocaleDateString('en-ZA', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <GlassCard hover>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <Box sx={{ flex: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>
              {source.name}
            </Typography>
            <Box
              sx={{
                width: 8,
                height: 8,
                borderRadius: '50%',
                backgroundColor: source.is_active ? '#22C55E' : '#64748B',
              }}
            />
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 1 }}>
            <LinkIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
            <Typography
              variant="body2"
              sx={{
                color: 'text.secondary',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                maxWidth: 300,
              }}
            >
              {source.url}
            </Typography>
          </Box>

          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            Last scraped: {formatDate(source.last_scraped_at)}
          </Typography>
        </Box>

        <Switch
          checked={source.is_active}
          onChange={() => onToggle?.(source.id)}
          disabled={isToggling}
          sx={{
            '& .MuiSwitch-switchBase.Mui-checked': {
              color: '#8B5CF6',
            },
            '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
              backgroundColor: '#8B5CF6',
            },
          }}
        />
      </Box>
    </GlassCard>
  );
};
