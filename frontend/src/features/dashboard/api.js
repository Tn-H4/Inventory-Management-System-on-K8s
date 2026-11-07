import { useMutation, useQuery } from '@tanstack/react-query';
import api from '../../lib/axios';

export const DASHBOARD_STATS_QUERY_KEY = ['dashboard-stats'];

export function useDashboardStats() {
  return useQuery({
    queryKey: DASHBOARD_STATS_QUERY_KEY,
    queryFn: async () => {
      const res = await api.get('/dashboard/stats');
      return res.data;
    },
  });
}

export function useExportDashboard() {
  return useMutation({
    mutationFn: async () => {
      const res = await api.get('/dashboard/export', { responseType: 'blob' });
      return res.data;
    },
  });
}
