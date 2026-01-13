import { Box, Typography } from '@mui/material';
import type { SvgIconComponent } from '@mui/icons-material';
import { GlassCard } from './GlassCard';

interface StatWidgetProps {
  icon: SvgIconComponent;
  value: string | number;
  label: string;
  color?: string;
}

export const StatWidget = ({ icon: Icon, value, label, color = '#8B5CF6' }: StatWidgetProps) => {
  return (
    <GlassCard hover>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <Box
          sx={{
            width: 56,
            height: 56,
            borderRadius: '12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: `linear-gradient(135deg, ${color}40 0%, ${color}20 100%)`,
            border: `1px solid ${color}40`,
          }}
        >
          <Icon sx={{ fontSize: 28, color }} />
        </Box>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700, color: 'text.primary' }}>
            {value}
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {label}
          </Typography>
        </Box>
      </Box>
    </GlassCard>
  );
};
