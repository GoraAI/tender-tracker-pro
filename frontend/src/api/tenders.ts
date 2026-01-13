import client from './client';
import type { Tender, TenderList, TenderFilters } from '../types';

export const tendersApi = {
  getTenders: async (filters: TenderFilters = {}): Promise<TenderList> => {
    const params = new URLSearchParams();

    if (filters.search) params.append('search', filters.search);
    if (filters.source_id) params.append('source_id', filters.source_id);
    if (filters.min_score !== undefined) params.append('min_score', String(filters.min_score));
    if (filters.deadline_from) params.append('deadline_from', filters.deadline_from);
    if (filters.deadline_to) params.append('deadline_to', filters.deadline_to);
    if (filters.sort_by) params.append('sort_by', filters.sort_by);
    if (filters.sort_order) params.append('sort_order', filters.sort_order);
    if (filters.page) params.append('page', String(filters.page));
    if (filters.per_page) params.append('per_page', String(filters.per_page));

    const response = await client.get<TenderList>(`/tenders?${params.toString()}`);
    return response.data;
  },

  getTender: async (id: string): Promise<Tender> => {
    const response = await client.get<Tender>(`/tenders/${id}`);
    return response.data;
  },
};
