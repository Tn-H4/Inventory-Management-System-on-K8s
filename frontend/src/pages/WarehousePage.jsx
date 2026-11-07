import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { downloadBlob } from '../lib/download';
import {
  useWarehouses,
  useDeleteWarehouse,
  useExportWarehouses,
  useToggleWarehouseStatus,
} from '../features/warehouses/api';
import PaginationComponent from '../components/PaginationComponent';
import { useConfirmation } from '../components/ConfirmationModal';
import { EmptyState, ErrorState, LoadingState } from '../components/EmptyState';
import { LoadingSpinner } from '../components/LoadingSpinner';

function getWarehouses(payload) {
  if (!payload) return [];
  if (Array.isArray(payload.warehouses)) return payload.warehouses;
  if (Array.isArray(payload.content)) return payload.content;
  if (Array.isArray(payload)) return payload;
  return [];
}

function getWarehouseStatus(warehouse) {
  if (!warehouse) return 'INACTIVE';

  if (typeof warehouse.status === 'string') {
    return warehouse.status.toUpperCase() === 'INACTIVE'
      ? 'INACTIVE'
      : 'ACTIVE';
  }

  if (typeof warehouse.isActive === 'boolean') {
    return warehouse.isActive ? 'ACTIVE' : 'INACTIVE';
  }

  if (typeof warehouse.isActive === 'string') {
    return warehouse.isActive.toUpperCase() === 'INACTIVE'
      ? 'INACTIVE'
      : 'ACTIVE';
  }

  return 'INACTIVE';
}

