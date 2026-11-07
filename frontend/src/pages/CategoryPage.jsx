import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { downloadBlob } from '../lib/download';
import {
  useCategories,
  useDeleteCategory,
  useExportCategories,
  useToggleCategoryStatus,
} from '../features/categories/api';
import PaginationComponent from '../components/PaginationComponent';
import { useConfirmation } from '../components/ConfirmationModal';
import { EmptyState, ErrorState, LoadingState } from '../components/EmptyState';
import { LoadingSpinner } from '../components/LoadingSpinner';

function getCategories(payload) {
  if (!payload) return [];
  if (Array.isArray(payload.categories)) return payload.categories;
  if (Array.isArray(payload.content)) return payload.content;
  if (Array.isArray(payload)) return payload;
  return [];
}

function getcategorieactive(category) {
  if (!category) return 'INACTIVE';

  if (typeof category.active === 'string') {
    return category.active.toUpperCase() === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE';
  }

  if (typeof category.isActive === 'boolean') {
    return category.isActive ? 'ACTIVE' : 'INACTIVE';
  }

  if (typeof category.isActive === 'string') {
    return category.isActive.toUpperCase() === 'INACTIVE'
      ? 'INACTIVE'
      : 'ACTIVE';
  }

  return 'INACTIVE';
}

