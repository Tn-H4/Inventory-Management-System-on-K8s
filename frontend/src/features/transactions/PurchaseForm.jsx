import React, { useMemo } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { purchaseSchema, purchaseFormDefaultValues } from './purchaseSchema';
import { StockHint, renderProductStockOption } from '../products/stockStatus';
import SearchableSelect from '../../components/SearchableSelect';
import { useGetProductSupplierTracking } from '../../features/productSuppliers/api';
import { formatCurrency } from '../../lib/format';

function PurchaseForm({
  defaultValues = purchaseFormDefaultValues,
  submitLabel = 'Create Purchase',
  onSubmit = () => {},
  isSubmitting: isSubmittingProp = false,
  serverError,
  products = [],
  suppliers = [],
}) {
  const {
    control,
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(purchaseSchema),
    defaultValues,
  });

  React.useEffect(() => {
    reset(defaultValues);
  }, [defaultValues, reset]);

  const submitting = isSubmitting || isSubmittingProp;

  const selectedProductId = watch('productId');
  const selectedSupplierId = watch('supplierId');
  const selectedProduct = products.find((p) => p.id === selectedProductId);

  // Fetch product-supplier link on-demand when both are selected
  const { data: trackingData } = useGetProductSupplierTracking(
    selectedProductId || null,
    selectedSupplierId || null
  );
  const productSupplierLink = trackingData?.productSupplier;
  const purchaseCost = productSupplierLink?.unitCost ?? 0;
  const quantity = watch('quantity') ?? 0;
  const purchaseTotal = purchaseCost * quantity;

  const supplierOptions = useMemo(
    () => suppliers.map((s) => ({ value: s.id, label: s.name })),
    [suppliers]
  );
  const productOptions = useMemo(
    () =>
      products.map((p) => ({
        value: p.id,
        label: p.name,
        meta: { stock: p.stockQuantity, threshold: p.reorderThreshold },
      })),
    [products]
  );

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="mx-auto max-w-[867px] min-h-[500px] rounded-[32px] border border-slate-200 bg-white p-10 shadow-sm"
      noValidate
    >
      <h1 className="text-2xl font-semibold text-slate-900 mb-1">
        Receive Inventory
      </h1>
      <p className="text-sm text-slate-500 mb-6">
        Add new stock into the inventory and record this transaction.
      </p>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div className="md:col-span-2">
          <Controller
            name="supplierId"
            control={control}
            render={({ field, fieldState }) => (
              <SearchableSelect
                id="supplierId"
                label="Supplier"
                placeholder="Search suppliers..."
                options={supplierOptions}
                value={field.value}
                onChange={field.onChange}
                error={fieldState.error?.message}
              />
            )}
          />
        </div>

        <div>
          <Controller
            name="productId"
            control={control}
            render={({ field, fieldState }) => (
              <SearchableSelect
                id="productId"
                label="Product"
                placeholder="Search products..."
                options={productOptions}
                value={field.value}
                onChange={field.onChange}
                error={fieldState.error?.message}
                renderOption={renderProductStockOption}
              />
            )}
          />
          <StockHint product={selectedProduct} />
        </div>

        <div className="space-y-2">
          <label
            htmlFor="quantity"
            className="block text-sm font-medium text-slate-700"
          >
            Quantity to Purchase
          </label>
          <input
            id="quantity"
            type="number"
            inputMode="numeric"
            min="1"
            step="1"
            className="w-full rounded-lg bg-slate-100 border-transparent px-4 py-3 text-sm focus:bg-white focus:border-[#3E3998] focus:ring-2 focus:ring-[#3E3998]/20"
            {...register('quantity', { valueAsNumber: true })}
            aria-invalid={errors.quantity ? 'true' : 'false'}
          />
          {errors.quantity ? (
            <p className="text-sm text-red-600" role="alert">
              {errors.quantity.message}
            </p>
          ) : null}
        </div>

        {/* Cost display */}
        {Boolean(selectedProductId) &&
          Boolean(selectedSupplierId) &&
          quantity > 0 &&
          purchaseCost > 0 && (
            <div className="rounded-lg bg-slate-50 p-4 md:col-span-2">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Purchase Cost / Unit
                  </p>
                  <p className="mt-1 text-lg font-semibold text-slate-900">
                    {formatCurrency(purchaseCost)}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Purchase Total
                  </p>
                  <p className="mt-1 text-lg font-semibold text-primary">
                    {formatCurrency(purchaseTotal)}
                  </p>
                </div>
              </div>
            </div>
          )}

        <div className="space-y-2 md:col-span-2">
          <label
            htmlFor="description"
            className="block text-sm font-medium text-slate-700"
          >
            Description (Optional)
          </label>
          <textarea
            id="description"
            rows={2}
            className="w-full rounded-lg bg-slate-100 border-transparent px-4 py-3 text-sm focus:bg-white focus:border-[#3E3998] focus:ring-2 focus:ring-[#3E3998]/20"
            {...register('description')}
            aria-invalid={errors.description ? 'true' : 'false'}
          />
        </div>

        <div className="space-y-2 md:col-span-2">
          <label
            htmlFor="note"
            className="block text-sm font-medium text-slate-700"
          >
            Note (Optional)
          </label>
          <textarea
            id="note"
            rows={2}
            className="w-full rounded-lg bg-slate-100 border-transparent px-4 py-3 text-sm focus:bg-white focus:border-[#3E3998] focus:ring-2 focus:ring-[#3E3998]/20"
            {...register('note')}
            aria-invalid={errors.note ? 'true' : 'false'}
          />
        </div>
      </div>

      {serverError ? (
        <div className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {serverError}
        </div>
      ) : null}

      <div className="mt-8">
        <button
          type="submit"
          className="w-full rounded-lg bg-[#3E3998] py-3 text-sm font-semibold text-white hover:opacity-90 transition-opacity disabled:cursor-not-allowed disabled:opacity-60"
          disabled={submitting}
        >
          {submitting ? 'Creating Purchase…' : 'Purchase Product'}
        </button>
      </div>
    </form>
  );
}

export default PurchaseForm;
