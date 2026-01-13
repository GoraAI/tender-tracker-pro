import client from './client';
import type { DashboardStats, RecentTender } from '../types';

export const dashboardApi = {
  getStats: async (): Promise<DashboardStats> => {
    const response = await client.get<DashboardStats>('/dashboard/stats');
    return response.data;
  },

  getRecentTenders: async (): Promise<RecentTender[]> => {
    const response = await client.get<RecentTender[]>('/dashboard/recent');
    return response.data;
  },
};
