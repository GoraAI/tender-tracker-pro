import { apiClient } from './client';
import type { Job, JobDetail } from '../types';

export const pipelineApi = {
  runDiscover: async (sourceId?: string): Promise<Job> => {
    const { data } = await apiClient.post<Job>('/pipeline/discover', {
      source_id: sourceId,
    });
    return data;
  },

  runDownload: async (sourceId?: string): Promise<Job> => {
    const { data } = await apiClient.post<Job>('/pipeline/download', {
      source_id: sourceId,
    });
    return data;
  },

  runExtract: async (sourceId?: string): Promise<Job> => {
    const { data } = await apiClient.post<Job>('/pipeline/extract', {
      source_id: sourceId,
    });
    return data;
  },

  runScore: async (sourceId?: string): Promise<Job> => {
    const { data } = await apiClient.post<Job>('/pipeline/score', {
      source_id: sourceId,
    });
    return data;
  },

  runFull: async (sourceId?: string): Promise<Job> => {
    const { data } = await apiClient.post<Job>('/pipeline/full', {
      source_id: sourceId,
    });
    return data;
  },

  getJobs: async (limit = 20, offset = 0): Promise<Job[]> => {
    const { data } = await apiClient.get<Job[]>('/pipeline/jobs', {
      params: { limit, offset },
    });
    return data;
  },

  getJob: async (id: string): Promise<JobDetail> => {
    const { data } = await apiClient.get<JobDetail>(`/pipeline/jobs/${id}`);
    return data;
  },

  cancelJob: async (id: string): Promise<Job> => {
    const { data } = await apiClient.post<Job>(`/pipeline/jobs/${id}/cancel`);
    return data;
  },
};
