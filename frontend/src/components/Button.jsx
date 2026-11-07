import React from 'react';
import { LoadingSpinner } from './LoadingSpinner';

/**
 * Button - Enhanced button component with loading state
 * @param {string} variant - 'primary' | 'secondary' | 'danger' | 'ghost' | 'outline'
 * @param {string} size - 'sm' | 'md' | 'lg'
 * @param {boolean} isLoading - Show loading state
 * @param {boolean} fullWidth - Full width button
 * @param {ReactNode} children - Button content
 */
function Button({
  variant = 'primary',
  size = 'md',
  isLoading = false,
  fullWidth = false,
  disabled,
  children,
  className = '',
  ...props
}) {
  const baseClasses =
    'inline-flex items-center justify-center font-semibold transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed';

  const variantClasses = {
    primary:
      'bg-primary hover:bg-primary-dark text-white focus:ring-primary/50 shadow-soft hover:shadow-card',
    secondary:
      'bg-slate-700 hover:bg-slate-800 text-white focus:ring-slate-500/50',
    danger: 'bg-error hover:bg-red-700 text-white focus:ring-error/50',
    ghost:
      'bg-transparent hover:bg-slate-100 text-slate-700 focus:ring-slate-400/50',
    outline:
      'border-2 border-slate-300 hover:border-slate-400 text-slate-700 hover:bg-slate-50 focus:ring-slate-400/50',
    success: 'bg-success hover:bg-emerald-700 text-white focus:ring-success/50',
  };

  const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm rounded-md gap-1.5',
    md: 'px-4 py-2.5 text-sm rounded-lg gap-2',
    lg: 'px-6 py-3 text-base rounded-xl gap-2.5',
  };

  return (
    <button
      className={`
        ${baseClasses}
        ${variantClasses[variant]}
        ${sizeClasses[size]}
        ${fullWidth ? 'w-full' : ''}
        ${className}
      `}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading && (
        <LoadingSpinner
          size="sm"
          color={
            variant === 'ghost' || variant === 'outline' ? 'primary' : 'white'
          }
        />
      )}
      {children}
    </button>
  );
}

/**
 * IconButton - Button with only icon
 */
function IconButton({
  variant = 'ghost',
  size = 'md',
  isLoading = false,
  disabled,
  children,
  className = '',
  ...props
}) {
  const baseClasses =
    'inline-flex items-center justify-center font-semibold transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed rounded-lg';

  const variantClasses = {
    primary:
      'bg-primary hover:bg-primary-dark text-white focus:ring-primary/50',
    secondary:
      'bg-slate-700 hover:bg-slate-800 text-white focus:ring-slate-500/50',
    danger: 'bg-error hover:bg-red-700 text-white focus:ring-error/50',
    ghost:
      'bg-transparent hover:bg-slate-100 text-slate-600 focus:ring-slate-400/50',
    outline:
      'border border-slate-300 hover:border-slate-400 text-slate-600 hover:bg-slate-50 focus:ring-slate-400/50',
    success: 'bg-success hover:bg-emerald-700 text-white focus:ring-success/50',
  };

  const sizeClasses = {
    sm: 'w-8 h-8',
    md: 'w-10 h-10',
    lg: 'w-12 h-12',
  };

  return (
    <button
      className={`
        ${baseClasses}
        ${variantClasses[variant]}
        ${sizeClasses[size]}
        ${className}
      `}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <LoadingSpinner
          size="sm"
          color={
            variant === 'ghost' || variant === 'outline' ? 'primary' : 'white'
          }
        />
      ) : (
        children
      )}
    </button>
  );
}

/**
 * FloatingActionButton - FAB for primary actions
 */
function FloatingActionButton({
  onClick,
  icon = '+',
  label = 'Add',
  className = '',
}) {
  return (
    <button
      onClick={onClick}
      className={`
        fixed bottom-6 right-6 z-30
        flex items-center gap-2
        px-5 py-3
        bg-primary hover:bg-primary-dark
        text-white font-semibold
        rounded-full shadow-elevated
        hover:shadow-xl
        transition-all duration-200
        hover:scale-105 active:scale-95
        focus:outline-none focus:ring-2 focus:ring-primary/50
        ${className}
      `}
    >
      <span className="text-xl font-bold">{icon}</span>
      <span>{label}</span>
    </button>
  );
}

export { Button, IconButton, FloatingActionButton };
export default Button;
