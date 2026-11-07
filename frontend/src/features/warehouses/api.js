import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../lib/axios';

const WAREHOUSES_QUERY_KEY = ['warehouses'];

function toWarehousePayload(formData) {
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

export function useWarehouses({
  page = 1,
  limit = 10,
  search = '',
  status = 'ALL',
  minCapacity = undefined,
  maxCapacity = undefined,
  sortBy = 'name',
  sortDir = 'asc',
} = {}) {
  return useQuery({
    queryKey: [
      ...WAREHOUSES_QUERY_KEY,
      {
        page,
        limit,
        search,
        status,
        minCapacity,
        maxCapacity,
        sortBy,
        sortDir,
      },
    ],
    queryFn: async () => {
      const response = await api.get('/warehouses/all', {
        params: {
          page,
          limit,
          search: search || undefined,
          status: status === 'ALL' ? undefined : status,
          minCapacity,
          maxCapacity,
          sortBy: sortBy || undefined,
          sortDir: sortDir || undefined,
        },
      });
      return response.data;
    },
    keepPreviousData: true,
  });
}

export function useWarehouse(id) {
  return useQuery({
    queryKey: [...WAREHOUSES_QUERY_KEY, id],
    queryFn: async () => {
      const response = await api.get(`/warehouses/${id}`);
      return response.data;
    },
    enabled: Boolean(id),
  });
}

export function useCreateWarehouse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload) => {
      const response = await api.post(
        '/warehouses/add',
        toWarehousePayload(payload)
      );
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: WAREHOUSES_QUERY_KEY });
    },
  });
}

export function useUpdateWarehouse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await api.put(
        `/warehouses/update/${id}`,
        toWarehousePayload(data)
      );
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: WAREHOUSES_QUERY_KEY });
    },
  });
}

export function useDeleteWarehouse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id) => {
      const response = await api.delete(`/warehouses/delete/${id}`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: WAREHOUSES_QUERY_KEY });
    },
  });
}

export function useToggleWarehouseStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id) => {
      const response = await api.put(`/warehouses/toggle-status/${id}`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: WAREHOUSES_QUERY_KEY });
    },
  });
}

export function useExportWarehouses() {
  return useMutation({
    mutationFn: async () => {
      const response = await api.get('/warehouses/export', {
        responseType: 'blob',
      });
      return response.data;
    },
  });
}
