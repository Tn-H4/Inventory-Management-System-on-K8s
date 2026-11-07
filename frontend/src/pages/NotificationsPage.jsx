import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useNotifications,
  useMarkAsRead,
  useMarkAllAsRead,
  useDeleteNotification,
  useDeleteAllNotifications,
} from '../features/notifications/api';
import { getTypeStyles, getTypeBorder } from '../features/notifications/utils';
import PaginationComponent from '../components/PaginationComponent';
import { useConfirmation } from '../components/ConfirmationModal';

function NotificationsPage() {
  const [filter, setFilter] = useState('ALL'); // ALL, UNREAD, READ
  const [deleteError, setDeleteError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(5);
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState('newest');
  const navigate = useNavigate();

  const {
    data: notificationsData,
    isLoading,
    isError,
    error,
  } = useNotifications({ page, limit, type: typeFilter, sortBy });
  const markAsReadMutation = useMarkAsRead();
  const markAllAsReadMutation = useMarkAllAsRead();
  const deleteMutation = useDeleteNotification();
  const deleteAllMutation = useDeleteAllNotifications();

  const { confirm: confirmDeleteAll, modal: deleteAllModal } = useConfirmation({
    title: 'Delete All Notifications',
    message:
      'Are you sure you want to delete all notifications? This cannot be undone.',
    confirmText: 'Delete All',
    cancelText: 'Cancel',
    type: 'danger',
  });

  const notifications = useMemo(() => {
    const list = notificationsData?.notifications ?? [];
    if (filter === 'UNREAD') return list.filter((n) => !n.isRead);
    if (filter === 'READ') return list.filter((n) => n.isRead);
    return list;
  }, [notificationsData, filter]);

  const unreadCount = notificationsData?.unreadCount ?? 0;
  const totalPages = notificationsData?.totalPages ?? 1;
  const totalElements = notificationsData?.totalElements ?? 0;

  const handleFilterChange = (e) => {
    setFilter(e.target.value);
    setPage(1);
  };

  const handleTypeFilterChange = (e) => {
    setTypeFilter(e.target.value);
    setPage(1);
  };

  const handleSortChange = (e) => {
    setSortBy(e.target.value);
    setPage(1);
  };

  const handleMarkAsRead = async (id) => {
    try {
      setDeleteError('');
      setSuccessMessage('');
      await markAsReadMutation.mutateAsync(id);
    } catch (err) {
      setDeleteError('Failed to mark notification as read');
      setTimeout(() => setDeleteError(''), 3000);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      setDeleteError('');
      setSuccessMessage('');
      await markAllAsReadMutation.mutateAsync();
      setSuccessMessage('All notifications marked as read');
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err) {
      setDeleteError('Failed to mark all as read');
      setTimeout(() => setDeleteError(''), 3000);
    }
  };

  const handleDelete = async (id) => {
    try {
      setDeleteError('');
      setSuccessMessage('');
      await deleteMutation.mutateAsync(id);
    } catch (err) {
      setDeleteError('Failed to delete notification');
      setTimeout(() => setDeleteError(''), 3000);
    }
  };

  const handleDeleteAll = async () => {
    const confirmed = await confirmDeleteAll();
    if (!confirmed) return;

    try {
      setDeleteError('');
      setSuccessMessage('');
      await deleteAllMutation.mutateAsync();
      setSuccessMessage('All notifications deleted');
      setPage(1);
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err) {
      setDeleteError('Failed to delete all notifications');
      setTimeout(() => setDeleteError(''), 3000);
    }
  };

  const openNotification = (notification) => {
    if (notification?.actionUrl) {
      navigate(notification.actionUrl);
    }
  };

  if (isError) {
    return (
      <div className="text-center py-12">
        <p className="text-error">
          Error loading notifications: {error?.message || 'Unknown error'}
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-display font-bold text-slate-900">
            Notifications
          </h1>
          <p className="text-slate-600 mt-1">
            {unreadCount > 0
              ? `${unreadCount} unread notification${unreadCount !== 1 ? 's' : ''}`
              : 'All caught up!'}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <select
            value={filter}
            onChange={handleFilterChange}
            aria-label="Filter notifications by read status"
            className="px-4 py-2 rounded-lg border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
          >
            <option value="ALL">All</option>
            <option value="UNREAD">Unread</option>
            <option value="READ">Read</option>
          </select>

          <select
            value={typeFilter}
            onChange={handleTypeFilterChange}
            aria-label="Filter notifications by type"
            className="px-4 py-2 rounded-lg border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
          >
            <option value="ALL">All Types</option>
            <option value="WARNING">Warning</option>
            <option value="SUCCESS">Success</option>
            <option value="INFO">Info</option>
            <option value="ERROR">Error</option>
          </select>

          <select
            value={sortBy}
            onChange={handleSortChange}
            aria-label="Sort notifications"
            className="px-4 py-2 rounded-lg border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
          >
            <option value="newest">Newest First</option>
            <option value="type">Type Priority</option>
          </select>

          {totalElements > 0 && (
            <p className="text-sm text-slate-500">
              {totalElements === 1
                ? '1 notification'
                : `${totalElements} notifications`}
            </p>
          )}

          {unreadCount > 0 && (
            <button
              type="button"
              onClick={handleMarkAllAsRead}
              disabled={markAllAsReadMutation.isPending}
              className="px-4 py-2 bg-primary text-white text-sm font-medium rounded-lg hover:bg-primary-dark transition-colors disabled:opacity-50"
            >
              {markAllAsReadMutation.isPending ? 'Marking...' : 'Mark all read'}
            </button>
          )}

          {totalElements > 0 && (
            <button
              type="button"
              onClick={handleDeleteAll}
              disabled={deleteAllMutation.isPending}
              className="px-4 py-2 bg-error text-white text-sm font-medium rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50"
            >
              {deleteAllMutation.isPending ? 'Deleting...' : 'Delete all'}
            </button>
          )}
        </div>
      </div>

      {deleteError && (
        <div className="p-4 bg-error-light text-error rounded-lg">
          {deleteError}
        </div>
      )}

      {successMessage && (
        <div className="p-4 bg-success-light text-success rounded-lg">
          {successMessage}
        </div>
      )}

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
        </div>
      ) : notifications.length === 0 ? (
        <div className="text-center py-12">
          <svg
            className="w-16 h-16 mx-auto text-slate-300"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
            />
          </svg>
          <p className="text-slate-500 mt-4">
            {filter === 'ALL'
              ? 'No notifications'
              : `No ${filter.toLowerCase()} notifications`}
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {notifications.map((notification) => {
            const type = notification.type || 'INFO';
            return (
              <div
                key={notification.id}
                role={notification.actionUrl ? 'button' : undefined}
                tabIndex={notification.actionUrl ? 0 : undefined}
                onClick={() => openNotification(notification)}
                onKeyDown={(event) => {
                  if (
                    notification.actionUrl &&
                    (event.key === 'Enter' || event.key === ' ')
                  ) {
                    event.preventDefault();
                    openNotification(notification);
                  }
                }}
                className={`p-4 rounded-xl border-l-4 shadow-soft ${!notification.isRead ? 'bg-slate-50' : 'bg-white'} ${getTypeBorder(type)} ${
                  !notification.isRead ? 'bg-slate-50' : ''
                } ${notification.actionUrl ? 'cursor-pointer hover:bg-slate-50' : ''}`}
              >
                <div className="flex flex-wrap items-center gap-2 mb-2">
                  <span
                    className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wide ${getTypeStyles(type)}`}
                  >
                    {type}
                  </span>
                  {!notification.isRead && (
                    <span className="w-2 h-2 rounded-full bg-primary flex-shrink-0" />
                  )}
                  <span className="text-xs text-slate-400">
                    {new Date(notification.createdAt).toLocaleString()}
                  </span>
                </div>

                <h3 className="font-semibold text-slate-900">
                  {notification.title}
                </h3>
                <p className="text-sm text-slate-600 mt-1">
                  {notification.message}
                </p>

                <div className="flex flex-wrap items-center gap-3 mt-3">
                  {!notification.isRead && (
                    <button
                      type="button"
                      onClick={(event) => {
                        event.stopPropagation();
                        handleMarkAsRead(notification.id);
                      }}
                      disabled={markAsReadMutation.isPending}
                      className="text-sm text-primary hover:text-primary-dark font-medium"
                    >
                      Mark read
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={(event) => {
                      event.stopPropagation();
                      handleDelete(notification.id);
                    }}
                    disabled={deleteMutation.isPending}
                    className="text-sm text-error hover:text-red-700 font-medium"
                  >
                    Delete
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 sm:gap-4 pt-3 sm:pt-4 border-t border-slate-100">
          <span className="text-xs sm:text-sm text-slate-500 whitespace-nowrap">
            Page {page} of {totalPages}
          </span>
          <PaginationComponent
            page={page}
            totalPages={totalPages}
            onPageChange={setPage}
            pageSize={limit}
            onPageSizeChange={(newLimit) => {
              setLimit(newLimit);
              setPage(1);
            }}
          />
        </div>
      )}
      {deleteAllModal}
    </div>
  );
}

export default NotificationsPage;
