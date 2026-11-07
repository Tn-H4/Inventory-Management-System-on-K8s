import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { downloadBlob } from '../lib/download';
import { parseSortOption } from '../lib/sortUtils';
import { highlightMatch } from '../lib/textUtils';
import { useDebounce } from '../hooks/useDebounce';
import {
  useProducts,
  useDeleteProduct,
  useExportProducts,
} from '../features/products/api';
import { useCategories } from '../features/categories/api';
import { useWarehouses } from '../features/warehouses/api';
import {
  getProductStockStatus,
  STOCK_STATUS_OPTIONS,
} from '../features/products/stockStatus';
import PaginationComponent from '../components/PaginationComponent';
import { useConfirmation } from '../components/ConfirmationModal';
import { EmptyState, ErrorState } from '../components/EmptyState';
import { CardSkeleton } from '../components/LoadingSpinner';
import { resolveMediaUrl } from '../lib/media';

const StockBadge = ({ product }) => {
  const status = getProductStockStatus(product);

  if (status === 'OUT_OF_STOCK') {
    return (
      <span className="ml-2 inline-flex items-center gap-1 rounded-full bg-error-light px-2.5 py-0.5 text-xs font-semibold text-error">
        <span className="w-1.5 h-1.5 rounded-full bg-error" />
        Out of Stock
      </span>
    );
  }

  if (status === 'LOW_STOCK') {
    return (
      <span className="ml-2 inline-flex items-center gap-1 rounded-full bg-warning-light px-2.5 py-0.5 text-xs font-semibold text-warning">
        <span className="w-1.5 h-1.5 rounded-full bg-warning" />
        Low Stock
      </span>
    );
  }

  return null;
};

const SORT_OPTIONS = [
  { value: 'id:ASC', label: 'Default' },
  { value: 'name:ASC', label: 'Name A-Z' },
  { value: 'name:DESC', label: 'Name Z-A' },
  { value: 'price:ASC', label: 'Price low to high' },
  { value: 'price:DESC', label: 'Price high to low' },
  { value: 'stock_quantity:ASC', label: 'Stock low to high' },
  { value: 'stock_quantity:DESC', label: 'Stock high to low' },
];

