import React, { useMemo, useState, useEffect, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import ProductForm from '../features/products/ProductForm';
import { productFormDefaultValues } from '../features/products/schema';
import { useProduct, useUpdateProduct } from '../features/products/api';
import { useCategories } from '../features/categories/api';
import { useWarehouses } from '../features/warehouses/api';
import { useSuppliers } from '../features/suppliers/api';
import {
  useAddProductSupplier,
  useDeleteProductSupplier,
  useProductSuppliers,
  useUpdateProductSupplier,
} from '../features/productSuppliers/api';
import { EmptyState, ErrorState, LoadingState } from '../components/EmptyState';
import { useConfirmation } from '../components/ConfirmationModal';
import { LoadingSpinner } from '../components/LoadingSpinner';

const PRODUCTS_QUERY_KEY = ['products'];
const PRODUCT_SUPPLIERS_QUERY_KEY = ['product-suppliers'];

function ProductEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const {
    data: productData,
    isLoading: isLoadingProduct,
    isError: isErrorProduct,
    error: productError,
  } = useProduct(id);

  const { data: categoriesData } = useCategories({ limit: 1000 });
  const categories = categoriesData?.categories ?? [];

  const { data: warehousesData } = useWarehouses();
  const warehouses = warehousesData?.warehouses ?? [];

  const { mutateAsync: updateProduct, isPending } = useUpdateProduct();
  const [serverError, setServerError] = useState('');
  const [serverSuccess, setServerSuccess] = useState('');
  const [supplierError, setSupplierError] = useState('');
  const [imageFile, setImageFile] = useState(null);
  const [imageUrl, setImageUrl] = useState('');

  const { data: suppliersData } = useSuppliers({ limit: 1000 });
  const suppliers = useMemo(
    () => suppliersData?.suppliers ?? [],
    [suppliersData]
  );

  const {
    data: productSuppliersData,
    isLoading: isLoadingProductSuppliers,
    refetch: refetchProductSuppliers,
  } = useProductSuppliers(id);
  const productSuppliers = useMemo(
    () => productSuppliersData?.productSuppliers ?? [],
    [productSuppliersData]
  );

  const linkedSupplierIds = useMemo(
    () => new Set(productSuppliers.map((ps) => ps.supplierId)),
    [productSuppliers]
  );

  const availableSuppliers = useMemo(
    () => suppliers.filter((s) => !linkedSupplierIds.has(s.id)),
    [suppliers, linkedSupplierIds]
  );

  const reloadEditPage = useCallback(async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: [...PRODUCTS_QUERY_KEY, id] }),
      queryClient.invalidateQueries({
        queryKey: [...PRODUCT_SUPPLIERS_QUERY_KEY, { productId: id }],
      }),
      refetchProductSuppliers(),
    ]);
  }, [queryClient, id, refetchProductSuppliers]);

  const addProductSupplierMutation = useAddProductSupplier();
  const updateProductSupplierMutation = useUpdateProductSupplier();
  const deleteProductSupplierMutation = useDeleteProductSupplier();

  // Confirmation modal for removing supplier
  const { confirm, modal: removeSupplierModal } = useConfirmation({
    title: 'Remove Supplier',
    message: 'Are you sure you want to remove this supplier from the product?',
    confirmText: 'Remove',
    cancelText: 'Cancel',
    type: 'danger',
  });

  const [addSupplierId, setAddSupplierId] = useState(0);
  const [addUnitCost, setAddUnitCost] = useState('');

  const defaultValues = useMemo(() => {
    if (!productData) {
      return productFormDefaultValues;
    }

    // no side-effects here
    return {
      name: productData.product.name ?? '',
      sku: productData.product.sku ?? '',
      price: productData.product.price ?? 0,
      stockQuantity: productData.product.stockQuantity ?? 0,
      reorderThreshold: productData.product.reorderThreshold ?? 10,
      description: productData.product.description ?? '',
      categoryId: productData.product.categoryId ?? 0,
      warehouseId: productData.product.warehouseId ?? 0,
    };
  }, [productData]);

  // move side-effect of setting imageUrl into useEffect
  useEffect(() => {
    if (productData?.product?.imageUrl) {
      setImageUrl(productData.product.imageUrl);
    }
  }, [productData]);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImageFile(file);
      setImageUrl(URL.createObjectURL(file));
    }
  };

  const handleSubmit = async (formData) => {
    setServerError('');
    setServerSuccess('');
    setSupplierError('');

    const data = new FormData();
    Object.keys(formData).forEach((key) => {
      if (key === 'warehouseId') {
        if (formData[key]) data.append(key, formData[key]);
        return;
      }
      data.append(key, formData[key]);
    });

    if (imageFile) {
      data.append('imageFile', imageFile);
    }

    try {
      await updateProduct({ id, data });
      setImageFile(null);
      await reloadEditPage();
      setServerSuccess('Product updated successfully.');
      navigate(`/products/${id}/edit`, { replace: true });
    } catch (err) {
      setServerError(err.message || 'Failed to update product.');
    }
  };

  const handleSetPreferred = async (ps) => {
    if (Boolean(ps.isPreferred)) {
      return;
    }

    setSupplierError('');
    try {
      await updateProductSupplierMutation.mutateAsync({
        productId: Number(id),
        supplierId: ps.supplierId,
        unitCost: Number(ps.unitCost ?? 0),
        isPreferred: true,
      });
      await reloadEditPage();
    } catch (err) {
      setSupplierError(err.message || 'Failed to set preferred supplier.');
    }
  };

  const handleUpdateSupplierCost = async (ps, value) => {
    const nextUnitCost = Number(value || 0);
    setSupplierError('');
    try {
      await updateProductSupplierMutation.mutateAsync({
        productId: Number(id),
        supplierId: ps.supplierId,
        unitCost: nextUnitCost,
        isPreferred: Boolean(ps.isPreferred),
      });
      await reloadEditPage();
    } catch (err) {
      setSupplierError(err.message || 'Failed to update supplier link.');
    }
  };

  const handleRemoveSupplier = async (ps) => {
    const confirmed = await confirm();
    if (!confirmed) return;

    setSupplierError('');
    try {
      await deleteProductSupplierMutation.mutateAsync({
        productId: Number(id),
        supplierId: ps.supplierId,
      });
      await reloadEditPage();
    } catch (err) {
      setSupplierError(err.message || 'Failed to remove supplier.');
    }
  };

  if (isLoadingProduct) {
    return (
      <LoadingState
        title="Loading product..."
        message="Please wait while we fetch the product data."
      />
    );
  }

  if (isErrorProduct) {
    return (
      <ErrorState
        title="Failed to load product"
        message={
          productError?.message ||
          'An error occurred while loading the product.'
        }
      />
    );
  }

  if (!productData) {
    return (
      <EmptyState
        title="Product not found"
        description="The product you're looking for doesn't exist or has been removed."
      />
    );
  }

  return (
    <div className="w-full min-h-full">
      <div className="w-full max-w-[1500px] space-y-8 px-0 sm:px-6 lg:px-10">
        <ProductForm
          defaultValues={defaultValues}
          submitLabel="Update Product"
          onSubmit={handleSubmit}
          mode="edit"
          isSubmitting={isPending}
          serverError={serverError}
          serverSuccess={serverSuccess}
          categories={categories}
          warehouses={warehouses}
          onImageChange={handleImageChange}
          imageUrl={imageUrl}
        />

        <section className="bg-white p-0 shadow-none sm:rounded-[32px] sm:border sm:border-slate-200 sm:p-8 sm:shadow-sm">
          <header className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-xl font-semibold text-slate-900">
                Suppliers
              </h2>
              <p className="text-sm text-slate-500">
                Manage suppliers linked to this product. Only one can be
                preferred at a time.
              </p>
            </div>
            <button
              type="button"
              onClick={() => navigate('/products')}
              className="text-sm font-medium text-slate-600 hover:text-primary"
            >
              Back to products
            </button>
          </header>

          {supplierError && (
            <div className="mb-4 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
              {supplierError}
            </div>
          )}

          <div className="mb-6 rounded-xl border border-slate-200 bg-slate-50 p-4">
            <h3 className="mb-3 text-sm font-semibold text-slate-900">
              Add Supplier
            </h3>
            <div className="grid grid-cols-1 gap-3 md:grid-cols-4">
              <select
                value={addSupplierId}
                onChange={(e) => setAddSupplierId(Number(e.target.value))}
                aria-label="Select supplier"
                className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm shadow-sm focus:border-primary focus:ring-2 focus:ring-primary/20"
              >
                <option value={0}>Select supplier</option>
                {availableSuppliers.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>

              <input
                type="number"
                min={0}
                step="0.01"
                value={addUnitCost}
                onChange={(e) => setAddUnitCost(e.target.value)}
                aria-label="Unit cost"
                className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm shadow-sm focus:border-primary focus:ring-2 focus:ring-primary/20"
                placeholder="Unit cost"
              />

              <button
                type="button"
                disabled={
                  addProductSupplierMutation.isPending || !addSupplierId
                }
                onClick={async () => {
                  setSupplierError('');
                  try {
                    await addProductSupplierMutation.mutateAsync({
                      productId: Number(id),
                      supplierId: addSupplierId,
                      unitCost: addUnitCost === '' ? 0 : Number(addUnitCost),
                      isPreferred: !productSuppliers.some((ps) =>
                        Boolean(ps.isPreferred)
                      ),
                    });
                    setAddSupplierId(0);
                    setAddUnitCost('');
                    await reloadEditPage();
                    navigate(`/products/${id}/edit`, { replace: true });
                  } catch (err) {
                    setSupplierError(err.message || 'Failed to add supplier.');
                  }
                }}
                className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-700 disabled:opacity-60 md:w-auto"
              >
                Add
              </button>
            </div>
          </div>

          {isLoadingProductSuppliers ? (
            <div className="py-10 text-center text-slate-500">Loading...</div>
          ) : productSuppliers.length === 0 ? (
            <div className="py-10 text-center text-slate-500">
              No suppliers linked to this product.
            </div>
          ) : (
            <>
              <div className="hidden overflow-x-auto rounded-xl border border-slate-200 md:block">
                <table className="min-w-full divide-y divide-slate-200">
                  <thead className="bg-slate-50">
                    <tr>
                      <th
                        scope="col"
                        className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider text-slate-700"
                      >
                        Supplier
                      </th>
                      <th
                        scope="col"
                        className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider text-slate-700"
                      >
                        Contact
                      </th>
                      <th
                        scope="col"
                        className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider text-slate-700"
                      >
                        Purchase Cost / Unit
                      </th>
                      <th
                        scope="col"
                        className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider text-slate-700"
                      >
                        Default
                      </th>
                      <th
                        scope="col"
                        className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider text-slate-700"
                      >
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 bg-white">
                    {productSuppliers.map((ps) => (
                      <tr key={`${ps.productId}-${ps.supplierId}`}>
                        <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-slate-900">
                          {ps.supplierName ??
                            ps.supplier?.name ??
                            ps.supplierId}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                          {ps.supplierContact ??
                            ps.supplier?.contactInfo ??
                            '—'}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                          <input
                            type="number"
                            min={0}
                            step="0.01"
                            defaultValue={ps.unitCost ?? 0}
                            onBlur={(e) =>
                              handleUpdateSupplierCost(ps, e.target.value)
                            }
                            aria-label="Unit cost"
                            className="w-28 rounded-md border border-slate-200 bg-white px-3 py-2 text-sm shadow-sm focus:border-primary focus:ring-2 focus:ring-primary/20"
                          />
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                          {Boolean(ps.isPreferred) ? (
                            <span className="inline-flex rounded-full bg-primary-subtle px-2.5 py-1 text-xs font-semibold text-primary">
                              Default
                            </span>
                          ) : (
                            <button
                              type="button"
                              onClick={() => handleSetPreferred(ps)}
                              disabled={updateProductSupplierMutation.isPending}
                              className="rounded-md border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:border-primary hover:text-primary disabled:opacity-60"
                            >
                              Use as default
                            </button>
                          )}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm">
                          <button
                            type="button"
                            disabled={deleteProductSupplierMutation.isPending}
                            onClick={() => handleRemoveSupplier(ps)}
                            className="rounded-md bg-error-light px-3 py-1.5 text-sm font-medium text-error hover:bg-red-100 disabled:opacity-60"
                          >
                            {deleteProductSupplierMutation.isPending ? (
                              <LoadingSpinner size="sm" />
                            ) : (
                              'Remove'
                            )}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="space-y-4 md:hidden">
                {productSuppliers.map((ps) => (
                  <article
                    key={`${ps.productId}-${ps.supplierId}`}
                    className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <h3 className="truncate text-base font-semibold text-slate-900">
                          {ps.supplierName ??
                            ps.supplier?.name ??
                            ps.supplierId}
                        </h3>
                        <p className="mt-1 truncate text-sm text-slate-500">
                          {ps.supplierContact ??
                            ps.supplier?.contactInfo ??
                            'No contact info'}
                        </p>
                      </div>
                      {Boolean(ps.isPreferred) ? (
                        <span className="shrink-0 rounded-full bg-primary-subtle px-2.5 py-1 text-xs font-semibold text-primary">
                          Default
                        </span>
                      ) : null}
                    </div>

                    <div className="mt-4 space-y-3">
                      <label className="block text-sm font-medium text-slate-700">
                        Purchase Cost / Unit
                        <input
                          type="number"
                          min={0}
                          step="0.01"
                          defaultValue={ps.unitCost ?? 0}
                          onBlur={(e) =>
                            handleUpdateSupplierCost(ps, e.target.value)
                          }
                          aria-label="Unit cost"
                          className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm shadow-sm focus:border-primary focus:ring-2 focus:ring-primary/20"
                        />
                      </label>

                      {Boolean(ps.isPreferred) ? (
                        <div className="rounded-lg bg-primary-subtle px-3 py-2 text-sm font-semibold text-primary">
                          Default supplier
                        </div>
                      ) : (
                        <button
                          type="button"
                          onClick={() => handleSetPreferred(ps)}
                          disabled={updateProductSupplierMutation.isPending}
                          className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-slate-700 hover:border-primary hover:text-primary disabled:opacity-60"
                        >
                          Make preferred
                        </button>
                      )}

                      <button
                        type="button"
                        disabled={deleteProductSupplierMutation.isPending}
                        onClick={() => handleRemoveSupplier(ps)}
                        className="w-full rounded-lg bg-error-light px-3 py-2 text-sm font-semibold text-error hover:bg-red-100 disabled:opacity-60"
                      >
                        {deleteProductSupplierMutation.isPending ? (
                          <LoadingSpinner size="sm" />
                        ) : (
                          'Remove'
                        )}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            </>
          )}

          {/* Remove Supplier Confirmation Modal */}
          {removeSupplierModal}
        </section>
      </div>
    </div>
  );
}

export default ProductEditPage;
