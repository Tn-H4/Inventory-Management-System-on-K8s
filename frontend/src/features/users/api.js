import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../lib/axios';

const USERS_QUERY_KEY = ['users'];

export function useUsers({
  page = 1,
  limit = 10,
  search = '',
  role = 'ALL',
  status = 'ALL',
  startDate = undefined,
  endDate = undefined,
  sortBy = 'id',
  sortDir = 'ASC',
} = {}) {
  return useQuery({
    queryKey: [
      ...USERS_QUERY_KEY,
      {
        page,
        limit,
        search,
        role,
        status,
        startDate,
        endDate,
        sortBy,
        sortDir,
      },
    ],
    queryFn: async () => {
      const response = await api.get('/users/all', {
        params: {
          page,
          limit,
          search: search || undefined,
          role: role === 'ALL' ? undefined : role,
          status: status === 'ALL' ? undefined : status,
          startDate,
          endDate,
          sortBy,
          sortDir,
        },
      });
      return response.data;
    },
    keepPreviousData: true,
  });
}

export function useUser(id) {
  return useQuery({
    queryKey: [...USERS_QUERY_KEY, id],
    queryFn: async () => {
      const response = await api.get(`/users/${id}`);
      return response.data;
    },
    enabled: Boolean(id),
  });
}

export function useCurrentUser() {
  return useQuery({
    queryKey: [...USERS_QUERY_KEY, 'current'],
    queryFn: async () => {
      const response = await api.get('/users/current');
      return response.data;
    },
  });
}

export function useCreateUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload) => {
      const response = await api.post('/users/create', payload);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: USERS_QUERY_KEY });
    },
  });
}

export function useUpdateUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await api.put(`/users/update/${id}`, data);
      return response.data;
    },
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: USERS_QUERY_KEY });
      queryClient.invalidateQueries({
        queryKey: [...USERS_QUERY_KEY, variables.id],
      });
      queryClient.invalidateQueries({
        queryKey: [...USERS_QUERY_KEY, 'current'],
      });
    },
  });
}

export function useDeleteUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id) => {
      const response = await api.delete(`/users/delete/${id}`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: USERS_QUERY_KEY });
    },
  });
}

export function useActivateUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id) => {
      const response = await api.put(`/users/activate/${id}`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: USERS_QUERY_KEY });
    },
  });
}

export function useChangePassword() {
  return useMutation({
    mutationFn: async ({ id, currentPassword, newPassword }) => {
      const response = await api.put(`/users/change-password/${id}`, {
        currentPassword,
        newPassword,
      });
      return response.data;
    },
  });
}

export function useExportUsers() {
  return useMutation({
    mutationFn: async () => {
      const response = await api.get('/users/export', {
        responseType: 'blob',
      });
      return response.data;
    },
  });
}
