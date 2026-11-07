import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { productSchema, productFormDefaultValues } from './schema';
import { resolveMediaUrl } from '../../lib/media';

function ProductForm({
  defaultValues = productFormDefaultValues,
  submitLabel = 'Save Product',
  onSubmit = () => {},
  mode = 'create',
  isSubmitting: isSubmittingProp = false,
  serverError,
  serverSuccess,
  categories = [],
  warehouses = [],
  onImageChange,
  imageUrl,
}) {
  const previewUrl = resolveMediaUrl(imageUrl);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty, isSubmitting },
  } = useForm({
    resolver: zodResolver(productSchema),
    defaultValues,
    mode: 'onChange',
  });

  React.useEffect(() => {
    reset(defaultValues);
  }, [defaultValues, reset]);

  const submitting = isSubmitting || isSubmittingProp;

  const [fileError, setFileError] = React.useState('');
  const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
  const MAX_IMAGE_MB = 5;

  const handleImageChange = (e) => {
    setFileError('');
    const file = e.target.files?.[0];
    if (!file) return;
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      setFileError('Only JPG, PNG, or WEBP images are allowed.');
      e.target.value = '';
      return;
    }
    if (file.size > MAX_IMAGE_MB * 1024 * 1024) {
      setFileError(`Image must be under ${MAX_IMAGE_MB} MB.`);
      e.target.value = '';
      return;
    }
    onImageChange?.(e);
  };

  const nameBoxRef = React.useRef(null);
  const descBoxRef = React.useRef(null);
  const rightColRef = React.useRef(null);
  const previewRef = React.useRef(null);

  React.useEffect(() => {
    let rafId = null;
    let roTimeout = null;
    let isScheduled = false;

    function syncPreview() {
      isScheduled = false;

      const nb = nameBoxRef.current;
      const db = descBoxRef.current;
      const rc = rightColRef.current;
      const pv = previewRef.current;
      if (!nb || !db || !rc || !pv) {
        return;
      }
      pv.style.marginBottom = '32px';

      const isMd = window.matchMedia('(min-width: 768px)').matches;
      if (!isMd) {
        pv.style.height = '';
        pv.style.width = '';
        pv.style.marginTop = '';
        pv.style.marginLeft = '';
        pv.style.marginRight = '';
        return;
      }

      const nameTop = nb.getBoundingClientRect().top;
      const descBottom = db.getBoundingClientRect().bottom;
      const rightTop = rc.getBoundingClientRect().top;

      const measuredHeight = Math.max(0, Math.round(descBottom - nameTop));
      const offsetTop = Math.max(0, Math.round(nameTop - rightTop));

      const rcStyles = getComputedStyle(rc);
      const rcWidth =
        rc.getBoundingClientRect().width -
        parseFloat(rcStyles.paddingLeft) -
        parseFloat(rcStyles.paddingRight);

      const side = Math.max(0, Math.min(measuredHeight, rcWidth));

      pv.style.marginTop = `${offsetTop}px`;
      pv.style.width = `${side}px`;
      pv.style.height = `${side}px`;
      pv.style.marginLeft = 'auto';
      pv.style.marginRight = 'auto';
    }

    function debouncedSync() {
      if (isScheduled) return;
      isScheduled = true;
      // Cancel any pending frame
      if (rafId) {
        cancelAnimationFrame(rafId);
      }
      // Use requestAnimationFrame for smooth updates and to avoid ResizeObserver loop
      rafId = requestAnimationFrame(() => {
        // Add a small delay to let layout settle
        roTimeout = setTimeout(syncPreview, 16);
      });
    }

    // Suppress ResizeObserver loop warning with debouncing
    const ro = new ResizeObserver(debouncedSync);
    nameBoxRef.current && ro.observe(nameBoxRef.current);
    descBoxRef.current && ro.observe(descBoxRef.current);
    rightColRef.current && ro.observe(rightColRef.current);

    function handleResize() {
      if (rafId) {
        cancelAnimationFrame(rafId);
        rafId = null;
      }
      if (roTimeout) clearTimeout(roTimeout);
      syncPreview();
    }

    window.addEventListener('resize', handleResize);
    requestAnimationFrame(syncPreview);

    return () => {
      ro.disconnect();
      window.removeEventListener('resize', handleResize);
      if (rafId) cancelAnimationFrame(rafId);
      if (roTimeout) clearTimeout(roTimeout);
    };
  }, []);

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="space-y-6 bg-white p-0 shadow-none sm:rounded-[32px] sm:border sm:border-slate-200 sm:p-6 sm:shadow-sm lg:rounded-[40px]"
      noValidate
    >
      <div className="space-y-1 mb-6">
        <h2 className="text-3xl font-inter font-bold text-slate-800 sm:text-[40px]">
          {mode === 'create' ? 'New Product' : 'Edit Product'}
        </h2>
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div className="space-y-4 md:col-span-1">
          <div className="space-y-2">
            <label
              htmlFor="name"
              className="block text-[19px] font-inter font-normal text-slate-700"
            >
              Product Name
            </label>
            <div
              ref={nameBoxRef}
              className="bg-gray-100 rounded-md px-3 py-2 hover:bg-gray-200 transition duration-300"
            >
              <input
                id="name"
                type="text"
                className="w-full bg-transparent outline-none"
                {...register('name')}
                aria-invalid={errors.name ? 'true' : 'false'}
              />
            </div>
            {errors.name && (
              <p className="text-sm text-red-600">{errors.name.message}</p>
            )}
          </div>

          {mode === 'edit' && (
            <div className="space-y-2">
              <label
                htmlFor="sku"
                className="block text-[19px] font-inter font-normal text-slate-700"
              >
                Product Code
              </label>
              <div className="bg-gray-100 rounded-md px-3 py-2">
                <input
                  id="sku"
                  type="text"
                  className="w-full bg-transparent text-slate-600 outline-none"
                  value={defaultValues.sku ?? ''}
                  disabled
                  readOnly
                />
              </div>
            </div>
          )}

          <div className="space-y-2">
            <label
              htmlFor="description"
              className="block text-[19px] font-normal font-inter text-slate-700"
            >
              Description
            </label>
            <div
              ref={descBoxRef}
              className="bg-gray-100 rounded-md px-3 py-2 hover:bg-gray-200 transition duration-300"
            >
              <textarea
                id="description"
                rows={8}
                className="w-full bg-transparent outline-none"
                {...register('description')}
                aria-invalid={errors.description ? 'true' : 'false'}
              />
            </div>
            {errors.description && (
              <p className="text-sm text-red-600">
                {errors.description.message}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <label
              htmlFor="price"
              className="block text-[19px] font-normal font-inter text-slate-700"
            >
              Selling Price
            </label>
            <div className="bg-gray-100 rounded-md px-3 py-2 hover:bg-gray-200 transition duration-300">
              <input
                id="price"
                type="number"
                className="w-full bg-transparent outline-none"
                {...register('price', { valueAsNumber: true })}
              />
              {errors.price && (
                <p className="text-sm text-red-600">{errors.price.message}</p>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <label
              htmlFor="stockQuantity"
              className="block text-[19px] font-inter font-normal text-slate-700"
            >
              Stock Quantity
            </label>
            <div className="bg-gray-100 rounded-md px-3 py-2 hover:bg-gray-200 transition duration-300">
              <input
                id="stockQuantity"
                type="number"
                className="w-full bg-transparent outline-none"
                {...register('stockQuantity', { valueAsNumber: true })}
              />
              {errors.stockQuantity && (
                <p className="text-sm text-red-600">
                  {errors.stockQuantity.message}
                </p>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <label
              htmlFor="reorderThreshold"
              className="block text-[19px] font-inter font-normal text-slate-700"
            >
              Reorder Threshold
            </label>
            <div className="bg-gray-100 rounded-md px-3 py-2 hover:bg-gray-200 transition duration-300">
              <input
                id="reorderThreshold"
                type="number"
                min={0}
                className="w-full bg-transparent outline-none"
                {...register('reorderThreshold', { valueAsNumber: true })}
              />
              {errors.reorderThreshold && (
                <p className="text-sm text-red-600">
                  {errors.reorderThreshold.message}
                </p>
              )}
            </div>
          </div>
        </div>

        <div
          ref={rightColRef}
          className="space-y-4 md:col-span-1 md:flex md:flex-col md:items-center"
        >
          <div
            ref={previewRef}
            className="
              w-full max-w-[320px] md:w-auto md:max-w-none aspect-square md:aspect-auto mx-auto
              grid place-items-center
              bg-gray-200 rounded-2xl sm:rounded-[30px] hover:bg-gray-300
              transition duration-300 ease-in-out overflow-hidden
            "
          >
            {previewUrl ? (
              <img
                src={previewUrl}
                alt="Preview"
                className="w-full h-full object-cover object-center"
              />
            ) : (
              <span className="text-sm text-slate-500 text-center">
                Image preview
              </span>
            )}
          </div>
          <div className="md:self-start w-full mt-6 md:mt-10">
            <div className="flex items-center justify-between gap-3 w-full">
              <label className="text-[19px] font-inter font-normal text-slate-700 whitespace-nowrap">
                Product Image
              </label>

              <label
                htmlFor="imageFile"
                className="cursor-pointer rounded-md bg-black px-5 py-2 text-sm font-semibold text-white transition hover:bg-black/80 sm:px-6"
              >
                Browse
              </label>
            </div>
            <div className="mt-2 max-w-full rounded-lg border border-slate-200 bg-gray-100 px-3 py-2 text-sm text-slate-700">
              <p className="truncate">
                {imageUrl ? imageUrl.split('/').pop() : 'No file selected'}
              </p>
            </div>

            <input
              id="imageFile"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={handleImageChange}
              className="hidden"
            />
            {fileError && (
              <p className="mt-1 text-sm text-red-600">{fileError}</p>
            )}
            <div className="space-y-2 mt-5">
              <label
                htmlFor="categoryId"
                className="block text-[19px] font-inter font-normal text-slate-700"
              >
                Category
              </label>
              <div className="bg-gray-100 rounded-md px-3 py-2 hover:bg-gray-200 transition duration-300">
                <select
                  id="categoryId"
                  className="w-full bg-transparent outline-none"
                  {...register('categoryId', { valueAsNumber: true })}
                >
                  <option value={0}>Select a category</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
                {errors.categoryId && (
                  <p className="text-sm text-red-600">
                    {errors.categoryId.message}
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-2 mt-5">
              <label
                htmlFor="warehouseId"
                className="block text-[19px] font-inter font-normal text-slate-700"
              >
                Warehouse
              </label>
              <div className="bg-gray-100 rounded-md px-3 py-2 hover:bg-gray-200 transition duration-300">
                <select
                  id="warehouseId"
                  className="w-full bg-transparent outline-none"
                  {...register('warehouseId', { valueAsNumber: true })}
                >
                  <option value={0}>Select a warehouse</option>
                  {warehouses.map((w) => (
                    <option key={w.id} value={w.id}>
                      {w.name}
                    </option>
                  ))}
                </select>
                {errors.warehouseId && (
                  <p className="text-sm text-red-600">
                    {errors.warehouseId.message}
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {serverError && (
        <div className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {serverError}
        </div>
      )}

      {serverSuccess && !isDirty && (
        <div className="rounded-md border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700">
          {serverSuccess}
        </div>
      )}

      <div className="flex justify-end gap-3">
        <button
          type="submit"
          className="w-full flex items-center justify-center rounded-md bg-primary px-4 py-3 text-sm font-semibold text-white shadow-sm"
          disabled={submitting}
        >
          {submitting ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  );
}

export default ProductForm;
