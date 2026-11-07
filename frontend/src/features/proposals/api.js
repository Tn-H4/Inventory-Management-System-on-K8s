import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../lib/axios';

const PROPOSALS_QUERY_KEY = ['proposals'];

export const useProposals = ({
  page = 1,
  limit = 10,
  sortBy = 'created_at',
  sortDir = 'DESC',
} = {}) => {
  return useQuery({
    queryKey: [...PROPOSALS_QUERY_KEY, { page, limit, sortBy, sortDir }],
    queryFn: async () => {
      const res = await api.get('/proposals/all', {
        params: { page, size: limit, sortBy, sortDir },
      });
      return res.data;
    },
  });
};

export const useProposal = (id) => {
  return useQuery({
    queryKey: [...PROPOSALS_QUERY_KEY, id],
    queryFn: async () => {
      const res = await api.get(`/proposals/${id}`);
      return res.data;
    },
    enabled: Boolean(id),
  });
};

export const usePendingProposalsCount = () => {
  return useQuery({
    queryKey: [...PROPOSALS_QUERY_KEY, 'pending-count'],
    queryFn: async () => {
      const res = await api.get('/proposals/pending-count');
      return res.data;
    },
    refetchInterval: 10000, // Refresh every 10 seconds
  });
};

export const useApproveProposal = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, reviewNote }) => {
      const res = await api.post(`/proposals/${id}/approve`, reviewNote);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PROPOSALS_QUERY_KEY });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] });
      queryClient.invalidateQueries({ queryKey: ['transactions-by-month'] });
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({
        queryKey: ['notifications', 'unread-count'],
      });
    },
  });
};

export const useRejectProposal = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, reviewNote }) => {
      const res = await api.post(`/proposals/${id}/reject`, reviewNote);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PROPOSALS_QUERY_KEY });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] });
      queryClient.invalidateQueries({ queryKey: ['transactions-by-month'] });
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({
        queryKey: ['notifications', 'unread-count'],
      });
    },
  });
};

export const useExportProposals = () => {
  return useMutation({
    mutationFn: async () => {
      const res = await api.get('/proposals/export', { responseType: 'blob' });
      return res.data;
    },
  });
};
