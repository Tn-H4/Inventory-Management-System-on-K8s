import React, { useEffect, useRef, useState } from 'react';

/**
 * Searchable combobox. Controlled via value/onChange.
 * Integrates with React Hook Form using <Controller>.
 *
 * Props:
 *   options: [{ value, label, meta? }]
 *   value: number | string
 *   onChange: (value) => void
 *   placeholder: string
 *   label: string
 *   id?: string
 *   renderOption?: (option, isHighlighted) => JSX
 *   disabled?: boolean
 *   error?: string
 */
function SearchableSelect({
  options = [],
  value,
  onChange,
  placeholder = 'Select...',
  label,
  id,
  renderOption,
  disabled = false,
  error,
}) {
  const [query, setQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [highlighted, setHighlighted] = useState(-1);
  const containerRef = useRef(null);
  const inputRef = useRef(null);
  const listRef = useRef(null);

  const selectedOption = options.find((o) => o.value === value);

  const filtered = options.filter((o) =>
    o.label.toLowerCase().includes(query.toLowerCase())
  );

  // Close dropdown on outside click
  useEffect(() => {
    function onClickOutside(e) {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpen(false);
        setQuery('');
        setHighlighted(-1);
      }
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  // Reset highlight when filter changes
  useEffect(() => {
    setHighlighted(-1);
  }, [query]);

  // Scroll highlighted item into view
  useEffect(() => {
    if (highlighted >= 0 && listRef.current) {
      listRef.current.children[highlighted]?.scrollIntoView({
        block: 'nearest',
      });
    }
  }, [highlighted]);

  function openDropdown() {
    if (!disabled) {
      setIsOpen(true);
      setQuery('');
    }
  }

  function handleSelect(option) {
    onChange(option.value);
    setIsOpen(false);
    setQuery('');
    setHighlighted(-1);
  }

  function handleKeyDown(e) {
    if (!isOpen) {
      if (e.key === 'ArrowDown' || e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        setIsOpen(true);
      }
      return;
    }

    if (e.key === 'Escape') {
      setIsOpen(false);
      setQuery('');
      setHighlighted(-1);
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlighted((h) => Math.min(h + 1, filtered.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlighted((h) => Math.max(h - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (highlighted >= 0 && filtered[highlighted]) {
        handleSelect(filtered[highlighted]);
      }
    }
  }

  // Show query while typing, selected label otherwise
  const displayText = isOpen ? query : (selectedOption?.label ?? '');

  return (
    <div className="space-y-2" ref={containerRef}>
      {label && (
        <label
          htmlFor={id}
          className="block text-sm font-medium text-slate-700"
        >
          {label}
        </label>
      )}
      <div className="relative">
        <input
          ref={inputRef}
          id={id}
          type="text"
          role="combobox"
          aria-expanded={isOpen}
          aria-haspopup="listbox"
          aria-autocomplete="list"
          aria-invalid={error ? 'true' : 'false'}
          autoComplete="off"
          disabled={disabled}
          value={displayText}
          placeholder={placeholder}
          onFocus={openDropdown}
          onClick={openDropdown}
          onChange={(e) => {
            setQuery(e.target.value);
            setIsOpen(true);
          }}
          onKeyDown={handleKeyDown}
          className={`w-full rounded-xl bg-slate-50 border-2 px-4 py-3 text-sm transition-all focus:bg-white focus:ring-2 focus:ring-primary/20 disabled:cursor-not-allowed disabled:opacity-50 ${
            error
              ? 'border-red-400 focus:border-red-400 focus:ring-red-200'
              : 'border-transparent focus:border-primary'
          }`}
        />
        <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">
          <svg
            className="h-4 w-4"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </span>

        {isOpen && (
          <ul
            ref={listRef}
            role="listbox"
            className="absolute z-50 w-full bg-white border border-slate-200 rounded-xl shadow-lg max-h-60 overflow-y-auto mt-1"
          >
            {filtered.length === 0 ? (
              <li className="px-4 py-3 text-sm text-slate-400">
                No options found
              </li>
            ) : (
              filtered.map((option, index) => (
                <li
                  key={option.value}
                  role="option"
                  aria-selected={option.value === value}
                  onMouseDown={(e) => {
                    e.preventDefault(); // prevent blur before selection
                    handleSelect(option);
                  }}
                  className={`px-4 py-2.5 text-sm cursor-pointer transition-colors ${
                    index === highlighted
                      ? 'bg-primary/10 text-primary'
                      : option.value === value
                        ? 'bg-slate-100 text-slate-900'
                        : 'text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  {renderOption
                    ? renderOption(option, index === highlighted)
                    : option.label}
                </li>
              ))
            )}
          </ul>
        )}
      </div>
      {error && (
        <p className="text-sm text-red-600" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

export default SearchableSelect;
