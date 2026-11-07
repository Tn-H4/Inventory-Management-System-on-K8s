import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { warehouseSchema, warehouseFormDefaultValues } from './schema';

function WarehouseForm({
  defaultValues = warehouseFormDefaultValues,
  submitLabel = 'Save Warehouse',
  onSubmit = () => {},
  isSubmitting: isSubmittingProp = false,
  serverError,
}) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(warehouseSchema),
    defaultValues,
  });

  React.useEffect(() => {
    reset(defaultValues);
  }, [defaultValues, reset]);

  const submitting = isSubmitting || isSubmittingProp;

  return (
    <div className="max-w-2xl mx-auto bg-white rounded-xl shadow-lg overflow-hidden">
      <div className="bg-white p-8">
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="space-y-6"
          noValidate
        >
          {/* Name Input */}
          <div className="space-y-1.5">
            <label
              htmlFor="name"
              className="block text-sm font-medium text-slate-700"
            >
              Warehouse Name
            </label>
            <input
              id="name"
              type="text"
              className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-900 focus:bg-white focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none"
              placeholder="Enter warehouse name"
              {...register('name')}
              aria-invalid={errors.name ? 'true' : 'false'}
            />
            {errors.name && (
              <p className="text-sm text-red-600">{errors.name.message}</p>
            )}
          </div>

          {/* Contact Info Input */}
          <div className="space-y-1.5">
            <label
              htmlFor="location"
              className="block text-sm font-medium text-slate-700"
            >
              Location
            </label>
            <input
              id="location"
              type="text"
              className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-900 focus:bg-white focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none"
              placeholder="Location (City)"
              {...register('location')}
              aria-invalid={errors.location ? 'true' : 'false'}
            />
            {errors.location && (
              <p className="text-sm text-red-600">{errors.location.message}</p>
            )}
          </div>

          {/* Address Input */}
          <div className="space-y-1.5">
            <label
              htmlFor="address"
              className="block text-sm font-medium text-slate-700"
            >
              Address
            </label>
            <textarea
              id="address"
              rows={3}
              className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-900 focus:bg-white focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none resize-none"
              placeholder="Enter full address"
              {...register('address')}
              aria-invalid={errors.address ? 'true' : 'false'}
            />
            {errors.address && (
              <p className="text-sm text-red-600">{errors.address.message}</p>
            )}
          </div>

          <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
            <div className="space-y-1.5">
              <label
                htmlFor="capacity"
                className="block text-sm font-medium text-slate-700"
              >
                Capacity
              </label>
              <input
                id="capacity"
                type="number"
                min={0}
                className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-900 focus:bg-white focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none"
                {...register('capacity')}
                aria-invalid={errors.capacity ? 'true' : 'false'}
              />
              {errors.capacity && (
                <p className="text-sm text-red-600">
                  {errors.capacity.message}
                </p>
              )}
            </div>

            <div className="space-y-1.5">
              <label
                htmlFor="managerName"
                className="block text-sm font-medium text-slate-700"
              >
                Manager's name
              </label>
              <input
                id="managerName"
                type="text"
                className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-900 focus:bg-white focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none"
                placeholder="Enter manager's name"
                {...register('managerName')}
                aria-invalid={errors.managerName ? 'true' : 'false'}
              />
              {errors.managerName && (
                <p className="text-sm text-red-600">
                  {errors.managerName.message}
                </p>
              )}
            </div>

            <div className="space-y-1.5">
              <label
                htmlFor="status"
                className="block text-sm font-medium text-slate-700"
              >
                Status
              </label>
              <select
                id="status"
                className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-900 focus:bg-white focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none"
                {...register('status')}
                aria-invalid={errors.status ? 'true' : 'false'}
              >
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
              {errors.status && (
                <p className="text-sm text-red-600">{errors.status.message}</p>
              )}
            </div>
          </div>

          {/* Server Error Message */}
          {serverError && (
            <div className="p-3 rounded-lg bg-red-50 border border-red-100 text-sm text-red-600">
              {serverError}
            </div>
          )}

          {/* Submit Button */}
          <div className="pt-4">
            <button
              type="submit"
              disabled={submitting}
              className="w-full bg-primary hover:bg-primary-dark text-white font-semibold py-3.5 rounded-lg shadow-md transition-all disabled:opacity-70 disabled:cursor-not-allowed flex justify-center items-center"
            >
              {submitting ? 'Processing...' : submitLabel}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default WarehouseForm;
