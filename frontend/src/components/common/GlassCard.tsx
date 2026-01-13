import { Box } from '@mui/material';
import type { BoxProps } from '@mui/material';
import { glassEffect } from '../../theme/glassmorphism';

interface GlassCardProps extends BoxProps {
  children: React.ReactNode;
  hover?: boolean;
}

export const GlassCard = ({ children, hover = false, sx, ...props }: GlassCardProps) => {
  return (
    <Box
      sx={{
        ...glassEffect,
        padding: 3,
        transition: 'all 0.3s ease',
        ...(hover && {
          '&:hover': {
            transform: 'translateY(-4px)',
            boxShadow: '0 12px 40px rgba(0, 0, 0, 0.4)',
            borderColor: 'rgba(139, 92, 246, 0.3)',
          },
        }),
        ...sx,
      }}
      {...props}
    >
      {children}
    </Box>
  );
};
