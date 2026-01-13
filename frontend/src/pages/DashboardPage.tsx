import { Box, Grid, Typography, Skeleton } from '@mui/material';
import DescriptionIcon from '@mui/icons-material/Description';
import StarIcon from '@mui/icons-material/Star';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import SourceIcon from '@mui/icons-material/Source';
import { StatWidget } from '../components/common/StatWidget';
import { RecentTendersCard } from '../components/dashboard/RecentTendersCard';
import { QuickActionsCard } from '../components/dashboard/QuickActionsCard';
import { useDashboardStats } from '../hooks/useDashboard';

export const DashboardPage = () => {
  const { data: stats, isLoading } = useDashboardStats();

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 4, fontWeight: 700 }}>
        Dashboard
      </Typography>

      {/* Stats Grid */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={100} sx={{ borderRadius: 4 }} />
          ) : (
            <StatWidget
              icon={DescriptionIcon}
              value={stats?.total_tenders || 0}
              label="Total Tenders"
              color="#8B5CF6"
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={100} sx={{ borderRadius: 4 }} />
          ) : (
            <StatWidget
              icon={StarIcon}
              value={stats?.high_priority_count || 0}
              label="High Priority"
              color="#22C55E"
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={100} sx={{ borderRadius: 4 }} />
          ) : (
            <StatWidget
              icon={TrendingUpIcon}
              value={stats?.avg_score ? Math.round(stats.avg_score) : 'N/A'}
              label="Avg Score"
              color="#3B82F6"
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={100} sx={{ borderRadius: 4 }} />
          ) : (
            <StatWidget
              icon={SourceIcon}
              value={stats?.sources_active || 0}
              label="Active Sources"
              color="#EC4899"
            />
          )}
        </Grid>
      </Grid>

      {/* Cards Grid */}
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <RecentTendersCard />
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <QuickActionsCard />
        </Grid>
      </Grid>
    </Box>
  );
};
