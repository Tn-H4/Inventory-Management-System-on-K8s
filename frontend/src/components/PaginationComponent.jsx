import React, { useMemo, useState } from 'react';

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50, 100];
const MAX_VISIBLE_PAGES = 3;

function PaginationComponent({
  currentPage,
  totalPages,
  onPageChange,
  pageSize,
  onPageSizeChange,
}) {
  const [jumpToPage, setJumpToPage] = useState('');

  // Generate visible page numbers with ellipsis (Discord-style)
  const visiblePages = useMemo(() => {
    if (totalPages <= MAX_VISIBLE_PAGES) {
      return Array.from({ length: totalPages }, (_, i) => i + 1);
    }

    const pages = [];
    const half = Math.floor(MAX_VISIBLE_PAGES / 2);

    let start = currentPage - half;
    let end = currentPage + half;

    // Adjust if at edges
    if (start <= 0) {
      start = 1;
      end = MAX_VISIBLE_PAGES;
    } else if (end > totalPages) {
      end = totalPages;
      start = totalPages - MAX_VISIBLE_PAGES + 1;
    }

    // Add first page and ellipsis if needed
    if (start > 1) {
      pages.push(1);
      if (start > 2) {
        pages.push('...');
      }
    }

    // Add middle pages
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }

    // Add last page and ellipsis if needed
    if (end < totalPages) {
      if (end < totalPages - 1) {
        pages.push('...');
      }
      pages.push(totalPages);
    }

    return pages;
  }, [currentPage, totalPages]);

  const handleJumpToPage = (e) => {
    e.preventDefault();
    const page = parseInt(jumpToPage, 10);
    if (page >= 1 && page <= totalPages) {
      onPageChange(page);
      setJumpToPage('');
    }
  };

  // Compact classes for mobile, full for desktop
  const baseButtonClass =
    'rounded-md border border-slate-200 bg-white px-1.5 py-1 sm:px-3 sm:py-2 text-xs sm:text-sm font-medium text-[#776B6B] shadow-sm disabled:cursor-not-allowed disabled:opacity-60 hover:bg-slate-50 min-w-[1.75rem] sm:min-w-[2rem]';
  const activeButtonClass =
    'rounded-md border border-[#776B6B] bg-[#776B6B] px-1.5 py-1 sm:px-3 sm:py-2 text-xs sm:text-sm font-medium text-white shadow-sm min-w-[1.75rem] sm:min-w-[2rem]';
  const ellipsisClass = 'px-0.5 text-xs sm:text-sm text-[#776B6B] font-medium';

  return (
    <nav
      aria-label="Pagination"
      className="w-full flex flex-col sm:flex-row items-center justify-center gap-2 sm:gap-4 overflow-hidden"
    >
      {/* Page Size Selector - hidden on mobile */}
      {onPageSizeChange && (
        <div className="hidden sm:flex items-center gap-1.5 sm:gap-2">
          <span className="text-xs sm:text-sm text-[#776B6B]">Show</span>
          <select
            value={pageSize}
            onChange={(e) => onPageSizeChange(Number(e.target.value))}
            aria-label="Items per page"
            className="rounded-md border border-slate-200 bg-white px-2 py-1.5 sm:py-2 text-xs sm:text-sm font-medium text-[#776B6B] shadow-sm focus:outline-none focus:ring-2 focus:ring-[#776B6B]"
          >
            {PAGE_SIZE_OPTIONS.map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
          <span className="text-xs sm:text-sm text-[#776B6B]">per page</span>
        </div>
      )}

      {/* Page Navigation - Compact on mobile */}
      <div className="flex items-center gap-0.5 sm:gap-2">
        <button
          type="button"
          aria-label="Previous page"
          className={`${baseButtonClass}`}
          disabled={currentPage === 1}
          onClick={() => onPageChange(currentPage - 1)}
        >
          &laquo; Prev
        </button>

        {visiblePages.map((page, index) =>
          page === '...' ? (
            <span key={`ellipsis-${index}`} className={ellipsisClass}>
              ...
            </span>
          ) : (
            <button
              key={page}
              type="button"
              aria-label={`Go to page ${page}`}
              aria-current={currentPage === page ? 'page' : undefined}
              className={
                currentPage === page ? activeButtonClass : baseButtonClass
              }
              onClick={() => onPageChange(page)}
            >
              {page}
            </button>
          )
        )}

        <button
          type="button"
          aria-label="Next page"
          className={`${baseButtonClass}`}
          disabled={currentPage === totalPages}
          onClick={() => onPageChange(currentPage + 1)}
        >
          Next &raquo;
        </button>

        {/* Jump to page input - hidden on mobile */}
        {totalPages > MAX_VISIBLE_PAGES && (
          <form
            onSubmit={handleJumpToPage}
            className="hidden sm:flex items-center gap-1 ml-1"
          >
            <input
              type="number"
              min={1}
              max={totalPages}
              value={jumpToPage}
              onChange={(e) => setJumpToPage(e.target.value)}
              placeholder="#"
              aria-label="Jump to page number"
              className="w-12 sm:w-14 rounded-md border border-slate-200 bg-white px-2 py-1.5 text-xs sm:text-sm font-medium text-[#776B6B] shadow-sm focus:outline-none focus:ring-2 focus:ring-[#776B6B]"
            />
            <button
              type="submit"
              className={`${baseButtonClass} whitespace-nowrap`}
              disabled={!jumpToPage}
            >
              Go
            </button>
          </form>
        )}
      </div>
    </nav>
  );
}

export default PaginationComponent;
