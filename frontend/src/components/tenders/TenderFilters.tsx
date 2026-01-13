import { Box, TextField, FormControl, InputLabel, Select, MenuItem, InputAdornment } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import type { TenderFilters as Filters } from '../../types';

interface TenderFiltersProps {
  filters: Filters;
  onChange: (filters: Filters) => void;
}

export const TenderFilters = ({ filters, onChange }: TenderFiltersProps) => {
  const handleChange = (key: keyof Filters, value: string | number | undefined) => {
    onChange({
      ...filters,
      [key]: value || undefined,
      page: 1, // Reset to first page on filter change
    });
  };

  return (
    <Box
      sx={{
        display: 'flex',
        gap: 2,
        flexWrap: 'wrap',
        mb: 3,
        p: 2,
        background: 'rgba(30, 41, 59, 0.4)',
        borderRadius: '16px',
        border: '1px solid rgba(255, 255, 255, 0.1)',
      }}
    >
      <TextField
        placeholder="Search tenders..."
        value={filters.search || ''}
        onChange={(e) => handleChange('search', e.target.value)}
        size="small"
        sx={{ flex: 1, minWidth: 200 }}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <SearchIcon sx={{ color: 'text.secondary' }} />
            </InputAdornment>
          ),
        }}
      />

      <FormControl size="small" sx={{ minWidth: 150 }}>
        <InputLabel>Min Score</InputLabel>
        <Select
          value={filters.min_score || ''}
          label="Min Score"
          onChange={(e) => handleChange('min_score', e.target.value as number)}
        >
          <MenuItem value="">All</MenuItem>
          <MenuItem value={90}>90+ (Excellent)</MenuItem>
          <MenuItem value={70}>70+ (Good)</MenuItem>
          <MenuItem value={50}>50+ (Moderate)</MenuItem>
        </Select>
      </FormControl>

      <FormControl size="small" sx={{ minWidth: 150 }}>
        <InputLabel>Sort By</InputLabel>
        <Select
          value={filters.sort_by || 'discovered_at'}
          label="Sort By"
          onChange={(e) => handleChange('sort_by', e.target.value)}
        >
          <MenuItem value="discovered_at">Discovered</MenuItem>
          <MenuItem value="deadline">Deadline</MenuItem>
          <MenuItem value="score">Score</MenuItem>
        </Select>
      </FormControl>

      <FormControl size="small" sx={{ minWidth: 120 }}>
        <InputLabel>Order</InputLabel>
        <Select
          value={filters.sort_order || 'desc'}
          label="Order"
          onChange={(e) => handleChange('sort_order', e.target.value as 'asc' | 'desc')}
        >
          <MenuItem value="desc">Descending</MenuItem>
          <MenuItem value="asc">Ascending</MenuItem>
        </Select>
      </FormControl>
    </Box>
  );
};
