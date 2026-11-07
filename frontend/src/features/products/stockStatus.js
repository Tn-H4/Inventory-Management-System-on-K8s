/**
 * Stock status for display and client-side filtering (search results).
 * Out of stock takes precedence; in stock is qty >= reorder threshold.
 */
export function getProductStockStatus(product) {
  const stock = Number(product?.stockQuantity ?? 0);
  const threshold = Number(product?.reorderThreshold ?? 0);

  if (stock === 0) {
    return 'OUT_OF_STOCK';
  }
  if (stock >= threshold) {
    return 'IN_STOCK';
  }
  return 'LOW_STOCK';
}

const STOCK_TEXT_CLASSES = {
  OUT_OF_STOCK: 'text-red-600',
  LOW_STOCK: 'text-amber-600',
  IN_STOCK: 'text-emerald-600',
};

/**
 * Inline stock hint rendered below a product select field.
 * Renders nothing when no product is selected.
 */
export function StockHint({ product }) {
  if (!product) return null;
  const status = getProductStockStatus(product);
  return (
    <p className={`mt-1.5 text-xs ${STOCK_TEXT_CLASSES[status]}`}>
      Current stock: <strong>{product.stockQuantity}</strong> unit
      {product.stockQuantity !== 1 ? 's' : ''}
    </p>
  );
}

const STOCK_BADGE_CLASSES = {
  OUT_OF_STOCK: 'bg-red-100 text-red-700',
  LOW_STOCK: 'bg-amber-100 text-amber-700',
  IN_STOCK: 'bg-green-100 text-green-700',
};

/**
 * Default renderOption for SearchableSelect product dropdowns.
 * Shows product name + a colour-coded stock badge.
 * opt.meta must have { stock, threshold }.
 */
export function renderProductStockOption(opt) {
  const status = getProductStockStatus({
    stockQuantity: opt.meta.stock,
    reorderThreshold: opt.meta.threshold,
  });
  return (
    <span className="flex items-center justify-between gap-2">
      <span>{opt.label}</span>
      <span
        className={`shrink-0 rounded px-1.5 py-0.5 text-xs font-medium ${STOCK_BADGE_CLASSES[status]}`}
      >
        stock: {opt.meta.stock}
      </span>
    </span>
  );
}

export const STOCK_STATUS_OPTIONS = [
  { value: '', label: 'All Stock Status' },
  { value: 'IN_STOCK', label: 'In Stock' },
  { value: 'LOW_STOCK', label: 'Low Stock' },
  { value: 'OUT_OF_STOCK', label: 'Out of Stock' },
];
