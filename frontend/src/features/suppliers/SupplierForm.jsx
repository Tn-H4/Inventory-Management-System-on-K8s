import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { supplierSchema, supplierFormDefaultValues } from './schema';

function SupplierForm({
  defaultValues = supplierFormDefaultValues,
  submitLabel = 'Save Supplier',
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
    resolver: zodResolver(supplierSchema),
    defaultValues,
    mode: 'onChange',
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
              Supplier Name
            </label>
            <input
              id="name"
              type="text"
              className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-900 focus:bg-white focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none"
              placeholder="Enter supplier name"
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
              htmlFor="contactInfo"
              className="block text-sm font-medium text-slate-700"
            >
              Contact Info
            </label>
            <input
              id="contactInfo"
              type="text"
              className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-900 focus:bg-white focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none"
              placeholder="Phone Number"
              {...register('contactInfo')}
              aria-invalid={errors.contactInfo ? 'true' : 'false'}
            />
            {errors.contactInfo && (
              <p className="text-sm text-red-600">
                {errors.contactInfo.message}
              </p>
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
                htmlFor="email"
                className="block text-sm font-medium text-slate-700"
              >
                Email (Optional)
              </label>
              <input
                id="email"
                type="email"
                className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-900 focus:bg-white focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none"
                placeholder="supplier@example.com"
                {...register('email')}
                aria-invalid={errors.email ? 'true' : 'false'}
              />
              {errors.email && (
                <p className="text-sm text-red-600">{errors.email.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <label
                htmlFor="website"
                className="block text-sm font-medium text-slate-700"
              >
                Website (Optional)
              </label>
              <input
                id="website"
                type="text"
                className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-900 focus:bg-white focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none"
                placeholder="https://example.com"
                {...register('website')}
                aria-invalid={errors.website ? 'true' : 'false'}
              />
              {errors.website && (
                <p className="text-sm text-red-600">{errors.website.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <label
                htmlFor="taxCode"
                className="block text-sm font-medium text-slate-700"
              >
                Tax Code (Optional)
              </label>
              <input
                id="taxCode"
                type="text"
                className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-900 focus:bg-white focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none"
                placeholder="0123456789"
                {...register('taxCode')}
                aria-invalid={errors.taxCode ? 'true' : 'false'}
              />
              {errors.taxCode && (
                <p className="text-sm text-red-600">{errors.taxCode.message}</p>
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

export default SupplierForm;
