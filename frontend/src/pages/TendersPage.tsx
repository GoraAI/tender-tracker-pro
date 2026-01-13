import { useState } from 'react';
import { Box, Typography, Pagination, Skeleton } from '@mui/material';
import { TenderCard } from '../components/tenders/TenderCard';
import { TenderFilters } from '../components/tenders/TenderFilters';
import { useTenders } from '../hooks/useTenders';
import type { TenderFilters as Filters } from '../types';

export const TendersPage = () => {
  const [filters, setFilters] = useState<Filters>({
    page: 1,
    per_page: 10,
    sort_by: 'discovered_at',
    sort_order: 'desc',
  });

  const { data, isLoading } = useTenders(filters);

  const handlePageChange = (_: React.ChangeEvent<unknown>, page: number) => {
    setFilters((prev) => ({ ...prev, page }));
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 4, fontWeight: 700 }}>
        Tenders
      </Typography>

      <TenderFilters filters={filters} onChange={setFilters} />

      {isLoading ? (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} variant="rectangular" height={120} sx={{ borderRadius: 4 }} />
          ))}
        </Box>
      ) : data?.items && data.items.length > 0 ? (
        <>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mb: 3 }}>
            {data.items.map((tender) => (
              <TenderCard key={tender.id} tender={tender} />
            ))}
          </Box>

          {data.pages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center' }}>
              <Pagination
                count={data.pages}
                page={data.page}
                onChange={handlePageChange}
                color="primary"
                sx={{
                  '& .MuiPaginationItem-root': {
                    color: 'text.primary',
                  },
                }}
              />
            </Box>
          )}
        </>
      ) : (
        <Box
          sx={{
            textAlign: 'center',
            py: 8,
            background: 'rgba(30, 41, 59, 0.4)',
            borderRadius: '16px',
            border: '1px solid rgba(255, 255, 255, 0.1)',
          }}
        >
          <Typography variant="h6" sx={{ color: 'text.secondary', mb: 1 }}>
            No tenders found
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            Run the pipeline to discover tenders, or adjust your filters.
          </Typography>
        </Box>
      )}
    </Box>
  );
};
