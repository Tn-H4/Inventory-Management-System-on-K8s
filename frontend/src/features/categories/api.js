import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../lib/axios';

const CATEGORIES_QUERY_KEY = ['categories'];

function toCategoryPayload(formData) {
  const status =
    typeof formData?.status === 'string' &&
    formData.status.toUpperCase() === 'INACTIVE'
      ? 'INACTIVE'
      : 'ACTIVE';

  return {
    ...formData,
    status,
    isActive: status === 'ACTIVE',
  };
}

export function useCategories({
  page = 1,
  limit = 10,
  search = '',
  description = '',
  status = 'ALL',
  sortBy = 'display_order',
  sortDir = 'asc',
} = {}) {
  return useQuery({
    queryKey: [
      ...CATEGORIES_QUERY_KEY,
      { page, limit, search, description, status, sortBy, sortDir },
    ],
    queryFn: async () => {
      const response = await api.get('/categories/all', {
        params: {
          page,
          limit,
          search: search || undefined,
          description: description || undefined,
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

export function useCategory(id) {
  return useQuery({
    queryKey: [...CATEGORIES_QUERY_KEY, id],
    queryFn: async () => {
      const response = await api.get(`/categories/${id}`);
      return response.data;
    },
    enabled: Boolean(id),
  });
}

export function useCreateCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload) => {
      const response = await api.post(
        '/categories/add',
        toCategoryPayload(payload)
      );
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CATEGORIES_QUERY_KEY });
    },
  });
}

export function useUpdateCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await api.put(
        `/categories/update/${id}`,
        toCategoryPayload(data)
      );
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CATEGORIES_QUERY_KEY });
    },
  });
}

export function useDeleteCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id) => {
      const response = await api.delete(`/categories/delete/${id}`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CATEGORIES_QUERY_KEY });
    },
  });
}

export function useToggleCategoryStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id) => {
      const response = await api.put(`/categories/toggle-status/${id}`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CATEGORIES_QUERY_KEY });
    },
  });
}

export function useExportCategories() {
  return useMutation({
    mutationFn: async (params) => {
      const response = await api.get('/categories/export', {
        params,
        responseType: 'blob',
      });
      return response.data;
    },
  });
}
