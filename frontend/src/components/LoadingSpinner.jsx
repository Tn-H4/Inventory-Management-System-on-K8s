import React from 'react';

/**
 * LoadingSpinner - Professional loading indicator
 * @param {string} size - 'sm' | 'md' | 'lg'
 * @param {string} color - 'primary' | 'white' | 'accent'
 * @param {boolean} withText - Show loading text
 * @param {string} text - Custom text
 */
function LoadingSpinner({
  size = 'md',
  color = 'primary',
  withText = false,
  text = 'Loading...',
  className = '',
}) {
  const sizeClasses = {
    sm: 'w-4 h-4 border-2',
    md: 'w-8 h-8 border-3',
    lg: 'w-12 h-12 border-4',
  };

  const colorClasses = {
    primary: 'border-primary/30 border-t-primary',
    white: 'border-white/30 border-t-white',
    accent: 'border-accent/30 border-t-accent',
  };

  return (
    <div className={`flex items-center gap-3 ${className}`}>
      <div
        className={`${sizeClasses[size]} ${colorClasses[color]} rounded-full animate-spin-slow`}
        role="status"
        aria-label="Loading"
      >
        <span className="sr-only">Loading</span>
      </div>
      {withText && (
        <span className="text-sm font-medium text-slate-600 animate-pulse">
          {text}
        </span>
      )}
    </div>
  );
}

/**
 * LoadingOverlay - Full screen loading state
 */
function LoadingOverlay({ isLoading, text = 'Loading...' }) {
  if (!isLoading) return null;

  return (
    <div className="fixed inset-0 z-modal-backdrop bg-white/80 backdrop-blur-sm flex items-center justify-center animate-fade-in">
      <div className="flex flex-col items-center gap-4 p-8 bg-white rounded-2xl shadow-elevated">
        <LoadingSpinner size="lg" withText={false} />
        <p className="text-sm font-medium text-slate-600">{text}</p>
      </div>
    </div>
  );
}

/**
 * PageLoader - Initial page loading state
 */
function PageLoader({ text = 'Loading page...' }) {
  return (
    <div className="min-h-[400px] flex flex-col items-center justify-center gap-4">
      <LoadingSpinner size="lg" color="primary" withText text={text} />
    </div>
  );
}

/**
 * Skeleton - Loading placeholder for content
 */
function Skeleton({ className = '', variant = 'text', width, height }) {
  const variantClasses = {
    text: 'h-4 rounded',
    title: 'h-8 rounded',
    avatar: 'rounded-full',
    card: 'rounded-xl',
    button: 'rounded-lg',
    input: 'rounded-md',
    image: 'rounded-xl',
  };

  const defaultHeights = {
    text: 'h-4',
    title: 'h-8',
    avatar: 'w-12 h-12',
    card: 'h-48',
    button: 'h-10',
    input: 'h-10',
    image: 'h-64',
  };

  return (
    <div
      className={`skeleton ${variantClasses[variant]} ${className}`}
      style={{
        width: width || (variant === 'avatar' ? '3rem' : '100%'),
        height: height || (variant === 'avatar' ? '3rem' : undefined),
      }}
      aria-hidden="true"
    />
  );
}

/**
 * CardSkeleton - Skeleton for card layouts
 */
function CardSkeleton() {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-soft">
      <Skeleton variant="image" className="w-full mb-4" />
      <Skeleton variant="title" className="w-3/4 mb-2" />
      <Skeleton variant="text" className="w-1/2 mb-4" />
      <div className="flex gap-2">
        <Skeleton variant="button" className="flex-1" />
        <Skeleton variant="button" className="w-20" />
      </div>
    </div>
  );
}

export { LoadingSpinner, LoadingOverlay, PageLoader, Skeleton, CardSkeleton };
export default LoadingSpinner;
