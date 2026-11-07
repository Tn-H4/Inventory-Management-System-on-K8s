import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../lib/axios';

const PRODUCT_SUPPLIERS_QUERY_KEY = ['product-suppliers'];

export function useProductSuppliers(productId) {
  return useQuery({
    queryKey: [...PRODUCT_SUPPLIERS_QUERY_KEY, { productId }],
    queryFn: async () => {
      const response = await api.get(`/product-suppliers/product/${productId}`);
      return response.data;
    },
    enabled: Boolean(productId),
  });
}

export function useAllProductSuppliers() {
  return useQuery({
    queryKey: [...PRODUCT_SUPPLIERS_QUERY_KEY, 'all'],
    queryFn: async () => {
      const response = await api.get('/product-suppliers/all');
      return response.data;
    },
  });
}

export function useAddProductSupplier() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload) => {
      const response = await api.post('/product-suppliers/add', payload);
      return response.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: [
          ...PRODUCT_SUPPLIERS_QUERY_KEY,
          { productId: variables.productId },
        ],
      });
    },
  });
}

export function useUpdateProductSupplier() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload) => {
      const response = await api.put('/product-suppliers/update', payload);
      return response.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: [
          ...PRODUCT_SUPPLIERS_QUERY_KEY,
          { productId: variables.productId },
        ],
      });
    },
  });
}

export function useDeleteProductSupplier() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ productId, supplierId }) => {
      const response = await api.delete('/product-suppliers/delete', {
        params: { productId, supplierId },
      });
      return response.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: [
          ...PRODUCT_SUPPLIERS_QUERY_KEY,
          { productId: variables.productId },
        ],
      });
    },
  });
}

export function useGetProductSupplierTracking(productId, supplierId) {
  return useQuery({
    queryKey: [
      ...PRODUCT_SUPPLIERS_QUERY_KEY,
      'tracking',
      { productId, supplierId },
    ],
    queryFn: async () => {
      const response = await api.get(
        `/product-suppliers/product/${productId}/supplier/${supplierId}`
      );
      return response.data;
    },
    enabled: Boolean(productId && supplierId),
  });
}
