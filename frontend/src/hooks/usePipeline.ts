import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { pipelineApi } from '../api/pipeline';
import type { JobDetail } from '../types';

export const usePipeline = () => {
  const queryClient = useQueryClient();

  // Get list of jobs
  const jobsQuery = useQuery({
    queryKey: ['jobs'],
    queryFn: () => pipelineApi.getJobs(),
  });

  // Run pipeline step mutations
  const runDiscoverMutation = useMutation({
    mutationFn: (sourceId?: string) => pipelineApi.runDiscover(sourceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });

  const runDownloadMutation = useMutation({
    mutationFn: (sourceId?: string) => pipelineApi.runDownload(sourceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });

  const runExtractMutation = useMutation({
    mutationFn: (sourceId?: string) => pipelineApi.runExtract(sourceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });

  const runScoreMutation = useMutation({
    mutationFn: (sourceId?: string) => pipelineApi.runScore(sourceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });

  const runFullMutation = useMutation({
    mutationFn: (sourceId?: string) => pipelineApi.runFull(sourceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });

  const cancelJobMutation = useMutation({
    mutationFn: (jobId: string) => pipelineApi.cancelJob(jobId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });

  return {
    jobs: jobsQuery.data ?? [],
    isLoadingJobs: jobsQuery.isLoading,
    refetchJobs: jobsQuery.refetch,

    runDiscover: runDiscoverMutation.mutateAsync,
    runDownload: runDownloadMutation.mutateAsync,
    runExtract: runExtractMutation.mutateAsync,
    runScore: runScoreMutation.mutateAsync,
    runFull: runFullMutation.mutateAsync,
    cancelJob: cancelJobMutation.mutateAsync,

    isRunning:
      runDiscoverMutation.isPending ||
      runDownloadMutation.isPending ||
      runExtractMutation.isPending ||
      runScoreMutation.isPending ||
      runFullMutation.isPending,
  };
};

export const useJob = (jobId: string) => {
  return useQuery({
    queryKey: ['job', jobId],
    queryFn: () => pipelineApi.getJob(jobId),
    enabled: !!jobId,
    refetchInterval: (query) => {
      // Refetch every 2 seconds while job is running
      const data = query.state.data as JobDetail | undefined;
      return data?.status === 'running' ? 2000 : false;
    },
  });
};
