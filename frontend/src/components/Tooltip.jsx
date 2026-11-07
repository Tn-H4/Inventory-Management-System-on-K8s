import React, { useState, useRef } from 'react';

/**
 * Tooltip - Professional tooltip component
 * @param {string} content - Tooltip text
 * @param {string} position - 'top' | 'bottom' | 'left' | 'right'
 * @param {string} variant - 'default' | 'dark' | 'light'
 * @param {ReactNode} children - Trigger element
 */
function Tooltip({ content, position = 'top', variant = 'dark', children }) {
  const [isVisible, setIsVisible] = useState(false);
  const timeoutRef = useRef(null);

  const showTooltip = () => {
    clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => setIsVisible(true), 200);
  };

  const hideTooltip = () => {
    clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => setIsVisible(false), 150);
  };

  const positionClasses = {
    top: 'bottom-full left-1/2 -translate-x-1/2 mb-2',
    bottom: 'top-full left-1/2 -translate-x-1/2 mt-2',
    left: 'right-full top-1/2 -translate-y-1/2 mr-2',
    right: 'left-full top-1/2 -translate-y-1/2 ml-2',
  };

  const variantClasses = {
    dark: 'bg-slate-800 text-white',
    light: 'bg-white text-slate-700 border border-slate-200 shadow-card',
    default: 'bg-slate-800 text-white',
  };

  const arrowClasses = {
    top: 'top-full left-1/2 -translate-x-1/2 border-t-slate-800',
    bottom: 'bottom-full left-1/2 -translate-x-1/2 border-b-slate-800',
    left: 'left-full top-1/2 -translate-y-1/2 border-l-slate-800',
    right: 'right-full top-1/2 -translate-y-1/2 border-r-slate-800',
  };

  return (
    <span
      className="relative inline-flex"
      onMouseEnter={showTooltip}
      onMouseLeave={hideTooltip}
      onFocus={showTooltip}
      onBlur={hideTooltip}
    >
      {children}

      {isVisible && content && (
        <span
          role="tooltip"
          className={`
            absolute z-tooltip px-3 py-1.5 text-xs font-medium rounded-lg
            whitespace-nowrap animate-fade-in
            ${positionClasses[position]}
            ${variantClasses[variant]}
          `}
        >
          {content}
          {/* Arrow */}
          <span
            className={`
              absolute w-0 h-0 border-4 border-transparent
              ${arrowClasses[position]}
              ${variant === 'light' ? 'border-t-white' : ''}
            `}
          />
        </span>
      )}
    </span>
  );
}

/**
 * IconTooltip - Tooltip with icon trigger
 */
function IconTooltip({
  content,
  position = 'top',
  icon = 'ⓘ',
  className = '',
  variant = 'dark',
}) {
  return (
    <Tooltip content={content} position={position} variant={variant}>
      <span
        className={`
          inline-flex items-center justify-center w-5 h-5
          text-xs font-medium text-slate-500 cursor-help
          hover:text-primary transition-colors
          ${className}
        `}
      >
        {icon}
      </span>
    </Tooltip>
  );
}

/**
 * TooltipProvider - Context provider for tooltips
 */
const TooltipContext = React.createContext({
  showTooltip: () => {},
  hideTooltip: () => {},
});

function TooltipProvider({ children }) {
  const [tooltip, setTooltip] = useState({
    isVisible: false,
    content: '',
    position: 'top',
    variant: 'dark',
  });

  const showTooltip = (content, position = 'top', variant = 'dark') => {
    setTooltip({ isVisible: true, content, position, variant });
  };

  const hideTooltip = () => {
    setTooltip({ ...tooltip, isVisible: false });
  };

  return (
    <TooltipContext.Provider value={{ showTooltip, hideTooltip }}>
      {children}
    </TooltipContext.Provider>
  );
}

export { Tooltip, IconTooltip, TooltipProvider };
export default Tooltip;
