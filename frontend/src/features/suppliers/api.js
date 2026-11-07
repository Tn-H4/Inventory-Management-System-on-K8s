import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../lib/axios';

const SUPPLIERS_QUERY_KEY = ['suppliers'];

export function useSuppliers({
  page = 1,
  limit = 10,
  search = '',
  taxCode = '',
  status = 'ALL',
  sortBy = 'name',
  sortDir = 'asc',
} = {}) {
  return useQuery({
    queryKey: [
      ...SUPPLIERS_QUERY_KEY,
      { page, limit, input: search, taxCode, status, sortBy, sortDir },
    ],
    queryFn: async () => {
      const response = await api.get('/suppliers/all', {
        params: {
          page,
          limit,
          input: search || undefined,
          taxCode: taxCode || undefined,
          status: status === 'ALL' ? undefined : status,
          sortBy: sortBy || undefined,
          sortDir: sortDir || undefined,
        },
      });
      return response.data;
    },
    keepPreviousData: true,
  });
}

export function useSupplier(id) {
  return useQuery({
    queryKey: [...SUPPLIERS_QUERY_KEY, id],
    queryFn: async () => {
      const response = await api.get(`/suppliers/${id}`);
      return response.data;
    },
    enabled: Boolean(id),
  });
}

export function useCreateSupplier() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload) => {
      const response = await api.post('/suppliers/add', payload);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SUPPLIERS_QUERY_KEY });
    },
  });
}

export function useUpdateSupplier() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await api.put(`/suppliers/update/${id}`, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SUPPLIERS_QUERY_KEY });
    },
  });
}

export function useDeleteSupplier() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id) => {
      const response = await api.delete(`/suppliers/delete/${id}`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SUPPLIERS_QUERY_KEY });
    },
  });
}

export function useToggleSupplierStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id) => {
      const response = await api.put(`/suppliers/toggle-status/${id}`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SUPPLIERS_QUERY_KEY });
    },
  });
}

export function useExportSuppliers() {
  return useMutation({
    mutationFn: async (params) => {
      const response = await api.get('/suppliers/export', {
        params,
        responseType: 'blob',
      });
      return response.data;
    },
  });
}
