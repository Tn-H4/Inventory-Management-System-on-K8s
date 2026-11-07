import React, { useState, useRef, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useNotifications,
  useMarkAsRead,
  useMarkAllAsRead,
  useDeleteNotification,
} from './api';
import { getTypeStyles, getTypeBorder } from './utils';

const MAX_UNREAD_DISPLAY = 9;
const TOAST_DURATION_MS = 8000;

function NotificationBell({ variant = 'dark', dropdownPosition = 'auto' }) {
  const className =
    variant === 'light'
      ? '!text-slate-700 hover:!bg-slate-100 hover:!text-slate-900'
      : '';
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [showToast, setShowToast] = useState(false);
  const [toastNotification, setToastNotification] = useState(null);
  const dropdownRef = useRef(null);
  const prevUnreadRef = useRef(null);
  const notificationsLoadedRef = useRef(false);
  const isInitialLoadCompleteRef = useRef(false);
  const toastTimerRef = useRef(null);

  const { data: notificationsData, isLoading } = useNotifications();
  const markAsReadMutation = useMarkAsRead();
  const markAllAsReadMutation = useMarkAllAsRead();
  const deleteMutation = useDeleteNotification();

  const unreadCount = notificationsData?.unreadCount ?? 0;
  const notifications = useMemo(
    () => notificationsData?.notifications ?? [],
    [notificationsData?.notifications]
  );

  // Get the most recent unread notification for toast
  const latestUnreadNotification = useMemo(() => {
    return notifications.find((n) => !n.isRead);
  }, [notifications]);

  // Show toast when a new unread notification arrives from the server stream.
  useEffect(() => {
    // Skip if still loading
    if (isLoading) return;

    // Initialize on first data load
    if (!notificationsLoadedRef.current && notifications.length > 0) {
      notificationsLoadedRef.current = true;
      prevUnreadRef.current = unreadCount;
      return;
    }

    // Skip if initial load is not yet complete
    if (!isInitialLoadCompleteRef.current) {
      isInitialLoadCompleteRef.current = true;
      prevUnreadRef.current = unreadCount;
      return;
    }

    // Skip if not yet initialized
    if (prevUnreadRef.current === null) {
      prevUnreadRef.current = unreadCount;
      return;
    }

    if (unreadCount > prevUnreadRef.current && latestUnreadNotification) {
      // Clear any existing timer before creating new one
      if (toastTimerRef.current) {
        clearTimeout(toastTimerRef.current);
      }
      setToastNotification(latestUnreadNotification);
      setShowToast(true);
      toastTimerRef.current = setTimeout(() => {
        setShowToast(false);
        toastTimerRef.current = null;
      }, TOAST_DURATION_MS);
    }
    prevUnreadRef.current = unreadCount;
  }, [unreadCount, latestUnreadNotification, isLoading, notifications.length]);

  // Cleanup timer on unmount
  useEffect(() => {
    return () => {
      if (toastTimerRef.current) {
        clearTimeout(toastTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleMarkAsRead = async (id) => {
    await markAsReadMutation.mutateAsync(id);
  };

  const handleMarkAllAsRead = async () => {
    await markAllAsReadMutation.mutateAsync();
  };

  const handleDelete = async (id) => {
    await deleteMutation.mutateAsync(id);
  };

  const openNotification = (notification) => {
    setShowToast(false);
    setIsOpen(false);
    if (notification?.actionUrl) {
      navigate(notification.actionUrl);
    } else {
      navigate('/notifications');
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className={`relative p-2 rounded-lg text-white/80 hover:bg-white/10 hover:text-white transition-colors ${
          unreadCount > 0 ? 'animate-pulse-slow' : ''
        } ${className}`}
        aria-label="Notifications"
      >
        <svg
          className={`w-5 h-5 ${unreadCount > 0 ? (variant === 'light' ? 'text-slate-700' : 'text-white drop-shadow-md') : variant === 'light' ? 'text-slate-600' : 'text-white/80'}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
          />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 min-w-[1.25rem] h-5 px-1 bg-error text-white text-xs font-bold rounded-full flex items-center justify-center animate-pulse ring-2 ring-error/30">
            {unreadCount > MAX_UNREAD_DISPLAY
              ? `${MAX_UNREAD_DISPLAY}+`
              : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 lg:left-0 lg:right-auto mt-2 w-[min(20rem,calc(100vw-2rem))] lg:w-80 bg-white rounded-xl shadow-lg border border-slate-200 z-50 max-h-96 overflow-hidden text-left">
          <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100">
            <h3 className="font-semibold text-slate-900">Notifications</h3>
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={handleMarkAllAsRead}
                disabled={markAllAsReadMutation.isPending}
                className="text-xs text-primary hover:text-primary-dark font-medium shrink-0"
              >
                Mark all read
              </button>
            )}
          </div>

          <div className="overflow-y-auto max-h-72">
            {isLoading ? (
              <div className="p-4 text-left text-slate-500">Loading...</div>
            ) : notifications.length === 0 ? (
              <div className="p-4 text-left text-slate-500">
                No notifications
              </div>
            ) : (
              notifications.map((notification) => {
                const type = notification.type || 'INFO';
                return (
                  <div
                    key={notification.id}
                    role="button"
                    tabIndex={0}
                    onClick={() => openNotification(notification)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        openNotification(notification);
                      }
                    }}
                    className={`px-4 py-3 border-b border-slate-50 text-left transition-colors border-l-4 ${getTypeBorder(type)} ${
                      !notification.isRead
                        ? 'bg-slate-50/80'
                        : 'hover:bg-slate-50'
                    } cursor-pointer`}
                  >
                    <div className="flex flex-wrap items-center gap-2 mb-1.5">
                      <span
                        className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${getTypeStyles(type)}`}
                      >
                        {type}
                      </span>
                      {!notification.isRead && (
                        <span
                          className="w-1.5 h-1.5 rounded-full bg-primary flex-shrink-0"
                          aria-hidden="true"
                        />
                      )}
                    </div>

                    <p className="text-sm font-medium text-slate-900 text-left">
                      {notification.title}
                    </p>
                    <p className="text-xs text-slate-600 mt-1 line-clamp-3 text-left">
                      {notification.message}
                    </p>
                    <p className="text-xs text-slate-400 mt-1 text-left">
                      {new Date(notification.createdAt).toLocaleString()}
                    </p>

                    <div className="flex items-center gap-3 mt-2">
                      {!notification.isRead && (
                        <button
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            handleMarkAsRead(notification.id);
                          }}
                          disabled={markAsReadMutation.isPending}
                          className="text-xs text-primary hover:text-primary-dark font-medium"
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
                        className="text-xs text-error hover:text-red-700 font-medium"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {notifications.length > 0 && (
            <div className="border-t border-slate-100 px-4 py-3">
              <button
                type="button"
                onClick={() => {
                  setIsOpen(false);
                  navigate('/notifications');
                }}
                className="w-full text-center text-sm text-primary hover:text-primary-dark font-medium"
              >
                View All Notifications
              </button>
            </div>
          )}
        </div>
      )}

      {/* Toast notification popup */}
      {showToast && toastNotification && (
        <div className="fixed top-4 right-4 z-50 animate-slide-in">
          <div
            className={`max-w-sm p-4 rounded-lg shadow-xl border-l-4 cursor-pointer ring-1 ring-slate-900/5 ${
              getTypeBorder(toastNotification.type) || 'border-primary'
            } bg-white`}
            onClick={() => openNotification(toastNotification)}
          >
            <div className="flex items-start gap-3">
              <div className="flex-shrink-0">
                <svg
                  className="w-5 h-5 text-primary"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
                  />
                </svg>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-slate-900">
                  {toastNotification.title}
                </p>
                <p className="text-xs text-slate-600 mt-1 line-clamp-2">
                  {toastNotification.message}
                </p>
              </div>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  setShowToast(false);
                }}
                className="flex-shrink-0 text-slate-400 hover:text-slate-600"
              >
                <svg
                  className="w-4 h-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
            <p className="text-xs text-slate-400 mt-2">Click to view</p>
          </div>
        </div>
      )}
    </div>
  );
}

export default NotificationBell;
