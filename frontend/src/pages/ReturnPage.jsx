import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useProducts } from '../features/products/api';
import {
  useProductSuppliers,
  useGetProductSupplierTracking,
} from '../features/productSuppliers/api';
import { useReturnTransaction } from '../features/transactions/api';
import {
  returnFormDefaultValues,
  returnSchema,
} from '../features/transactions/returnSchema';
import {
  StockHint,
  renderProductStockOption,
} from '../features/products/stockStatus';
import { LoadingState } from '../components/EmptyState';
import SearchableSelect from '../components/SearchableSelect';
import { formatCurrency } from '../lib/format';

function ReturnPage() {
  const navigate = useNavigate();
  const [successMessage, setSuccessMessage] = useState('');
  const [serverError, setServerError] = useState('');

  const { data: productsData, isLoading: isLoadingProducts } = useProducts({
    page: 1,
    limit: 9999,
  });

  const products = useMemo(() => productsData?.products ?? [], [productsData]);

  const { mutate, isPending, error } = useReturnTransaction();

  const {
    control,
    handleSubmit,
    reset,
    watch,
    register,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(returnSchema),
    defaultValues: returnFormDefaultValues,
  });

  useEffect(() => {
    reset(returnFormDefaultValues);
  }, [reset]);

  // Reactive supplier filtering based on selected product
  const selectedProductId = watch('productId');
  const selectedSupplierId = watch('supplierId');
  const selectedProduct = products.find((p) => p.id === selectedProductId);
  const { data: productSuppliersData } = useProductSuppliers(
    selectedProductId || null
  );

  // Fetch tracking data to show available to return
  const { data: trackingData } = useGetProductSupplierTracking(
    selectedProductId || null,
    selectedSupplierId || null
  );

  const productSupplierLink = trackingData?.productSupplier;
  const totalPurchased = productSupplierLink?.totalPurchased ?? 0;
  const totalReturned = productSupplierLink?.totalReturned ?? 0;
  const availableToReturn = totalPurchased - totalReturned;

  const supplierOptions = useMemo(() => {
    if (!selectedProductId) return [];
    return (productSuppliersData?.productSuppliers ?? []).map((ps) => ({
      value: ps.supplierId,
      label:
        ps.supplierName ?? ps.supplier?.name ?? `Supplier #${ps.supplierId}`,
      meta: {
        unitCost: ps.unitCost,
        isPreferred: ps.isPreferred,
        totalPurchased: ps.totalPurchased ?? 0,
        totalReturned: ps.totalReturned ?? 0,
      },
    }));
  }, [selectedProductId, productSuppliersData]);

  const productOptions = useMemo(
    () =>
      products.map((p) => ({
        value: p.id,
        label: p.name,
        meta: { stock: p.stockQuantity, threshold: p.reorderThreshold },
      })),
    [products]
  );

  const onSubmit = (formData) => {
    setSuccessMessage('');
    setServerError('');

    mutate(formData, {
      onSuccess: (res) => {
        setSuccessMessage(res?.message || 'Return created successfully.');
        reset(returnFormDefaultValues);
        setTimeout(() => navigate('/transactions'), 400);
      },
      onError: (err) => {
        setServerError(err.message || 'Failed to create return.');
      },
    });
  };

  const isLoading = isLoadingProducts;

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="mx-auto max-w-[867px] min-h-[500px] rounded-2xl border border-slate-200 bg-white p-8 shadow-soft"
      noValidate
    >
      <header className="mb-10">
        <h1 className="text-2xl font-semibold text-slate-900">
          Return to Supplier
        </h1>
        <p className="mt-2 text-sm text-slate-500">
          Create a return transaction and deduct stock.
        </p>
      </header>

      {isLoading ? (
        <LoadingState
          title="Loading data..."
          message="Please wait while we fetch products and suppliers."
        />
      ) : (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          {/* Product */}
          <div className="md:col-span-2">
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

          {/* Supplier — filtered to product's linked suppliers */}
          <div className="md:col-span-2">
            <Controller
              name="supplierId"
              control={control}
              render={({ field, fieldState }) => (
                <SearchableSelect
                  id="supplierId"
                  label="Supplier"
                  placeholder={
                    selectedProductId
                      ? 'Search linked suppliers...'
                      : 'Select a product first'
                  }
                  options={supplierOptions}
                  value={field.value}
                  onChange={field.onChange}
                  disabled={!selectedProductId}
                  error={fieldState.error?.message}
                  renderOption={(opt) => {
                    const available =
                      (opt.meta?.totalPurchased ?? 0) -
                      (opt.meta?.totalReturned ?? 0);
                    return (
                      <span className="flex items-center gap-2">
                        <span>{opt.label}</span>
                        {opt.meta?.isPreferred && (
                          <span className="rounded px-1.5 py-0.5 text-xs font-medium bg-amber-100 text-amber-700">
                            Default
                          </span>
                        )}
                        <span className="text-xs text-slate-500">
                          Available: {available}
                        </span>
                        {opt.meta?.unitCost != null && (
                          <span className="ml-auto shrink-0 text-xs text-slate-500">
                            ${Number(opt.meta.unitCost).toFixed(2)}/unit
                          </span>
                        )}
                      </span>
                    );
                  }}
                />
              )}
            />
            {Boolean(selectedSupplierId) && (
              <p className="mt-1 text-sm text-slate-600">
                Available to return:{' '}
                <span className="font-medium text-primary">
                  {availableToReturn} units
                </span>{' '}
                (purchased: {totalPurchased}, returned: {totalReturned})
              </p>
            )}
          </div>

          {/* Quantity */}
          <div className="space-y-2">
            <label
              htmlFor="quantity"
              className="block text-sm font-medium text-slate-700"
            >
              Quantity to Return
            </label>
            <input
              id="quantity"
              type="number"
              inputMode="numeric"
              min="1"
              step="1"
              className="w-full rounded-xl bg-slate-50 border-2 border-transparent px-4 py-3 text-sm focus:bg-white focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
              {...register('quantity', { valueAsNumber: true })}
              aria-invalid={errors.quantity ? 'true' : 'false'}
            />
            {errors.quantity ? (
              <p className="text-sm text-red-600" role="alert">
                {errors.quantity.message}
              </p>
            ) : null}
          </div>

          {/* Return value display */}
          {Boolean(selectedSupplierId) &&
            productSupplierLink?.unitCost != null &&
            (watch('quantity') || 0) > 0 && (
              <div className="rounded-lg bg-slate-50 p-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                      Purchase Cost / Unit
                    </p>
                    <p className="mt-1 text-lg font-semibold text-slate-900">
                      {formatCurrency(productSupplierLink.unitCost)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                      Return Value
                    </p>
                    <p className="mt-1 text-lg font-semibold text-accent">
                      {formatCurrency(
                        Number(productSupplierLink.unitCost) *
                          (watch('quantity') || 0)
                      )}
                    </p>
                  </div>
                </div>
              </div>
            )}

          {/* Description */}
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
              className="w-full rounded-xl bg-slate-50 border-2 border-transparent px-4 py-3 text-sm focus:bg-white focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
              {...register('description')}
              aria-invalid={errors.description ? 'true' : 'false'}
            />
          </div>

          {successMessage ? (
            <div className="md:col-span-2 rounded-xl border-2 border-success bg-success-light p-3 text-sm text-success font-medium">
              {successMessage}
            </div>
          ) : null}

          {serverError || error?.message ? (
            <div className="md:col-span-2 rounded-xl border-2 border-error bg-error-light p-3 text-sm text-error font-medium">
              {serverError || error?.message}
            </div>
          ) : null}
        </div>
      )}

      <div className="mt-8">
        <button
          type="submit"
          className="w-full rounded-xl bg-accent py-3 text-sm font-semibold text-white shadow-soft hover:shadow-card transition-all disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isPending || isLoading}
        >
          {isPending ? 'Creating Return…' : 'Return to Supplier'}
        </button>
      </div>
    </form>
  );
}

export default ReturnPage;
