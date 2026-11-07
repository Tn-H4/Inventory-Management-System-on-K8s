import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';

const RECONNECT_DELAY_MS = 3000;

function getApiBaseUrl() {
  return (process.env.REACT_APP_API_BASE_URL || '').replace(/\/$/, '');
}

function isNotificationListQuery(query) {
  const key = query.queryKey;
  return (
    key[0] === 'notifications' &&
    typeof key[1] === 'object' &&
    (!key[1].type || key[1].type === 'ALL')
  );
}

function upsertNotification(data, notification) {
  if (!data?.notifications || !notification?.id) {
    return data;
  }

  const exists = data.notifications.some((item) => item.id === notification.id);
  const limit = Math.max(data.notifications.length, 1);
  const notifications = exists
    ? data.notifications.map((item) =>
        item.id === notification.id ? notification : item
      )
    : [notification, ...data.notifications].slice(0, limit);

  return {
    ...data,
    notifications,
    unreadCount: Number(data.unreadCount || 0) + (notification.isRead ? 0 : 1),
    totalElements: Number(data.totalElements || 0) + (exists ? 0 : 1),
  };
}

export function useNotificationStream() {
  const queryClient = useQueryClient();
  const retryTimerRef = useRef(null);
  const abortControllerRef = useRef(null);

  useEffect(() => {
    let isMounted = true;

    const connect = async () => {
      const token = localStorage.getItem('token');
      const baseUrl = getApiBaseUrl();

      if (!token || !baseUrl) {
        return;
      }

      abortControllerRef.current = new AbortController();

      try {
        const response = await fetch(`${baseUrl}/notifications/stream`, {
          headers: {
            Authorization: `Bearer ${token}`,
            Accept: 'text/event-stream',
          },
          signal: abortControllerRef.current.signal,
        });

        if (response.status === 401 || response.status === 403) {
          return;
        }

        if (!response.ok || !response.body) {
          throw new Error('Notification stream unavailable');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (isMounted) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const chunks = buffer.split(/\r?\n\r?\n/);
          buffer = chunks.pop() || '';

          for (const chunk of chunks) {
            handleSseChunk(chunk, queryClient);
          }
        }
      } catch (error) {
        if (isMounted && error.name !== 'AbortError') {
          retryTimerRef.current = setTimeout(connect, RECONNECT_DELAY_MS);
        }
      }
    };

    connect();

    return () => {
      isMounted = false;
      if (retryTimerRef.current) {
        clearTimeout(retryTimerRef.current);
      }
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [queryClient]);
}

function handleSseChunk(chunk, queryClient) {
  const lines = chunk.split('\n');
  const eventName = lines
    .find((line) => line.startsWith('event:'))
    ?.replace('event:', '')
    .trim();
  const dataLine = lines.find((line) => line.startsWith('data:'));

  if (!eventName || !dataLine) {
    return;
  }

  let payload = null;
  try {
    payload = JSON.parse(dataLine.replace('data:', '').trim());
  } catch {
    return;
  }

  if (eventName === 'notification.created') {
    queryClient.setQueriesData({ predicate: isNotificationListQuery }, (data) =>
      upsertNotification(data, payload)
    );
    queryClient.setQueryData(['notifications', 'unread-count'], (data) => {
      if (!data || payload?.isRead) return data;
      return {
        ...data,
        unreadCount: Number(data.unreadCount || 0) + 1,
      };
    });
    queryClient.invalidateQueries({
      queryKey: ['notifications', 'unread-count'],
    });
    queryClient.invalidateQueries({ queryKey: ['notifications'] });
    invalidateRelatedQueries(queryClient, payload);
  }

  if (eventName === 'notification.changed') {
    queryClient.invalidateQueries({ queryKey: ['notifications'] });
  }
}

function invalidateRelatedQueries(queryClient, notification) {
  if (notification?.eventType?.startsWith('PROPOSAL_')) {
    queryClient.invalidateQueries({ queryKey: ['proposals'] });
  }

  if (
    notification?.eventType === 'STOCK_LOW' ||
    notification?.eventType === 'STOCK_OUT'
  ) {
    queryClient.invalidateQueries({ queryKey: ['products'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] });
  }
}
