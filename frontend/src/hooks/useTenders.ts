import { useQuery } from '@tanstack/react-query';
import { tendersApi } from '../api/tenders';
import type { TenderFilters } from '../types';

export const useTenders = (filters: TenderFilters = {}) => {
  return useQuery({
    queryKey: ['tenders', filters],
    queryFn: () => tendersApi.getTenders(filters),
  });
};

export const useTender = (id: string) => {
  return useQuery({
    queryKey: ['tender', id],
    queryFn: () => tendersApi.getTender(id),
    enabled: !!id,
  });
};
