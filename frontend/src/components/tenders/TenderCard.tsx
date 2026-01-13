import { Box, Typography, Chip } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import { GlassCard } from '../common/GlassCard';
import { ScoreBadge } from '../common/ScoreBadge';
import type { TenderListItem } from '../../types';

interface TenderCardProps {
  tender: TenderListItem;
}

export const TenderCard = ({ tender }: TenderCardProps) => {
  const navigate = useNavigate();

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return 'No deadline';
    return new Date(dateStr).toLocaleDateString('en-ZA', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  };

  const formatValue = (value: number | null) => {
    if (!value) return null;
    return new Intl.NumberFormat('en-ZA', {
      style: 'currency',
      currency: 'ZAR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  return (
    <GlassCard
      hover
      onClick={() => navigate(`/tenders/${tender.id}`)}
      sx={{ cursor: 'pointer' }}
    >
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
        <Box sx={{ flex: 1, minWidth: 0, mr: 2 }}>
          <Typography variant="h6" sx={{ fontWeight: 600, mb: 0.5 }} noWrap>
            {tender.title || tender.reference || 'Untitled Tender'}
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {tender.reference} | {tender.source_id}
          </Typography>
        </Box>
        {tender.score !== null && (
          <ScoreBadge score={tender.score} showCategory />
        )}
      </Box>

      <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
        <Chip
          icon={<CalendarTodayIcon sx={{ fontSize: 16 }} />}
          label={formatDate(tender.deadline)}
          size="small"
          variant="outlined"
          sx={{ borderColor: 'rgba(255, 255, 255, 0.1)' }}
        />
        {tender.estimated_value && (
          <Chip
            icon={<AttachMoneyIcon sx={{ fontSize: 16 }} />}
            label={formatValue(tender.estimated_value)}
            size="small"
            variant="outlined"
            sx={{ borderColor: 'rgba(255, 255, 255, 0.1)' }}
          />
        )}
      </Box>
    </GlassCard>
  );
};
