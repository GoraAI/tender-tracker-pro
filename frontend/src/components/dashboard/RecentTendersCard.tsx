import { Box, List, ListItem, ListItemButton, ListItemText, Typography, Skeleton } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { GlassCard } from '../common/GlassCard';
import { ScoreBadge } from '../common/ScoreBadge';
import { useRecentTenders } from '../../hooks/useDashboard';

export const RecentTendersCard = () => {
  const navigate = useNavigate();
  const { data: tenders, isLoading } = useRecentTenders();

  return (
    <GlassCard sx={{ height: '100%' }}>
      <Typography variant="h6" sx={{ mb: 2 }}>
        Recent High-Priority Tenders
      </Typography>

      {isLoading ? (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} variant="rectangular" height={60} sx={{ borderRadius: 2 }} />
          ))}
        </Box>
      ) : tenders && tenders.length > 0 ? (
        <List disablePadding>
          {tenders.map((tender) => (
            <ListItem key={tender.id} disablePadding sx={{ mb: 1 }}>
              <ListItemButton
                onClick={() => navigate(`/tenders/${tender.id}`)}
                sx={{
                  borderRadius: '12px',
                  background: 'rgba(255, 255, 255, 0.03)',
                  border: '1px solid rgba(255, 255, 255, 0.05)',
                  '&:hover': {
                    background: 'rgba(139, 92, 246, 0.1)',
                    borderColor: 'rgba(139, 92, 246, 0.2)',
                  },
                }}
              >
                <ListItemText
                  primary={tender.title || tender.reference || 'Untitled Tender'}
                  secondary={tender.source_id}
                  primaryTypographyProps={{
                    noWrap: true,
                    fontWeight: 500,
                  }}
                  secondaryTypographyProps={{
                    color: 'text.secondary',
                  }}
                />
                <ScoreBadge score={tender.score} size="small" />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      ) : (
        <Typography variant="body2" sx={{ color: 'text.secondary', textAlign: 'center', py: 4 }}>
          No high-priority tenders yet. Run the pipeline to discover tenders.
        </Typography>
      )}
    </GlassCard>
  );
};
