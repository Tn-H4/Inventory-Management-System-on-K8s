import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { downloadBlob } from '../lib/download';
import {
  useSuppliers,
  useDeleteSupplier,
  useExportSuppliers,
  useToggleSupplierStatus,
} from '../features/suppliers/api';
import PaginationComponent from '../components/PaginationComponent';
import { useConfirmation } from '../components/ConfirmationModal';
import { EmptyState, ErrorState, LoadingState } from '../components/EmptyState';
import { LoadingSpinner } from '../components/LoadingSpinner';

function SupplierPage() {
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [taxCodeInput, setTaxCodeInput] = useState('');
  const [taxCode, setTaxCode] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState('name');
  const [sortDir, setSortDir] = useState('asc');
  const [hasEmail, setHasEmail] = useState(false);
  const [hasWebsite, setHasWebsite] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const [exportError, setExportError] = useState('');
  const navigate = useNavigate();

  const { data, isLoading, isError, error, isFetching } = useSuppliers({
    page,
    search,
    taxCode,
    status: statusFilter,
    sortBy,
    sortDir,
    limit,
  });

  const deleteMutation = useDeleteSupplier();
  const toggleMutation = useToggleSupplierStatus();
  const exportMutation = useExportSuppliers();

  // Check user role for toggle button visibility
  const userRole = localStorage.getItem('userRole');
  const canToggle = userRole === 'ADMIN' || userRole === 'MANAGER';
  const isAdmin = userRole === 'ADMIN';

  const handleExport = () => {
    setExportError('');
    exportMutation.mutate(
      {
        input: search || undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
      },
      {
        onSuccess: (blob) => {
          downloadBlob(blob, 'suppliers.csv');
        },
        onError: (err) =>
          setExportError(err.message || 'Failed to export CSV.'),
      }
    );
  };

  const handleToggleStatus = (id) => {
    toggleMutation.mutate(id, {
      onError: (err) => {
        setDeleteError(err.message || 'Failed to toggle supplier status');
      },
    });
  };

  // Confirmation modal
  const { confirm, modal: deleteModal } = useConfirmation({
    title: 'Delete Supplier',
    message:
      'Are you sure you want to delete this supplier? This action cannot be undone.',
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
          setDeleteError(err.message || 'Failed to delete supplier');
        },
      });
    }
  };

  const handleSearchSubmit = (event) => {
    event.preventDefault();
    setPage(1);
    setSearch(searchInput.trim());
    setTaxCode(taxCodeInput.trim());
  };

  const columns = useMemo(
    () => [
      { key: 'name', header: 'Name' },
      { key: 'status', header: 'Status' },
      { key: 'contactInfo', header: 'Contact Info' },
      { key: 'address', header: 'Address' },
      { key: 'actions', header: 'Actions' },
    ],
    []
  );

  const allSuppliers = data?.suppliers ?? [];
  const suppliers = allSuppliers.filter((s) => {
    if (hasEmail && !s.email) return false;
    if (hasWebsite && !s.website) return false;
    return true;
  });
  const totalPages = data?.totalPages ?? 1;
  const totalElements = data?.totalElements ?? 0;
  const startIndex = totalElements === 0 ? 0 : (page - 1) * limit + 1;
  const endIndex =
    totalElements === 0 ? 0 : Math.min(page * limit, totalElements);

  return (
    <div className="p-3 sm:p-6 bg-white rounded-2xl shadow-soft space-y-4 sm:space-y-6">
      {/* Header & Search Section */}
      <header className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-b border-slate-100 pb-4 sm:pb-6">
        <div>
          <h1 className="text-xl sm:text-2xl font-display font-bold text-slate-900">
            Suppliers
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5 sm:mt-1">
            Manage your product suppliers
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
            onClick={() => navigate('/suppliers/new')}
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
          <form
            onSubmit={handleSearchSubmit}
            className="flex-1 max-w-2xl flex flex-col sm:flex-row gap-3"
          >
            <div className="relative flex-1">
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
                placeholder="Search suppliers..."
                aria-label="Search suppliers"
                className="w-full pl-10 pr-3 py-2 sm:pl-12 sm:pr-4 sm:py-3 text-xs sm:text-sm bg-slate-50 border-2 border-transparent rounded-lg sm:rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
              />
            </div>
            <div className="relative flex-1">
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
                    d="M9 12h6m-6 4h6m2 5H4a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.586 5.586a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                  />
                </svg>
              </span>
              <input
                value={taxCodeInput}
                onChange={(e) => setTaxCodeInput(e.target.value)}
                placeholder="Tax Code"
                aria-label="Tax Code"
                className="w-full pl-10 pr-3 py-2 sm:pl-12 sm:pr-4 sm:py-3 text-xs sm:text-sm bg-slate-50 border-2 border-transparent rounded-lg sm:rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
              />
            </div>
            <button
              type="submit"
              className="bg-primary text-white px-4 py-2 rounded-lg sm:rounded-xl text-xs sm:text-sm font-semibold hover:bg-primary-dark transition-all"
            >
              Search
            </button>
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
                <option value="email">Sort by Email</option>
                <option value="tax_code">Sort by Tax Code</option>
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

        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs sm:text-sm text-slate-600">
          <label className="flex items-center gap-1.5 sm:gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={hasEmail}
              onChange={(e) => {
                setHasEmail(e.target.checked);
                setPage(1);
              }}
              className="w-3 h-3 sm:w-4 sm:h-4 rounded border-slate-300 text-primary focus:ring-primary/20"
            />
            <span className="font-medium">Has email</span>
          </label>
          <label className="flex items-center gap-1.5 sm:gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={hasWebsite}
              onChange={(e) => {
                setHasWebsite(e.target.checked);
                setPage(1);
              }}
              className="w-3 h-3 sm:w-4 sm:h-4 rounded border-slate-300 text-primary focus:ring-primary/20"
            />
            <span className="font-medium">Has website</span>
          </label>
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
          title="Loading suppliers..."
          message="Please wait while we fetch your data."
        />
      ) : isError ? (
        <ErrorState
          title="Failed to load suppliers"
          message={
            error?.message || 'An error occurred while fetching suppliers.'
          }
        />
      ) : suppliers.length === 0 ? (
        <EmptyState
          title="No suppliers found"
          description={
            search || statusFilter !== 'ALL' || hasEmail || hasWebsite
              ? 'Try adjusting your filters or search query.'
              : 'Get started by adding your first supplier.'
          }
          actionLabel={
            !search && statusFilter === 'ALL' && !hasEmail && !hasWebsite
              ? 'Add Supplier'
              : undefined
          }
          actionHref={
            !search && statusFilter === 'ALL' && !hasEmail && !hasWebsite
              ? '/suppliers/new'
              : undefined
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
                {suppliers.map((supplier) => (
                  <tr
                    key={supplier.id}
                    className="hover:bg-slate-50 transition-colors"
                  >
                    <td className="px-6 py-4 text-sm font-medium text-slate-900">
                      {supplier.name}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      <span
                        className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                          supplier.status === 'ACTIVE'
                            ? 'bg-success-light text-success'
                            : 'bg-error-light text-error'
                        }`}
                      >
                        {supplier.status || 'ACTIVE'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-600">
                      {supplier.contactInfo}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-600">
                      {supplier.address}
                    </td>
                    <td className="px-6 py-4 text-sm font-medium flex gap-3">
                      {canToggle && (
                        <button
                          onClick={() => handleToggleStatus(supplier.id)}
                          disabled={toggleMutation.isPending}
                          aria-label={`Toggle ${supplier.name} status`}
                          className="text-slate-600 hover:text-slate-900 transition-colors disabled:opacity-50"
                        >
                          {toggleMutation.isPending ? (
                            <LoadingSpinner size="sm" />
                          ) : (
                            'Toggle Status'
                          )}
                        </button>
                      )}
                      <button
                        onClick={() =>
                          navigate(`/suppliers/${supplier.id}/edit`)
                        }
                        aria-label={`Edit ${supplier.name}`}
                        className="text-primary hover:text-primary-dark transition-colors"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(supplier.id)}
                        disabled={deleteMutation.isPending}
                        aria-label={`Delete ${supplier.name}`}
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
            {suppliers.map((supplier) => (
              <div
                key={supplier.id}
                className="rounded-lg border border-slate-200 bg-white p-3 shadow-soft"
              >
                <div className="space-y-1">
                  <div className="flex justify-between">
                    <span className="text-xs text-slate-500">Name</span>
                    <span className="text-xs font-medium text-slate-900 truncate max-w-[160px]">
                      {supplier.name}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-xs text-slate-500">Contact</span>
                    <span className="text-xs text-slate-700 truncate max-w-[160px]">
                      {supplier.contactInfo}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-xs text-slate-500">Address</span>
                    <span className="text-xs text-slate-700 truncate max-w-[160px]">
                      {supplier.address}
                    </span>
                  </div>
                </div>
                <div className="mt-3 pt-2 border-t border-slate-100 flex gap-2">
                  <button
                    onClick={() => navigate(`/suppliers/${supplier.id}/edit`)}
                    aria-label={`Edit ${supplier.name}`}
                    className="flex-1 py-1.5 text-xs font-semibold text-primary hover:text-primary-dark bg-primary-subtle rounded-md transition-colors"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(supplier.id)}
                    disabled={deleteMutation.isPending}
                    aria-label={`Delete ${supplier.name}`}
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

export default SupplierPage;
