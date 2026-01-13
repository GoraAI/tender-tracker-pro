import { useParams, useNavigate } from 'react-router-dom';
import { Box, Button, Chip, Grid, Typography, Skeleton, Divider } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import PersonIcon from '@mui/icons-material/Person';
import EmailIcon from '@mui/icons-material/Email';
import LinkIcon from '@mui/icons-material/Link';
import { GlassCard } from '../components/common/GlassCard';
import { ScoreBadge } from '../components/common/ScoreBadge';
import { useTender } from '../hooks/useTenders';

export const TenderDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: tender, isLoading } = useTender(id || '');

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return 'Not specified';
    return new Date(dateStr).toLocaleDateString('en-ZA', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  };

  const formatValue = (value: number | null) => {
    if (!value) return 'Not specified';
    return new Intl.NumberFormat('en-ZA', {
      style: 'currency',
      currency: 'ZAR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  if (isLoading) {
    return (
      <Box>
        <Skeleton variant="rectangular" height={400} sx={{ borderRadius: 4 }} />
      </Box>
    );
  }

  if (!tender) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography variant="h6">Tender not found</Typography>
        <Button onClick={() => navigate('/tenders')} sx={{ mt: 2 }}>
          Back to Tenders
        </Button>
      </Box>
    );
  }

  return (
    <Box>
      <Button
        startIcon={<ArrowBackIcon />}
        onClick={() => navigate('/tenders')}
        sx={{ mb: 3 }}
      >
        Back to Tenders
      </Button>

      <Grid container spacing={3}>
        {/* Main Content */}
        <Grid size={{ xs: 12, lg: 8 }}>
          <GlassCard>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 3 }}>
              <Box>
                <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
                  {tender.title || 'Untitled Tender'}
                </Typography>
                <Typography variant="body1" sx={{ color: 'text.secondary' }}>
                  Reference: {tender.reference || 'N/A'}
                </Typography>
              </Box>
              {tender.score !== null && (
                <ScoreBadge score={tender.score} showCategory size="medium" />
              )}
            </Box>

            <Divider sx={{ my: 3, borderColor: 'rgba(255, 255, 255, 0.1)' }} />

            {/* Details Grid */}
            <Grid container spacing={3}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                  <CalendarTodayIcon sx={{ color: 'text.secondary' }} />
                  <Box>
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                      Deadline
                    </Typography>
                    <Typography variant="body1">{formatDate(tender.deadline)}</Typography>
                  </Box>
                </Box>
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                  <AttachMoneyIcon sx={{ color: 'text.secondary' }} />
                  <Box>
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                      Estimated Value
                    </Typography>
                    <Typography variant="body1">{formatValue(tender.estimated_value)}</Typography>
                  </Box>
                </Box>
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                  <PersonIcon sx={{ color: 'text.secondary' }} />
                  <Box>
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                      Contact
                    </Typography>
                    <Typography variant="body1">{tender.contact_name || 'Not specified'}</Typography>
                  </Box>
                </Box>
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                  <EmailIcon sx={{ color: 'text.secondary' }} />
                  <Box>
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                      Email
                    </Typography>
                    <Typography variant="body1">{tender.contact_email || 'Not specified'}</Typography>
                  </Box>
                </Box>
              </Grid>
            </Grid>

            {tender.requirements_summary && (
              <>
                <Divider sx={{ my: 3, borderColor: 'rgba(255, 255, 255, 0.1)' }} />
                <Typography variant="h6" sx={{ mb: 2 }}>
                  Requirements Summary
                </Typography>
                <Typography variant="body1" sx={{ color: 'text.secondary', whiteSpace: 'pre-wrap' }}>
                  {tender.requirements_summary}
                </Typography>
              </>
            )}

            <Divider sx={{ my: 3, borderColor: 'rgba(255, 255, 255, 0.1)' }} />

            <Button
              variant="outlined"
              startIcon={<LinkIcon />}
              href={tender.pdf_url}
              target="_blank"
              rel="noopener noreferrer"
            >
              View Original PDF
            </Button>
          </GlassCard>
        </Grid>

        {/* Sidebar */}
        <Grid size={{ xs: 12, lg: 4 }}>
          <GlassCard>
            <Typography variant="h6" sx={{ mb: 2 }}>
              Score Details
            </Typography>

            {tender.score !== null ? (
              <>
                <Box sx={{ textAlign: 'center', mb: 3 }}>
                  <Typography
                    variant="h1"
                    sx={{
                      fontWeight: 700,
                      background: 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent',
                    }}
                  >
                    {tender.score}
                  </Typography>
                  <Chip
                    label={tender.relevance_category}
                    sx={{
                      mt: 1,
                      backgroundColor: 'rgba(139, 92, 246, 0.2)',
                      color: 'primary.main',
                    }}
                  />
                </Box>

                {tender.score_explanation && (
                  <Box>
                    <Typography variant="subtitle2" sx={{ mb: 1 }}>
                      Why this score?
                    </Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                      {tender.score_explanation}
                    </Typography>
                  </Box>
                )}
              </>
            ) : (
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                This tender has not been scored yet.
              </Typography>
            )}
          </GlassCard>

          <GlassCard sx={{ mt: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>
              Metadata
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box>
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                  Source
                </Typography>
                <Typography variant="body2">{tender.source_id}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                  Discovered
                </Typography>
                <Typography variant="body2">{formatDate(tender.discovered_at)}</Typography>
              </Box>
              {tender.extraction_confidence && (
                <Box>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                    Extraction Confidence
                  </Typography>
                  <Typography variant="body2">
                    {Math.round(tender.extraction_confidence * 100)}%
                  </Typography>
                </Box>
              )}
            </Box>
          </GlassCard>
        </Grid>
      </Grid>
    </Box>
  );
};
