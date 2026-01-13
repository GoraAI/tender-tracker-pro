import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { useAuthStore } from '../store/authStore';
import type { LoginCredentials, RegisterData } from '../types';

export const useAuth = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { token, user, isAuthenticated, login, logout, setUser } = useAuthStore();

  // Fetch current user on mount if token exists
  const { data: userData, isLoading: isLoadingUser, isError } = useQuery({
    queryKey: ['user'],
    queryFn: authApi.getMe,
    enabled: !!token && !user,
    retry: false,
  });

  // Handle user data updates
  useEffect(() => {
    if (userData) {
      setUser(userData);
    }
  }, [userData, setUser]);

  // Handle auth errors
  useEffect(() => {
    if (isError) {
      logout();
    }
  }, [isError, logout]);

  // Login mutation
  const loginMutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: async (data) => {
      useAuthStore.getState().setToken(data.access_token);
      const user = await authApi.getMe();
      login(data.access_token, user);
      queryClient.invalidateQueries();
      navigate('/dashboard');
    },
  });

  // Register mutation
  const registerMutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: async (data) => {
      useAuthStore.getState().setToken(data.access_token);
      const user = await authApi.getMe();
      login(data.access_token, user);
      queryClient.invalidateQueries();
      navigate('/dashboard');
    },
  });

  // Logout handler
  const handleLogout = () => {
    logout();
    queryClient.clear();
    navigate('/login');
  };

  return {
    user,
    token,
    isAuthenticated,
    isLoading: isLoadingUser,
    login: (credentials: LoginCredentials) => loginMutation.mutateAsync(credentials),
    register: (data: RegisterData) => registerMutation.mutateAsync(data),
    logout: handleLogout,
    loginError: loginMutation.error,
    registerError: registerMutation.error,
    isLoggingIn: loginMutation.isPending,
    isRegistering: registerMutation.isPending,
  };
};