function WarehousePage() {
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [minCapacityInput, setMinCapacityInput] = useState('');
  const [maxCapacityInput, setMaxCapacityInput] = useState('');
  const [minCapacity, setMinCapacity] = useState('');
  const [maxCapacity, setMaxCapacity] = useState('');
  const [sortBy, setSortBy] = useState('name');
  const [sortDir, setSortDir] = useState('asc');
  const [deleteError, setDeleteError] = useState('');
  const [exportError, setExportError] = useState('');
  const navigate = useNavigate();

  const { data, isLoading, isError, error, isFetching } = useWarehouses({
    page,
    search,
    status: statusFilter,
    minCapacity: minCapacity === '' ? undefined : parseInt(minCapacity, 10),
    maxCapacity: maxCapacity === '' ? undefined : parseInt(maxCapacity, 10),
    sortBy,
    sortDir,
    limit,
  });

  const deleteMutation = useDeleteWarehouse();
  const toggleMutation = useToggleWarehouseStatus();
  const exportMutation = useExportWarehouses();

  // Check user role for toggle button visibility
  const userRole = localStorage.getItem('userRole');
  const canToggle = userRole === 'ADMIN' || userRole === 'MANAGER';
  const isAdmin = userRole === 'ADMIN';

  const handleExport = () => {
    setExportError('');
    exportMutation.mutate(undefined, {
      onSuccess: (blob) => {
        downloadBlob(blob, 'warehouses.csv');
      },
      onError: (err) => setExportError(err.message || 'Failed to export CSV.'),
    });
  };

  const handleToggleStatus = (id) => {
    toggleMutation.mutate(id, {
      onError: (err) => {
        setDeleteError(err.message || 'Failed to toggle warehouse status');
      },
    });
  };

  const { confirm, modal: deleteModal } = useConfirmation({
    title: 'Delete Warehouse',
    message:
      'Are you sure you want to delete this warehouse? This action cannot be undone.',
    confirmText: 'Delete',
    cancelText: 'Cancel',
    type: 'danger',
  });

  const handleDelete = async (id) => {
    setDeleteError('');
    const confirmed = await confirm();
    if (confirmed) {
      deleteMutation.mutate(id, {
        onSuccess: () => setDeleteError(''),
        onError: (err) => {
          setDeleteError(err.message || 'Failed to delete warehouse');
        },
      });
    }
  };

  const handleSearchSubmit = (event) => {
    event.preventDefault();
    setPage(1);
    setSearch(searchInput.trim());
  };

  const handleCapacitySubmit = (e) => {
    e.preventDefault();
    setMinCapacity(minCapacityInput);
    setMaxCapacity(maxCapacityInput);
    setPage(1);
  };

  const columns = useMemo(
    () => [
      { key: 'name', header: 'Name' },
      { key: 'location', header: 'Location' },
      { key: 'address', header: 'Address' },
      { key: 'capacity', header: 'Capacity' },
      { key: 'managerName', header: "Manager's Name" },
      { key: 'actions', header: 'Actions' },
    ],
    []
  );

  const allWarehouses = getWarehouses(data);
  const warehouses = allWarehouses;

  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 1;
  const paginatedWarehouses = warehouses;
  const startIndex = totalElements === 0 ? 0 : (page - 1) * limit + 1;
  const endIndex =
    totalElements === 0 ? 0 : Math.min(page * limit, totalElements);

  return (
    <div className="p-3 sm:p-6 bg-white rounded-2xl shadow-soft space-y-4 sm:space-y-6">
      {/* Header & Search Section */}
      <header className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-b border-slate-100 pb-4 sm:pb-6">
        <div>
          <h1 className="text-xl sm:text-2xl font-display font-bold text-slate-900">
            Warehouses
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5 sm:mt-1">
            Manage your product warehouses
          </p>
        </div>

        <div className="flex items-center gap-2">
          {isAdmin && (
            <button
              type="button"
              onClick={handleExport}
              disabled={exportMutation.isPending}
              className="flex items-center gap-1.5 sm:gap-2 text-slate-700 bg-white border-2 border-slate-200 hover:border-primary hover:text-primary px-3 py-2 sm:px-4 sm:py-2.5 rounded-lg sm:rounded-xl text-xs sm:text-sm font-semibold transition-all duration-200 disabled:opacity-60"
            >
              {exportMutation.isPending ? 'Exporting…' : 'Export CSV'}
            </button>
          )}
          <button
            onClick={() => navigate('/warehouses/new')}
            className="flex items-center gap-1.5 sm:gap-2 bg-primary hover:bg-primary-dark text-white px-4 py-2 sm:px-5 sm:py-2.5 rounded-lg sm:rounded-xl text-xs sm:text-sm font-semibold shadow-soft hover:shadow-card transition-all duration-200"
          >
            <span className="text-base sm:text-lg font-bold">+</span>
            Add
          </button>
        </div>
      </header>
      {exportError && (
        <div className="rounded-lg sm:rounded-xl border-2 border-error bg-error-light px-3 py-2 sm:px-4 sm:py-3 text-xs sm:text-sm text-error font-medium">
          {exportError}
        </div>
      )}

      {/* Filters */}
      <div className="space-y-3 sm:space-y-4">
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
                placeholder="Search warehouses..."
                aria-label="Search warehouses"
                className="w-full pl-10 pr-3 py-2 sm:pl-12 sm:pr-4 sm:py-3 text-xs sm:text-sm bg-slate-50 border-2 border-transparent rounded-lg sm:rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
              />
            </div>
          </form>

          <div className="flex flex-col sm:flex-row items-center gap-3">
            <div className="flex items-center gap-1.5 sm:gap-2 rounded-full border-2 border-slate-200 bg-slate-50 p-0.5 sm:p-1">
              {['ALL', 'ACTIVE', 'INACTIVE'].map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => {
                    setStatusFilter(s);
                    setPage(1);
                  }}
                  aria-pressed={statusFilter === s}
                  className={`rounded-full px-3 py-1.5 sm:px-4 sm:py-2 text-xs sm:text-sm font-medium transition-all ${
                    statusFilter === s
                      ? 'bg-primary text-white shadow-soft'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  {s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
            <div className="flex items-center gap-2">
              <select
                value={sortBy}
                onChange={(e) => {
                  setSortBy(e.target.value);
                  setPage(1);
                }}
                className="text-xs sm:text-sm bg-slate-50 border-2 border-slate-200 rounded-lg sm:rounded-xl px-2 py-1.5 focus:border-primary focus:outline-none"
              >
                <option value="name">Sort by Name</option>
                <option value="location">Sort by Location</option>
                <option value="capacity">Sort by Capacity</option>
                <option value="manager_name">Sort by Manager</option>
                <option value="created_at">Sort by Date</option>
              </select>
              <select
                value={sortDir}
                onChange={(e) => {
                  setSortDir(e.target.value);
                  setPage(1);
                }}
                className="text-xs sm:text-sm bg-slate-50 border-2 border-slate-200 rounded-lg sm:rounded-xl px-2 py-1.5 focus:border-primary focus:outline-none"
              >
                <option value="asc">Ascending</option>
                <option value="desc">Descending</option>
              </select>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-x-3 gap-y-2 text-xs sm:text-sm text-slate-600">
          <form
            onSubmit={handleCapacitySubmit}
            className="flex items-center gap-2"
          >
            <span className="font-medium">Capacity:</span>
            <input
              type="number"
              value={minCapacityInput}
              onChange={(e) => setMinCapacityInput(e.target.value)}
              placeholder="Min"
              className="w-20 px-2 py-1 border border-slate-200 rounded-md focus:border-primary focus:outline-none"
            />
            <span className="text-slate-400">—</span>
            <input
              type="number"
              value={maxCapacityInput}
              onChange={(e) => setMaxCapacityInput(e.target.value)}
              placeholder="Max"
              className="w-20 px-2 py-1 border border-slate-200 rounded-md focus:border-primary focus:outline-none"
            />
          </form>
        </div>
      </div>

      {/* Results Count */}
      <div className="mb-3 sm:mb-4 text-xs sm:text-sm text-slate-600">
        Showing {startIndex}–{endIndex} of {totalElements} results
      </div>

      {deleteError && (
        <div className="rounded-lg sm:rounded-xl border-2 border-error bg-error-light px-3 py-2 sm:px-4 sm:py-3 text-xs sm:text-sm text-error font-medium">
          {deleteError}
        </div>
      )}

      {/* Table Section */}
      {isLoading ? (
        <LoadingState
          title="Loading warehouses..."
          message="Please wait while we fetch your data."
        />
      ) : isError ? (
        <ErrorState
          title="Failed to load warehouses"
          message={
            error?.message || 'An error occurred while fetching warehouses.'
          }
        />
      ) : warehouses.length === 0 ? (
        <EmptyState
          title="No warehouses found"
          description={
            search || statusFilter !== 'ALL'
              ? 'Try adjusting your filters or search query.'
              : 'Get started by adding your first warehouse.'
          }
          actionLabel={
            !search && statusFilter === 'ALL' ? 'Add Warehouse' : undefined
          }
          actionHref={
            !search && statusFilter === 'ALL' ? '/warehouses/new' : undefined
          }
        />
      ) : (
        <>
          {/* Desktop Table */}
          <div className="hidden md:block overflow-x-auto rounded-xl border border-slate-200 shadow-soft">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  {columns.map((column) => (
                    <th
                      key={column.key}
                      className="px-6 py-4 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700"
                    >
                      {column.header}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-slate-200">
                {paginatedWarehouses.map((warehouse) => (
                  <tr
                    key={warehouse.id}
                    className="hover:bg-slate-50 transition-colors"
                  >
                    <td className="px-6 py-4 text-sm font-medium text-slate-900">
                      {warehouse.name}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-600">
                      {warehouse.location}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-600">
                      {warehouse.address}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-600">
                      {warehouse.capacity}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-600">
                      {warehouse.managerName}
                    </td>
                    <td className="px-6 py-4 text-sm font-medium flex gap-3">
                      <button
                        onClick={() =>
                          navigate(`/warehouses/${warehouse.id}/edit`)
                        }
                        aria-label={`Edit ${warehouse.name}`}
                        className="text-primary hover:text-primary-dark transition-colors"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(warehouse.id)}
                        disabled={deleteMutation.isPending}
                        aria-label={`Delete ${warehouse.name}`}
                        className="text-error hover:text-red-800 transition-colors disabled:opacity-50"
                      >
                        {deleteMutation.isPending ? (
                          <LoadingSpinner size="sm" />
                        ) : (
                          'Delete'
                        )}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Cards */}
          <div className="block md:hidden space-y-3">
            {paginatedWarehouses.map((warehouse) => (
              <div
                key={warehouse.id}
                className="rounded-lg border border-slate-200 bg-white p-3 shadow-soft"
              >
                <div className="space-y-1">
                  <div className="flex justify-between">
                    <span className="text-xs text-slate-500">Name</span>
                    <span className="text-xs font-medium text-slate-900 truncate max-w-[160px]">
                      {warehouse.name}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-xs text-slate-500">Location</span>
                    <span className="text-xs text-slate-700 truncate max-w-[160px]">
                      {warehouse.location}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-xs text-slate-500">Address</span>
                    <span className="text-xs text-slate-700 truncate max-w-[160px]">
                      {warehouse.address}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-xs text-slate-500">Capacity</span>
                    <span className="text-xs text-slate-700 truncate max-w-[160px]">
                      {warehouse.capacity}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-xs text-slate-500">
                      Manager's name
                    </span>
                    <span className="text-xs text-slate-700 truncate max-w-[160px]">
                      {warehouse.managerName}
                    </span>
                  </div>
                </div>
                <div className="mt-3 pt-2 border-t border-slate-100 flex gap-2">
                  <button
                    onClick={() => navigate(`/warehouses/${warehouse.id}/edit`)}
                    aria-label={`Edit ${warehouse.name}`}
                    className="flex-1 py-1.5 text-xs font-semibold text-primary hover:text-primary-dark bg-primary-subtle rounded-md transition-colors"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(warehouse.id)}
                    disabled={deleteMutation.isPending}
                    aria-label={`Delete ${warehouse.name}`}
                    className="flex-1 py-1.5 text-xs font-semibold text-error hover:bg-error-light rounded-md transition-colors disabled:opacity-50"
                  >
                    {deleteMutation.isPending ? (
                      <LoadingSpinner size="sm" />
                    ) : (
                      'Delete'
                    )}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {/* Footer Pagination */}
      {totalPages > 1 && (
        <footer className="flex flex-col sm:flex-row items-center justify-between gap-3 sm:gap-4 pt-3 sm:pt-4 border-t border-slate-100">
          <span className="text-xs sm:text-sm text-slate-500 whitespace-nowrap">
            Page {page} of {totalPages}
            {isFetching && !isLoading && (
              <span className="ml-1.5 sm:ml-2 text-primary animate-pulse">
                (Updating...)
              </span>
            )}
          </span>
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
        </footer>
      )}

      {/* Delete Confirmation Modal */}
      {deleteModal}
    </div>
  );
}

export default WarehousePage;
