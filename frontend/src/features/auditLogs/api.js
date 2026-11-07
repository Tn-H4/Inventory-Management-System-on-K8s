import { useMutation, useQuery } from '@tanstack/react-query';
import api from '../../lib/axios';

const AUDIT_LOGS_QUERY_KEY = ['auditLogs'];

export const useAuditLogs = ({
  page = 1,
  limit = 20,
  search = '',
  action = 'ALL',
  entityType = 'ALL',
  startDate = undefined,
  endDate = undefined,
  sortBy = 'created_at',
  sortDir = 'DESC',
} = {}) => {
  return useQuery({
    queryKey: [
      ...AUDIT_LOGS_QUERY_KEY,
      {
        page,
        limit,
        search,
        action,
        entityType,
        startDate,
        endDate,
        sortBy,
        sortDir,
      },
    ],
    queryFn: async () => {
      const res = await api.get('/audit-logs/all', {
        params: {
          page,
          size: limit,
          search: search || undefined,
          action: action === 'ALL' ? undefined : action,
          entityType: entityType === 'ALL' ? undefined : entityType,
          startDate,
          endDate,
          sortBy,
          sortDir,
        },
      });
      return res.data;
    },
    keepPreviousData: true,
  });
};

export const useAuditLog = (id) => {
  return useQuery({
    queryKey: [...AUDIT_LOGS_QUERY_KEY, id],
    queryFn: async () => {
      const res = await api.get(`/audit-logs/${id}`);
      return res.data;
    },
    enabled: Boolean(id),
  });
};

export const useExportAuditLogs = () => {
  return useMutation({
    mutationFn: async () => {
      const res = await api.get('/audit-logs/export', { responseType: 'blob' });
      return res.data;
    },
  });
};
