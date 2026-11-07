import React, { useEffect, useRef } from 'react';

/**
 * ConfirmationModal - Professional confirmation dialog
 * @param {boolean} isOpen - Modal visibility
 * @param {string} title - Modal title
 * @param {string} message - Modal message
 * @param {string} confirmText - Confirm button text
 * @param {string} cancelText - Cancel button text
 * @param {string} type - 'danger' | 'warning' | 'info' | 'success'
 * @param {function} onConfirm - Confirm callback
 * @param {function} onCancel - Cancel callback
 */
function ConfirmationModal({
  isOpen = false,
  title = 'Confirm Action',
  message = 'Are you sure you want to proceed?',
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  type = 'danger',
  onConfirm = () => {},
  onCancel = () => {},
}) {
  const modalRef = useRef(null);
  const cancelButtonRef = useRef(null);

  // Handle escape key
  useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onCancel();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [isOpen, onCancel]);

  // Focus management
  useEffect(() => {
    if (isOpen) {
      // Focus the cancel button by default for safety
      setTimeout(() => {
        cancelButtonRef.current?.focus();
      }, 100);
    }
  }, [isOpen]);

  // Prevent body scroll when modal is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const typeStyles = {
    danger: {
      icon: (
        <svg
          className="w-7 h-7"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M6 18L18 6M6 6l12 12"
          />
        </svg>
      ),
      iconBg: 'bg-error-light',
      iconText: 'text-error',
      confirmBtn: 'bg-error hover:bg-red-700 text-white',
    },
    warning: {
      icon: (
        <svg
          className="w-7 h-7"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
          />
        </svg>
      ),
      iconBg: 'bg-warning-light',
      iconText: 'text-warning',
      confirmBtn: 'bg-warning hover:bg-amber-700 text-white',
    },
    info: {
      icon: (
        <svg
          className="w-7 h-7"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      ),
      iconBg: 'bg-info-light',
      iconText: 'text-info',
      confirmBtn: 'bg-info hover:bg-sky-700 text-white',
    },
    success: {
      icon: (
        <svg
          className="w-7 h-7"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      ),
      iconBg: 'bg-success-light',
      iconText: 'text-success',
      confirmBtn: 'bg-success hover:bg-emerald-700 text-white',
    },
  };

  const styles = typeStyles[type];

  return (
    <div
      className="fixed inset-0 z-modal-backdrop flex items-center justify-center p-4 animate-fade-in"
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
      aria-describedby="modal-description"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm"
        onClick={onCancel}
        aria-hidden="true"
      />

      {/* Modal Content */}
      <div
        ref={modalRef}
        className="relative w-full max-w-md bg-white rounded-2xl shadow-elevated animate-scale-in"
      >
        {/* Icon */}
        <div className="flex justify-center -mt-6">
          <div
            className={`${styles.iconBg} ${styles.iconText} w-14 h-14 rounded-full flex items-center justify-center shadow-soft`}
          >
            {styles.icon}
          </div>
        </div>

        {/* Content */}
        <div className="p-6 pt-4">
          <h2
            id="modal-title"
            className="text-xl font-display font-semibold text-slate-900 text-center mb-2"
          >
            {title}
          </h2>
          <p
            id="modal-description"
            className="text-sm text-slate-600 text-center mb-6"
          >
            {message}
          </p>

          {/* Actions */}
          <div className="flex gap-3">
            <button
              ref={cancelButtonRef}
              onClick={onCancel}
              className="flex-1 px-4 py-3 text-sm font-semibold text-slate-700 bg-slate-100 rounded-lg hover:bg-slate-200 focus:ring-2 focus:ring-slate-400/30 transition-colors"
            >
              {cancelText}
            </button>
            <button
              onClick={onConfirm}
              className={`flex-1 px-4 py-3 text-sm font-semibold rounded-lg ${styles.confirmBtn} focus:ring-2 focus:ring-offset-2 transition-colors`}
            >
              {confirmText}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * useConfirmation - Hook for managing confirmation modal state
 */
function useConfirmation({
  title = 'Confirm Action',
  message = 'Are you sure you want to proceed?',
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  type = 'danger',
} = {}) {
  const [isOpen, setIsOpen] = React.useState(false);
  const [promise, setPromise] = React.useState(null);

  const confirm = () => {
    return new Promise((resolve) => {
      setPromise({ resolve });
      setIsOpen(true);
    });
  };

  const handleConfirm = () => {
    promise?.resolve(true);
    setIsOpen(false);
    setPromise(null);
  };

  const handleCancel = () => {
    promise?.resolve(false);
    setIsOpen(false);
    setPromise(null);
  };

  const modal = (
    <ConfirmationModal
      isOpen={isOpen}
      title={title}
      message={message}
      confirmText={confirmText}
      cancelText={cancelText}
      type={type}
      onConfirm={handleConfirm}
      onCancel={handleCancel}
    />
  );

  return { confirm, modal };
}

export { ConfirmationModal, useConfirmation };
export default ConfirmationModal;
