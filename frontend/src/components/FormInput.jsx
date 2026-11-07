import React, { useState, useEffect } from 'react';
import { IconTooltip } from './Tooltip';

/**
 * FormInput - Enhanced form input with validation
 * @param {string} label - Input label
 * @param {string} error - Error message
 * @param {string} hint - Helper text
 * @param {boolean} showSuccess - Show success state
 * @param {string} icon - Icon prefix
 */
function FormInput({
  label,
  error,
  hint,
  showSuccess = false,
  icon,
  id,
  className = '',
  ...props
}) {
  const [isFocused, setIsFocused] = useState(false);
  const [hasValue, setHasValue] = useState(false);

  useEffect(() => {
    setHasValue(!!props.value || !!props.defaultValue);
  }, [props.value, props.defaultValue]);

  const hasError = !!error;
  const isValid = showSuccess && !hasError && hasValue;

  const inputContainerClasses = `
    relative rounded-xl transition-all duration-200
    ${
      hasError
        ? 'bg-error-light border-2 border-error'
        : isValid
          ? 'bg-success-light border-2 border-success'
          : isFocused
            ? 'bg-white border-2 border-primary shadow-focus'
            : 'bg-slate-50 border-2 border-transparent hover:border-slate-300'
    }
  `;

  return (
    <div className={`space-y-1.5 ${className}`}>
      {label && (
        <label
          htmlFor={id}
          className="block text-sm font-semibold text-slate-700"
        >
          {label}
        </label>
      )}

      <div className={inputContainerClasses}>
        {icon && (
          <span className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">
            {icon}
          </span>
        )}

        <input
          id={id}
          {...props}
          className={`
            w-full px-4 py-3 text-sm text-slate-800 bg-transparent outline-none
            placeholder:text-slate-400
            ${icon ? 'pl-12' : ''}
            disabled:opacity-50 disabled:cursor-not-allowed
          `}
          onFocus={(e) => {
            setIsFocused(true);
            props.onFocus?.(e);
          }}
          onBlur={(e) => {
            setIsFocused(false);
            props.onBlur?.(e);
          }}
        />
      </div>

      {/* Error Message */}
      {hasError && (
        <p className="flex items-center gap-1 text-sm text-error font-medium animate-slide-up">
          <svg
            className="w-4 h-4 flex-shrink-0"
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
          {error}
        </p>
      )}

      {/* Success Message */}
      {isValid && (
        <p className="flex items-center gap-1 text-sm text-success font-medium">
          <svg
            className="w-4 h-4 flex-shrink-0"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M5 13l4 4L19 7"
            />
          </svg>
          Looks good!
        </p>
      )}

      {/* Hint Text */}
      {hint && !hasError && !isValid && (
        <p className="flex items-center gap-2 text-xs text-slate-500">
          <IconTooltip content={hint} variant="light">
            ⓘ
          </IconTooltip>
        </p>
      )}
    </div>
  );
}

/**
 * FormTextarea - Enhanced textarea
 */
function FormTextarea({
  label,
  error,
  hint,
  rows = 4,
  id,
  className = '',
  ...props
}) {
  const [isFocused, setIsFocused] = useState(false);
  const hasError = !!error;

  const containerClasses = `
    relative rounded-xl transition-all duration-200
    ${
      hasError
        ? 'bg-error-light border-2 border-error'
        : isFocused
          ? 'bg-white border-2 border-primary shadow-focus'
          : 'bg-slate-50 border-2 border-transparent hover:border-slate-300'
    }
  `;

  return (
    <div className={`space-y-1.5 ${className}`}>
      {label && (
        <label
          htmlFor={id}
          className="block text-sm font-semibold text-slate-700"
        >
          {label}
        </label>
      )}

      <div className={containerClasses}>
        <textarea
          id={id}
          {...props}
          rows={rows}
          className="w-full px-4 py-3 text-sm text-slate-800 bg-transparent outline-none placeholder:text-slate-400 resize-none disabled:opacity-50 disabled:cursor-not-allowed rounded-xl"
          onFocus={(e) => {
            setIsFocused(true);
            props.onFocus?.(e);
          }}
          onBlur={(e) => {
            setIsFocused(false);
            props.onBlur?.(e);
          }}
        />
      </div>

      {hasError && (
        <p className="flex items-center gap-1 text-sm text-error font-medium animate-slide-up">
          <svg
            className="w-4 h-4 flex-shrink-0"
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
          {error}
        </p>
      )}

      {hint && !hasError && <p className="text-xs text-slate-500">{hint}</p>}
    </div>
  );
}

/**
 * FormSelect - Enhanced select dropdown
 */
function FormSelect({
  label,
  error,
  options = [],
  placeholder = 'Select an option',
  id,
  className = '',
  ...props
}) {
  const [isFocused, setIsFocused] = useState(false);
  const hasError = !!error;

  const containerClasses = `
    relative rounded-xl transition-all duration-200
    ${
      hasError
        ? 'bg-error-light border-2 border-error'
        : isFocused
          ? 'bg-white border-2 border-primary shadow-focus'
          : 'bg-slate-50 border-2 border-transparent hover:border-slate-300'
    }
  `;

  return (
    <div className={`space-y-1.5 ${className}`}>
      {label && (
        <label
          htmlFor={id}
          className="block text-sm font-semibold text-slate-700"
        >
          {label}
        </label>
      )}

      <div className={containerClasses}>
        <select
          id={id}
          {...props}
          className="w-full px-4 py-3 text-sm text-slate-800 bg-transparent outline-none appearance-none cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed rounded-xl"
          onFocus={(e) => {
            setIsFocused(true);
            props.onFocus?.(e);
          }}
          onBlur={(e) => {
            setIsFocused(false);
            props.onBlur?.(e);
          }}
        >
          <option value="" disabled>
            {placeholder}
          </option>
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        {/* Custom arrow */}
        <span className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400">
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
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </span>
      </div>

      {hasError && (
        <p className="flex items-center gap-1 text-sm text-error font-medium animate-slide-up">
          <svg
            className="w-4 h-4 flex-shrink-0"
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
          {error}
        </p>
      )}
    </div>
  );
}

/**
 * FormCheckbox - Checkbox with label
 */
function FormCheckbox({ label, error, className = '', ...props }) {
  return (
    <div className={`flex items-start gap-3 ${className}`}>
      <div className="relative flex items-center">
        <input
          type="checkbox"
          {...props}
          className="w-5 h-5 rounded border-2 border-slate-300 text-primary focus:ring-2 focus:ring-primary/30 focus:ring-offset-2 cursor-pointer appearance-none checked:bg-primary checked:border-primary transition-colors"
        />
        <svg
          className="absolute w-4 h-4 text-white pointer-events-none left-0.5 top-0.5 opacity-0 checked:opacity-100 transition-opacity"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={3}
            d="M5 13l4 4L19 7"
          />
        </svg>
      </div>
      {label && (
        <label className="text-sm text-slate-700 cursor-pointer select-none">
          {label}
        </label>
      )}
    </div>
  );
}

export { FormInput, FormTextarea, FormSelect, FormCheckbox };
export default FormInput;
