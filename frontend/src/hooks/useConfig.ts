import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { configApi } from '../api/config';
import type { CriteriaCreate, CriteriaUpdate } from '../types';

export const useCriteria = () => {
  const queryClient = useQueryClient();

  const criteriaQuery = useQuery({
    queryKey: ['criteria'],
    queryFn: configApi.getCriteria,
  });

  const createMutation = useMutation({
    mutationFn: (data: CriteriaCreate) => configApi.createCriteria(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['criteria'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CriteriaUpdate }) =>
      configApi.updateCriteria(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['criteria'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => configApi.deleteCriteria(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['criteria'] });
    },
  });

  return {
    criteria: criteriaQuery.data ?? [],
    isLoading: criteriaQuery.isLoading,
    error: criteriaQuery.error,
    refetch: criteriaQuery.refetch,

    createCriteria: createMutation.mutateAsync,
    updateCriteria: (id: string, data: CriteriaUpdate) =>
      updateMutation.mutateAsync({ id, data }),
    deleteCriteria: deleteMutation.mutateAsync,

    isCreating: createMutation.isPending,
    isUpdating: updateMutation.isPending,
    isDeleting: deleteMutation.isPending,
  };
};

export const useSources = () => {
  const queryClient = useQueryClient();

  const sourcesQuery = useQuery({
    queryKey: ['sources'],
    queryFn: configApi.getSources,
  });

  const toggleMutation = useMutation({
    mutationFn: (id: string) => configApi.toggleSource(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sources'] });
    },
  });

  return {
    sources: sourcesQuery.data ?? [],
    isLoading: sourcesQuery.isLoading,
    error: sourcesQuery.error,
    refetch: sourcesQuery.refetch,

    toggleSource: toggleMutation.mutateAsync,
    isToggling: toggleMutation.isPending,
  };
};