function ProductPage() {
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(9);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [stockStatusFilter, setStockStatusFilter] = useState('');
  const [selectedWarehouse, setSelectedWarehouse] = useState('');
  const [sortOption, setSortOption] = useState('id:ASC');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [deleteError, setDeleteError] = useState('');
  const [deletingId, setDeletingId] = useState(null);
  const [exportError, setExportError] = useState('');
  const navigate = useNavigate();

  const debouncedSearch = useDebounce(searchInput.trim());

  useEffect(() => {
    setPage(1);
    setSearch(debouncedSearch);
  }, [debouncedSearch]);

  const userRole = localStorage.getItem('userRole');
  const canExportProducts = userRole === 'ADMIN' || userRole === 'MANAGER';

  const exportMutation = useExportProducts();
  const { sortBy, sortDir } = parseSortOption(sortOption);

  const handleExport = () => {
    setExportError('');
    exportMutation.mutate(
      {
        categoryId: selectedCategory || undefined,
        warehouseId: selectedWarehouse || undefined,
        stockStatus: stockStatusFilter || undefined,
        search: search || undefined,
        minPrice: minPrice || undefined,
        maxPrice: maxPrice || undefined,
      },
      {
        onSuccess: (blob) => {
          downloadBlob(blob, 'products.csv');
        },
        onError: (err) =>
          setExportError(err.message || 'Failed to export CSV.'),
      }
    );
  };

  const { data: categoriesData } = useCategories({ limit: 100 });
  const categories = categoriesData?.categories || [];

  const { data: warehousesData } = useWarehouses({ limit: 100 });
  const warehouses = warehousesData?.warehouses ?? [];

  const { data, isLoading, isError, error, isFetching } = useProducts({
    page,
    search,
    limit,
    categoryId: selectedCategory || null,
    warehouseId: selectedWarehouse || null,
    stockStatus: stockStatusFilter || null,
    minPrice: minPrice ? parseFloat(minPrice) : null,
    maxPrice: maxPrice ? parseFloat(maxPrice) : null,
    sortBy,
    sortDir,
  });

  const deleteMutation = useDeleteProduct();

  // Confirmation modal
  const { confirm, modal: deleteModal } = useConfirmation({
    title: 'Delete Product',
    message:
      'Are you sure you want to delete this product? This action cannot be undone.',
    confirmText: 'Delete',
    cancelText: 'Cancel',
    type: 'danger',
  });

  const handleDelete = async (product) => {
    setDeleteError('');
    const confirmed = await confirm();
    if (confirmed) {
      setDeletingId(product.id);
      // Delay to ensure loading state is visible (300ms minimum)
      await new Promise((resolve) => setTimeout(resolve, 300));
      deleteMutation.mutate(product.id, {
        onSuccess: () => {
          setDeleteError('');
          setDeletingId(null);
        },
        onError: (err) => {
          setDeleteError(err.message || 'Failed to delete product');
          setDeletingId(null);
        },
      });
    }
  };

  const hasActiveFilters =
    Boolean(search) ||
    Boolean(selectedCategory) ||
    Boolean(stockStatusFilter) ||
    Boolean(selectedWarehouse) ||
    Boolean(minPrice) ||
    Boolean(maxPrice);

  const totalElements = data?.totalElements ?? data?.products?.length ?? 0;
  const totalPages = data?.totalPages ?? 0;
  const products = data?.products ?? [];

  const startIndex = totalElements === 0 ? 0 : (page - 1) * limit + 1;
  const endIndex =
    totalElements === 0 ? 0 : Math.min(page * limit, totalElements);

  return (
    <div className="w-full">
      {/* Header */}
      <header className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6 sm:mb-8">
        <div>
          <h1 className="text-2xl font-display font-bold text-slate-900">
            Products
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Manage your product inventory
          </p>
        </div>
        <div className="flex items-center gap-2">
          {canExportProducts && (
            <button
              type="button"
              onClick={handleExport}
              disabled={exportMutation.isPending}
              className="flex items-center gap-2 px-4 py-2.5 min-h-[44px] text-sm font-semibold text-slate-700 bg-white border-2 border-slate-200 hover:border-primary hover:text-primary rounded-xl transition-all duration-200 disabled:opacity-60"
            >
              {exportMutation.isPending ? 'Exporting…' : 'Export CSV'}
            </button>
          )}
          <button
            onClick={() => navigate('/products/new')}
            className="flex items-center gap-2 px-5 py-2.5 min-h-[44px] text-sm font-semibold text-white bg-primary hover:bg-primary-dark rounded-xl shadow-soft hover:shadow-card transition-all duration-200"
          >
            <span className="text-lg font-bold">+</span>
            Add Product
          </button>
        </div>
      </header>
      {exportError && (
        <div className="mb-4 rounded-xl border-2 border-error bg-error-light px-4 py-3 text-sm text-error font-medium">
          {exportError}
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between mb-8">
        <div className="flex-1 max-w-md">
          <div className="relative">
            <span className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">
              <svg
                className="w-5 h-5"
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
              placeholder="Search products by name or SKU..."
              aria-label="Search products"
              className="w-full pl-12 pr-4 py-3 text-sm bg-slate-50 border-2 border-transparent rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
            />
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2">
            <input
              type="number"
              value={minPrice}
              onChange={(e) => {
                setMinPrice(e.target.value);
                setPage(1);
              }}
              placeholder="Min Price"
              aria-label="Minimum price"
              className="w-24 px-4 py-2.5 text-sm bg-slate-50 border-2 border-transparent rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
            />
            <span className="text-slate-400 text-sm">—</span>
            <input
              type="number"
              value={maxPrice}
              onChange={(e) => {
                setMaxPrice(e.target.value);
                setPage(1);
              }}
              placeholder="Max Price"
              aria-label="Maximum price"
              className="w-24 px-4 py-2.5 text-sm bg-slate-50 border-2 border-transparent rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
            />
          </div>
          <select
            value={sortOption}
            onChange={(e) => {
              setSortOption(e.target.value);
              setPage(1);
            }}
            aria-label="Sort products"
            className="px-4 py-2.5 text-sm bg-slate-50 border-2 border-transparent rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
          >
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <select
            value={stockStatusFilter}
            onChange={(e) => {
              setStockStatusFilter(e.target.value);
              setPage(1);
            }}
            aria-label="Filter by stock status"
            className="px-4 py-2.5 text-sm bg-slate-50 border-2 border-transparent rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
          >
            {STOCK_STATUS_OPTIONS.map((option) => (
              <option key={option.value || 'all'} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <select
            value={selectedWarehouse}
            onChange={(e) => {
              setSelectedWarehouse(e.target.value);
              setPage(1);
            }}
            aria-label="Filter by warehouse"
            className="px-4 py-2.5 text-sm bg-slate-50 border-2 border-transparent rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
          >
            <option value="">All Warehouses</option>
            {warehouses.map((warehouse) => (
              <option key={warehouse.id} value={warehouse.id}>
                {warehouse.name}
              </option>
            ))}
          </select>

          <select
            value={selectedCategory}
            onChange={(e) => {
              setSelectedCategory(e.target.value);
              setPage(1);
            }}
            aria-label="Filter by category"
            className="px-4 py-2.5 text-sm bg-slate-50 border-2 border-transparent rounded-xl focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
          >
            <option value="">All Categories</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Results Count */}
      <div className="mb-6 text-sm text-slate-600">
        {isFetching && (
          <svg
            aria-hidden="true"
            className="inline w-4 h-4 mr-2 text-primary animate-spin"
            fill="none"
            viewBox="0 0 24 24"
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
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        )}
        Showing {startIndex}–{endIndex} of {totalElements} results
      </div>

      {deleteError && (
        <div className="mb-4 rounded-xl border-2 border-error bg-error-light px-4 py-3 text-sm text-error font-medium">
          {deleteError}
        </div>
      )}

      {/* Content */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(6)].map((_, i) => (
            <CardSkeleton key={i} />
          ))}
        </div>
      ) : isError ? (
        <ErrorState
          title="Failed to load products"
          message={
            error?.message || 'An error occurred while fetching products.'
          }
        />
      ) : products.length === 0 ? (
        <EmptyState
          title="No products found"
          description={
            hasActiveFilters
              ? 'Try adjusting your filters or search query.'
              : 'Get started by adding your first product.'
          }
          actionLabel={!hasActiveFilters ? 'Add Product' : undefined}
          actionHref={!hasActiveFilters ? '/products/new' : undefined}
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {products.map((product) => (
            <div
              key={product.id}
              className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-soft hover:shadow-card transition-all duration-200"
            >
              <div className="relative h-52 bg-slate-100">
                <img
                  className="w-full h-full object-cover"
                  src={
                    product.imageUrl
                      ? resolveMediaUrl(product.imageUrl)
                      : 'https://via.placeholder.com/300'
                  }
                  alt={product.name}
                />
                {/* Stock overlay */}
                <div className="absolute top-3 right-3">
                  <StockBadge product={product} />
                </div>
              </div>

              <div className="p-4 space-y-1">
                <h3 className="text-lg font-display font-semibold text-slate-900 truncate">
                  {highlightMatch(product.name, search)}
                </h3>
                <p className="text-xs text-slate-500">
                  SKU: {highlightMatch(product.sku, search)}
                </p>

                <div className="mt-4 flex items-center justify-between">
                  <span className="text-xl font-bold text-primary">
                    ${product.price}
                  </span>
                  <span className="text-sm text-slate-500">
                    Qty: {product.stockQuantity}
                  </span>
                </div>
              </div>

              <div className="flex border-t border-slate-100">
                <button
                  onClick={() => navigate(`/products/${product.id}/edit`)}
                  aria-label={`Edit ${product.name}`}
                  className="flex-1 px-4 py-3 text-sm font-semibold text-slate-600 hover:text-primary hover:bg-primary-subtle transition-colors"
                >
                  Edit
                </button>
                <button
                  onClick={() =>
                    deletingId !== product.id && handleDelete(product)
                  }
                  aria-label={`Delete ${product.name}`}
                  disabled={deletingId === product.id}
                  className="flex-1 px-4 py-3 text-sm font-semibold text-error hover:bg-error-light transition-colors border-l border-slate-100 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-transparent flex items-center justify-center gap-2"
                >
                  {deletingId === product.id ? (
                    <>
                      <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24">
                        <circle
                          className="opacity-25"
                          cx="12"
                          cy="12"
                          r="10"
                          stroke="currentColor"
                          strokeWidth="4"
                          fill="none"
                        />
                        <path
                          className="opacity-75"
                          fill="currentColor"
                          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                        />
                      </svg>
                      Deleting...
                    </>
                  ) : (
                    'Delete'
                  )}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      <footer className="flex justify-center pt-10">
        {totalPages > 1 && (
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
        )}
      </footer>

      {/* Delete Confirmation Modal */}
      {deleteModal}
    </div>
  );
}

export default ProductPage;