function CategoryPage() {
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [descriptionSearch, setDescriptionSearch] = useState('');
  const [descriptionInput, setDescriptionInput] = useState('');
  const [activeFilter, setActiveFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState('display_order');
  const [sortDir, setSortDir] = useState('asc');
  const [deleteError, setDeleteError] = useState('');
  const [exportError, setExportError] = useState('');
  const navigate = useNavigate();

  const { data, isLoading, isError, error, isFetching } = useCategories({
    page,
    search,
    description: descriptionSearch,
    status: activeFilter,
    sortBy,
    sortDir,
    limit,
  });

  const deleteMutation = useDeleteCategory();
  const toggleMutation = useToggleCategoryStatus();
  const exportMutation = useExportCategories();

  // Check user role for toggle button visibility
  const userRole = localStorage.getItem('userRole');
  const canToggle = userRole === 'ADMIN' || userRole === 'MANAGER';
  const isAdmin = userRole === 'ADMIN';

  const handleExport = () => {
    setExportError('');
    exportMutation.mutate(
      { search: search || undefined },
      {
        onSuccess: (blob) => {
          downloadBlob(blob, 'categories.csv');
        },
        onError: (err) =>
          setExportError(err.message || 'Failed to export CSV.'),
      }
    );
  };

  const handleToggleStatus = (id) => {
    toggleMutation.mutate(id, {
      onError: (err) => {
        setDeleteError(err.message || 'Failed to toggle category status');
      },
    });
  };

  const { confirm, modal: deleteModal } = useConfirmation({
    title: 'Delete Category',
    message:
      'Are you sure you want to delete this category? This action cannot be undone.',
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
          setDeleteError(err.message || 'Failed to delete category');
        },
      });
    }
  };

  const handleSearchSubmit = (event) => {
    event.preventDefault();
    setPage(1);
    setSearch(searchInput.trim());
    setDescriptionSearch(descriptionInput.trim());
  };

  const columns = useMemo(
    () => [
      { key: 'name', header: 'Name' },
      { key: 'description', header: 'Description' },
      { key: 'status', header: 'Status' },
      { key: 'actions', header: 'Actions' },
    ],
    []
  );

  const rawCategories = data?.content || data?.categories || [];
  const categories = rawCategories;

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
            Categories
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5 sm:mt-1">
            Manage your product categories
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
            onClick={() => navigate('/categories/add')}
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
                placeholder="Search by name..."
                aria-label="Search categories by name"
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
                value={descriptionInput}
                onChange={(e) => setDescriptionInput(e.target.value)}
                placeholder="Search by description..."
                aria-label="Search categories by description"
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
            <div className="flex gap-1 rounded-lg border border-slate-200 bg-slate-100 p-1">
              {[
                ['ALL', 'All'],
                ['ACTIVE', 'Active'],
                ['INACTIVE', 'Inactive'],
              ].map(([val, label]) => (
                <button
                  key={val}
                  type="button"
                  onClick={() => {
                    setActiveFilter(val);
                    setPage(1);
                  }}
                  aria-pressed={activeFilter === val}
                  className={`rounded-md px-3 py-1 text-xs font-medium transition-all ${
                    activeFilter === val
                      ? 'bg-white text-slate-900 shadow-sm'
                      : 'text-slate-500 hover:text-slate-700'
                  }`}
                >
                  {label}
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
                className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-700"
              >
                <option value="name">Sort by Name</option>
                <option value="description">Sort by Description</option>
                <option value="display_order">Sort by Order</option>
                <option value="created_at">Sort by Date</option>
              </select>
              <select
                value={sortDir}
                onChange={(e) => {
                  setSortDir(e.target.value);
                  setPage(1);
                }}
                className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-700"
              >
                <option value="asc">Ascending</option>
                <option value="desc">Descending</option>
              </select>
            </div>
          </div>
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
          title="Loading categories..."
          message="Please wait while we fetch your data."
        />
      ) : isError ? (
        <ErrorState
          title="Failed to load categories"
          message={
            error?.message || 'An error occurred while fetching categories.'
          }
        />
      ) : categories.length === 0 ? (
        <EmptyState
          title="No categories found"
          description={
            search || activeFilter !== 'ALL'
              ? 'Try adjusting your filters or search query.'
              : 'Get started by adding your first category.'
          }
          actionLabel={
            !search && activeFilter === 'ALL' ? 'Add Category' : undefined
          }
          actionHref={
            !search && activeFilter === 'ALL' ? '/categories/add' : undefined
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
                {categories.map((category) => (
                  <tr
                    key={category.id}
                    className="hover:bg-slate-50 transition-colors"
                  >
                    <td className="px-6 py-4 text-sm font-medium text-slate-900">
                      {category.name}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-600">
                      {category.description}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      {getcategorieactive(category) === 'ACTIVE' ? (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                          Active
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-600">
                          Inactive
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm font-medium flex gap-3">
                      <button
                        onClick={() =>
                          navigate(`/categories/${category.id}/edit`)
                        }
                        aria-label={`Edit ${category.name}`}
                        className="text-primary hover:text-primary-dark transition-colors"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(category.id)}
                        disabled={deleteMutation.isPending}
                        aria-label={`Delete ${category.name}`}
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
            {categories.map((category) => (
              <div
                key={category.id}
                className="rounded-lg border border-slate-200 bg-white p-3 shadow-soft"
              >
                <div className="space-y-1">
                  <div className="flex justify-between">
                    <span className="text-xs text-slate-500">Name</span>
                    <span className="text-xs font-medium text-slate-900 truncate max-w-[160px]">
                      {category.name}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-xs text-slate-500">Description</span>
                    <span className="text-xs text-slate-700 truncate max-w-[160px]">
                      {category.description}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-xs text-slate-500">Status</span>
                    {getcategorieactive(category) === 'ACTIVE' ? (
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                        Active
                      </span>
                    ) : (
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-600">
                        Inactive
                      </span>
                    )}
                  </div>
                </div>
                <div className="mt-3 pt-2 border-t border-slate-100 flex gap-2">
                  <button
                    onClick={() => navigate(`/categories/${category.id}/edit`)}
                    aria-label={`Edit ${category.name}`}
                    className="flex-1 py-1.5 text-xs font-semibold text-primary hover:text-primary-dark bg-primary-subtle rounded-md transition-colors"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(category.id)}
                    disabled={deleteMutation.isPending}
                    aria-label={`Delete ${category.name}`}
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
          <span className="text-xs sm:text-sm text-slate-500">
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

export default CategoryPage;
