import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from './Button';

/**
 * EmptyState - Professional empty state component
 * @param {string} title - Empty state title
 * @param {string} description - Description text
 * @param {string} actionLabel - CTA button label
 * @param {string} actionHref - CTA button link
 * @param {function} onAction - CTA button callback
 */
function EmptyState({
  title = 'No data found',
  description = 'There are no items to display.',
  actionLabel,
  actionHref,
  onAction,
}) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4 text-center animate-fade-in">
      <div className="w-20 h-20 rounded-full bg-slate-100 flex items-center justify-center mb-6">
        <svg
          className="w-10 h-10 text-slate-400"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
          />
        </svg>
      </div>

      {/* Title */}
      <h3 className="text-xl font-display font-semibold text-slate-800 mb-2">
        {title}
      </h3>

      {/* Description */}
      <p className="text-sm text-slate-500 max-w-md mb-6">{description}</p>

      {/* Action Button */}
      {(actionLabel || onAction) &&
        (actionHref ? (
          <Link to={actionHref}>
            <Button variant="primary">{actionLabel || 'Get Started'}</Button>
          </Link>
        ) : (
          <Button variant="primary" onClick={onAction}>
            {actionLabel || 'Get Started'}
          </Button>
        ))}
    </div>
  );
}

/**
 * ErrorState - Error display component
 */
function ErrorState({
  title = 'Something went wrong',
  message = 'An error occurred while loading this content.',
  onRetry,
}) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4 text-center animate-fade-in">
      <div className="w-20 h-20 rounded-full bg-error-light flex items-center justify-center mb-6">
        <svg
          className="w-10 h-10 text-error"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
          />
        </svg>
      </div>

      {/* Title */}
      <h3 className="text-xl font-display font-semibold text-slate-800 mb-2">
        {title}
      </h3>

      {/* Description */}
      <p className="text-sm text-slate-500 max-w-md mb-6">{message}</p>

      {/* Retry Button */}
      {onRetry && (
        <Button variant="outline" onClick={onRetry}>
          Try Again
        </Button>
      )}
    </div>
  );
}

/**
 * LoadingState - Loading display component
 */
function LoadingState({
  title = 'Loading...',
  message = 'Please wait while we fetch the data.',
}) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
      <div className="w-20 h-20 rounded-full bg-primary-subtle flex items-center justify-center mb-6">
        <svg
          className="w-10 h-10 text-primary animate-spin"
          fill="none"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
          />
        </svg>
      </div>

      {/* Title */}
      <h3 className="text-xl font-display font-semibold text-slate-800 mb-2">
        {title}
      </h3>

      {/* Description */}
      <p className="text-sm text-slate-500 max-w-md">{message}</p>
    </div>
  );
}

/**
 * SuccessState - Success display component
 */
function SuccessState({
  title = 'Success!',
  message = 'Your action was completed successfully.',
  actionLabel,
  onAction,
}) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4 text-center animate-scale-in">
      <div className="w-20 h-20 rounded-full bg-success-light flex items-center justify-center mb-6">
        <svg
          className="w-10 h-10 text-success"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      </div>

      {/* Title */}
      <h3 className="text-xl font-display font-semibold text-slate-800 mb-2">
        {title}
      </h3>

      {/* Description */}
      <p className="text-sm text-slate-500 max-w-md mb-6">{message}</p>

      {/* Action Button */}
      {(actionLabel || onAction) && (
        <Button variant="success" onClick={onAction}>
          {actionLabel || 'Continue'}
        </Button>
      )}
    </div>
  );
}

export { EmptyState, ErrorState, LoadingState, SuccessState };
export default EmptyState;
