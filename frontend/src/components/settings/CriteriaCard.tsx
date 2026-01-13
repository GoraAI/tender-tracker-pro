import { Box, Chip, IconButton, Typography } from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import StarIcon from '@mui/icons-material/Star';
import { GlassCard } from '../common/GlassCard';
import type { Criteria } from '../../types';

interface CriteriaCardProps {
  criteria: Criteria;
  onEdit?: (criteria: Criteria) => void;
  onDelete?: (id: string) => void;
}

export const CriteriaCard = ({ criteria, onEdit, onDelete }: CriteriaCardProps) => {
  const formatValueRange = () => {
    const { min, max } = criteria.value_range;
    if (max === null) {
      return min > 0 ? `R ${min.toLocaleString()}+` : 'Any';
    }
    return `R ${min.toLocaleString()} - R ${max.toLocaleString()}`;
  };

  return (
    <GlassCard hover>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="h6" sx={{ fontWeight: 600 }}>
            {criteria.name}
          </Typography>
          {criteria.is_default && (
            <StarIcon sx={{ fontSize: 18, color: '#F59E0B' }} />
          )}
        </Box>
        <Box>
          <IconButton
            size="small"
            onClick={() => onEdit?.(criteria)}
            sx={{ color: 'text.secondary' }}
          >
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            onClick={() => onDelete?.(criteria.id)}
            sx={{ color: 'text.secondary', '&:hover': { color: '#EF4444' } }}
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Box>
      </Box>

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        {criteria.capabilities.length > 0 && (
          <Box>
            <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mb: 0.5 }}>
              Capabilities
            </Typography>
            <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
              {criteria.capabilities.map((cap) => (
                <Chip
                  key={cap}
                  label={cap}
                  size="small"
                  sx={{
                    backgroundColor: 'rgba(139, 92, 246, 0.2)',
                    color: '#A78BFA',
                  }}
                />
              ))}
            </Box>
          </Box>
        )}

        {criteria.preferred_sectors.length > 0 && (
          <Box>
            <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mb: 0.5 }}>
              Sectors
            </Typography>
            <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
              {criteria.preferred_sectors.map((sector) => (
                <Chip
                  key={sector}
                  label={sector}
                  size="small"
                  sx={{
                    backgroundColor: 'rgba(236, 72, 153, 0.2)',
                    color: '#F472B6',
                  }}
                />
              ))}
            </Box>
          </Box>
        )}

        {criteria.keywords.length > 0 && (
          <Box>
            <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mb: 0.5 }}>
              Keywords
            </Typography>
            <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
              {criteria.keywords.map((kw) => (
                <Chip
                  key={kw}
                  label={kw}
                  size="small"
                  variant="outlined"
                  sx={{
                    borderColor: 'rgba(255, 255, 255, 0.2)',
                    color: 'text.secondary',
                  }}
                />
              ))}
            </Box>
          </Box>
        )}

        <Box>
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            Value Range: {formatValueRange()}
          </Typography>
        </Box>
      </Box>
    </GlassCard>
  );
};
