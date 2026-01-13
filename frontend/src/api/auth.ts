import client from './client';
import type { Token, User, LoginCredentials, RegisterData } from '../types';

export interface ProfileUpdate {
  full_name?: string;
  company_name?: string;
}

export interface PasswordChange {
  current_password: string;
  new_password: string;
}

export const authApi = {
  login: async (credentials: LoginCredentials): Promise<Token> => {
    const response = await client.post<Token>('/auth/login', credentials);
    return response.data;
  },

  register: async (data: RegisterData): Promise<Token> => {
    const response = await client.post<Token>('/auth/register', data);
    return response.data;
  },

  getMe: async (): Promise<User> => {
    const response = await client.get<User>('/auth/me');
    return response.data;
  },

  refresh: async (): Promise<Token> => {
    const response = await client.post<Token>('/auth/refresh');
    return response.data;
  },

  updateProfile: async (data: ProfileUpdate): Promise<User> => {
    const response = await client.put<User>('/auth/me', data);
    return response.data;
  },

  changePassword: async (data: PasswordChange): Promise<{ message: string }> => {
    const response = await client.post<{ message: string }>('/auth/change-password', data);
    return response.data;
  },
};
