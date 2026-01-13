import { Chip } from '@mui/material';
import type { ChipProps } from '@mui/material';
import { getScoreColor, getScoreCategory } from '../../theme/glassmorphism';

interface ScoreBadgeProps extends Omit<ChipProps, 'label' | 'color'> {
  score: number;
  showCategory?: boolean;
}

export const ScoreBadge = ({ score, showCategory = false, sx, ...props }: ScoreBadgeProps) => {
  const color = getScoreColor(score);
  const category = getScoreCategory(score);

  return (
    <Chip
      label={showCategory ? `${score} - ${category}` : score}
      sx={{
        backgroundColor: `${color}20`,
        color: color,
        border: `1px solid ${color}40`,
        fontWeight: 600,
        ...sx,
      }}
      {...props}
    />
  );
};
