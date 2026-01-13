import { apiClient } from './client';
import type { Criteria, CriteriaCreate, CriteriaUpdate, Source } from '../types';

export const configApi = {
  // Criteria endpoints
  getCriteria: async (): Promise<Criteria[]> => {
    const { data } = await apiClient.get<Criteria[]>('/config/criteria');
    return data;
  },

  getCriteriaById: async (id: string): Promise<Criteria> => {
    const { data } = await apiClient.get<Criteria>(`/config/criteria/${id}`);
    return data;
  },

  createCriteria: async (criteria: CriteriaCreate): Promise<Criteria> => {
    const { data } = await apiClient.post<Criteria>('/config/criteria', criteria);
    return data;
  },

  updateCriteria: async (id: string, criteria: CriteriaUpdate): Promise<Criteria> => {
    const { data } = await apiClient.put<Criteria>(`/config/criteria/${id}`, criteria);
    return data;
  },

  deleteCriteria: async (id: string): Promise<void> => {
    await apiClient.delete(`/config/criteria/${id}`);
  },

  // Source endpoints
  getSources: async (): Promise<Source[]> => {
    const { data } = await apiClient.get<Source[]>('/config/sources');
    return data;
  },

  toggleSource: async (id: string): Promise<Source> => {
    const { data } = await apiClient.put<Source>(`/config/sources/${id}/toggle`);
    return data;
  },

  updateSource: async (id: string, isActive: boolean): Promise<Source> => {
    const { data } = await apiClient.put<Source>(`/config/sources/${id}`, {
      is_active: isActive,
    });
    return data;
  },
};
