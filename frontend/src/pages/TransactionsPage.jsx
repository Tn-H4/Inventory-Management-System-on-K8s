import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { downloadBlob } from '../lib/download';
import { parseSortOption } from '../lib/sortUtils';
import {
  useExportTransactions,
  useTransactions,
} from '../features/transactions/api';
import PaginationComponent from '../components/PaginationComponent';
import { EmptyState, ErrorState, LoadingState } from '../components/EmptyState';
import { LoadingSpinner } from '../components/LoadingSpinner';

// --- Helper Components for Badges ---

const ArrowDownRight = () => (
  <svg
    width="12"
    height="12"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="3"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <line x1="7" y1="7" x2="17" y2="17"></line>
    <polyline points="17 7 17 17 7 17"></polyline>
  </svg>
);

const ArrowUpRight = () => (
  <svg
    width="12"
    height="12"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="3"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <line x1="7" y1="17" x2="17" y2="7"></line>
    <polyline points="7 7 17 7 17 17"></polyline>
  </svg>
);

const TypeBadge = ({ type }) => {
  const styles = {
    PURCHASE: 'bg-error text-white',
    SALE: 'bg-slate-800 text-white',
    RETURN_TO_SUPPLIER: 'bg-warning text-white',
  };

  const icons = {
    PURCHASE: <ArrowDownRight />,
    SALE: <ArrowUpRight />,
    RETURN_TO_SUPPLIER: null,
  };

  const labels = {
    PURCHASE: 'PURCHASE',
    SALE: 'SALE',
    RETURN_TO_SUPPLIER: 'RETURN',
  };

  if (!styles[type]) {
    return <span className="text-sm text-slate-600">{type}</span>;
  }

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border-[3px] border-white px-3 py-1 text-xs font-bold shadow-sm ${styles[type]}`}
    >
      {icons[type]}
      {labels[type]}
    </span>
  );
};

// --- Main Page Component ---

const SORT_OPTIONS = [
  { value: 'created_at:DESC', label: 'Date (newest first)' },
  { value: 'created_at:ASC', label: 'Date (oldest first)' },
  { value: 'total_price:DESC', label: 'Price (high to low)' },
  { value: 'total_price:ASC', label: 'Price (low to high)' },
  { value: 'total_products:DESC', label: 'Quantity (high to low)' },
  { value: 'status:ASC', label: 'Status (A–Z)' },
];

const STATUS_OPTIONS = [
  { value: '', label: 'All Statuses' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'PROCESSING', label: 'Processing' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

function TransactionsPage() {
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [filterType, setFilterType] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('');
  const [sortOption, setSortOption] = useState('created_at:DESC');
  const [exportError, setExportError] = useState('');
  const navigate = useNavigate();

  const userRole = localStorage.getItem('userRole');
  const canExportTransactions = userRole === 'ADMIN' || userRole === 'MANAGER';

  const { sortBy, sortDir } = parseSortOption(sortOption);
  const isSearching = Boolean(search.trim());
  const apiPage = isSearching ? 1 : page;
  const apiLimit = isSearching ? 1000 : limit;

  const { data, isLoading, isError, error } = useTransactions({
    page: apiPage,
    search,
    limit: apiLimit,
    filter: filterType === 'ALL' ? '' : filterType,
    status: statusFilter,
    sortBy,
    sortDir,
  });

  const exportMutation = useExportTransactions();

  const handleFilterChange = (nextFilter) => {
    setFilterType(nextFilter);
    setPage(1);
  };

  const handleSearchSubmit = (event) => {
    event.preventDefault();
    setPage(1);
    setSearch(searchInput.trim());
  };

  const allTransactions = data?.transactions ?? [];
  const filteredTransactions = allTransactions.filter((tx) => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) return true;
    const searchable = [
      tx.id,
      tx.status,
      tx.transactionType,
      tx.product?.name,
      tx.totalPrice,
      tx.totalProducts,
      tx.createdAt ? new Date(tx.createdAt).toLocaleDateString('en-CA') : '',
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    return searchable.includes(keyword);
  });
  const displayedTransactions = isSearching
    ? filteredTransactions.slice((page - 1) * limit, page * limit)
    : filteredTransactions;

  const totalElements = isSearching
    ? filteredTransactions.length
    : (data?.totalElements ?? 0);
  const totalPages = isSearching
    ? Math.max(1, Math.ceil(totalElements / limit))
    : (data?.totalPages ?? 1);
  const startIndex = totalElements === 0 ? 0 : (page - 1) * limit + 1;
  const endIndex =
    totalElements === 0 ? 0 : Math.min(page * limit, totalElements);

  const handleExport = () => {
    setExportError('');
    const filter = filterType === 'ALL' ? '' : filterType;
    exportMutation.mutate(filter, {
      onSuccess: (blob) => {
        downloadBlob(blob, 'transactions.csv');
      },
      onError: (err) => {
        setExportError(err.message || 'Failed to export CSV.');
      },
    });
  };

  return (
    <div className="w-full min-h-[600px] rounded-2xl border border-slate-200 bg-white p-3 sm:p-6 lg:p-8 shadow-soft">
      {/* Header Section */}
      <header className="mb-4 sm:mb-8">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-xl sm:text-2xl font-display font-bold text-slate-900">
              Transactions
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 mt-0.5 sm:mt-1">
              View all purchase and sale transactions
            </p>
          </div>

          {canExportTransactions && (
            <button
              type="button"
              onClick={handleExport}
              disabled={exportMutation.isPending}
              className="w-fit flex items-center gap-1.5 sm:gap-2 px-3 py-2 sm:px-4 sm:py-2.5 text-xs sm:text-sm font-semibold text-white bg-primary hover:bg-primary-dark rounded-lg sm:rounded-xl shadow-soft hover:shadow-card transition-all duration-200 disabled:opacity-60"
            >
              {exportMutation.isPending ? (
                <>
                  <LoadingSpinner size="sm" color="white" />
                  Export...
                </>
              ) : (
                <>
                  <svg
                    className="w-3 h-3 sm:w-4 sm:h-4"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
                    />
                  </svg>
                  Export
                </>
              )}
            </button>
          )}
        </div>
      </header>

      {exportError && (
        <div className="mb-3 sm:mb-4 rounded-lg sm:rounded-xl border-2 border-error bg-error-light px-3 py-2 sm:px-4 sm:py-3 text-xs sm:text-sm text-error font-medium">
          {exportError}
        </div>
      )}

      {/* Search & Filters */}
      <div className="mb-4 sm:mb-6 space-y-3 sm:space-y-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <form onSubmit={handleSearchSubmit} className="flex-1 max-w-md">
            <div className="relative">
              <span className="absolute left-3 sm:left-4 top-1/2 -translate-y-1/2 text-slate-400">
                <svg
                  className="w-4 h-4 sm:w-5 sm:h-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                  />
                </svg>
              </span>
              <input
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Search..."
                aria-label="Search transactions"
                className="w-full pl-10 pr-3 py-2 sm:pl-12 sm:pr-4 sm:py-3 text-xs sm:text-sm bg-slate-50 border-2 border-transparent rounded-lg sm:rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
              />
            </div>
          </form>

          <div className="flex flex-wrap items-center gap-2 sm:gap-3">
            <div className="flex items-center gap-1.5 sm:gap-2 text-xs sm:text-sm text-slate-700">
              <span className="whitespace-nowrap font-medium">Status:</span>
              <select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(1);
                }}
                aria-label="Filter transactions by status"
                className="px-2 py-1.5 sm:px-3 sm:py-2 text-xs sm:text-sm bg-slate-50 border-2 border-transparent rounded-lg sm:rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
              >
                {STATUS_OPTIONS.map((opt) => (
                  <option key={opt.value || 'all'} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex items-center gap-1.5 sm:gap-2 text-xs sm:text-sm text-slate-700">
              <span className="whitespace-nowrap font-medium">Sort:</span>
              <select
                value={sortOption}
                onChange={(e) => {
                  setSortOption(e.target.value);
                  setPage(1);
                }}
                aria-label="Sort transactions by"
                className="px-2 py-1.5 sm:px-3 sm:py-2 text-xs sm:text-sm bg-slate-50 border-2 border-transparent rounded-lg sm:rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
              >
                {SORT_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {/* Filter Tabs */}
        <div className="overflow-x-auto">
          <div className="flex w-fit items-center rounded-full border-2 border-slate-200 bg-slate-50 p-0.5 sm:p-1">
            {[
              { key: 'ALL', label: 'All' },
              { key: 'PURCHASE', label: 'Purchases' },
              { key: 'SALE', label: 'Sales' },
              { key: 'RETURN_TO_SUPPLIER', label: 'Returns' },
            ].map(({ key, label }) => (
              <button
                key={key}
                onClick={() => handleFilterChange(key)}
                aria-pressed={filterType === key}
                className={`rounded-full px-3 py-1.5 sm:px-4 sm:py-2 text-xs sm:text-sm font-medium transition-all ${
                  filterType === key
                    ? 'bg-primary text-white shadow-soft'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Results Count */}
      <div className="mb-3 sm:mb-4 text-xs sm:text-sm text-slate-600">
        Showing {startIndex}–{endIndex} of {totalElements} results
      </div>

      {/* Table Section */}
      {isLoading ? (
        <LoadingState
          title="Loading transactions..."
          message="Please wait while we fetch your data."
        />
      ) : isError ? (
        <ErrorState
          title="Failed to load transactions"
          message={
            error?.message || 'An error occurred while fetching transactions.'
          }
        />
      ) : displayedTransactions.length === 0 ? (
        <EmptyState
          title="No transactions found"
          description={
            search || filterType !== 'ALL' || statusFilter
              ? 'Try adjusting your filters or search query.'
              : 'There are no transactions yet.'
          }
        />
      ) : (
        <>
          {/* Desktop Table */}
          <div className="hidden md:block overflow-x-auto rounded-xl border border-slate-200 shadow-soft">
            <table className="min-w-full divide-y divide-slate-200">
              <thead>
                <tr className="bg-slate-50">
                  <th className="px-6 py-4 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700">
                    Date
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700">
                    Type
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700">
                    Status
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700">
                    Total Products
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700">
                    Total Price
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700">
                    Product
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {displayedTransactions.map((tx) => (
                  <tr
                    key={tx.id}
                    className="hover:bg-slate-50 transition-colors"
                  >
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-900">
                      {new Date(tx.createdAt).toLocaleDateString('en-CA')}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <TypeBadge type={tx.transactionType} />
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <span
                        className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                          tx.status === 'COMPLETED'
                            ? 'bg-success-light text-success'
                            : tx.status === 'PENDING'
                              ? 'bg-warning-light text-warning'
                              : 'bg-slate-100 text-slate-700'
                        }`}
                      >
                        {tx.status.charAt(0) + tx.status.slice(1).toLowerCase()}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                      {tx.totalProducts}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-slate-900">
                      ${tx.totalPrice}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                      {tx.product?.name || 'Unknown Product'}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm">
                      <button
                        onClick={() => navigate(`/transactions/${tx.id}`)}
                        className="font-semibold text-primary hover:text-primary-dark transition-colors"
                      >
                        View Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Cards */}
          <div className="block md:hidden w-full max-w-full overflow-hidden space-y-2">
            {displayedTransactions.map((tx) => (
              <div
                key={tx.id}
                className="w-full rounded-lg border border-slate-200 bg-white p-2.5 shadow-soft"
              >
                {/* Header row: Date + Type + Status */}
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs text-slate-500">
                    {new Date(tx.createdAt).toLocaleDateString('en-CA')}
                  </span>
                  <div className="flex items-center gap-1.5">
                    <span
                      className={`inline-flex items-center gap-0.5 rounded-full px-1.5 py-0.5 text-[10px] font-bold ${
                        tx.transactionType === 'PURCHASE'
                          ? 'bg-error text-white'
                          : tx.transactionType === 'SALE'
                            ? 'bg-slate-800 text-white'
                            : 'bg-warning text-white'
                      }`}
                    >
                      {tx.transactionType === 'PURCHASE'
                        ? 'BUY'
                        : tx.transactionType === 'SALE'
                          ? 'SELL'
                          : 'RET'}
                    </span>
                    <span
                      className={`inline-flex rounded-full px-1.5 py-0.5 text-[10px] font-semibold ${
                        tx.status === 'COMPLETED'
                          ? 'bg-success-light text-success'
                          : tx.status === 'PENDING'
                            ? 'bg-warning-light text-warning'
                            : 'bg-slate-100 text-slate-700'
                      }`}
                    >
                      {tx.status === 'COMPLETED'
                        ? 'Done'
                        : tx.status === 'PENDING'
                          ? 'Pend'
                          : 'Can'}
                    </span>
                  </div>
                </div>

                {/* Product name */}
                <div className="text-xs font-medium text-slate-900 truncate mb-1">
                  {tx.product?.name || 'Unknown Product'}
                </div>

                {/* Details row */}
                <div className="flex items-center justify-between text-xs text-slate-600 mb-2">
                  <span>
                    {tx.totalProducts} item{tx.totalProducts !== 1 ? 's' : ''}
                  </span>
                  <span className="font-semibold text-slate-900">
                    ${tx.totalPrice}
                  </span>
                </div>

                {/* Action button */}
                <button
                  onClick={() => navigate(`/transactions/${tx.id}`)}
                  className="w-full py-1.5 text-xs font-semibold text-primary hover:bg-primary-subtle rounded-md transition-colors"
                >
                  View Details
                </button>
              </div>
            ))}
          </div>
        </>
      )}

      {/* Pagination */}
      {totalPages >= 1 && (
        <div className="mt-4 sm:mt-6 flex justify-center">
          <PaginationComponent
            currentPage={page}
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
    </div>
  );
}

export default TransactionsPage;
