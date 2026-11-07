import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { profileSchema, profileFormDefaultValues } from './profileSchema';

function ProfileForm({
  defaultValues = profileFormDefaultValues,
  submitLabel = 'Save Changes',
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
    resolver: zodResolver(profileSchema),
    defaultValues,
  });

  React.useEffect(() => {
    reset(defaultValues);
  }, [defaultValues, reset]);

  const submitting = isSubmitting || isSubmittingProp;

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-8" noValidate>
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div className="space-y-2">
          <label
            htmlFor="name"
            className="text-sm font-semibold text-slate-600"
          >
            Name
          </label>
          <input
            id="name"
            type="text"
            className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-900 placeholder:text-slate-400 focus:border-[#6e56cf] focus:outline-none focus:ring-2 focus:ring-[#6e56cf]/30"
            {...register('name')}
            aria-invalid={errors.name ? 'true' : 'false'}
          />
          {errors.name ? (
            <p className="text-xs text-red-600" role="alert">
              {errors.name.message}
            </p>
          ) : null}
        </div>

        <div className="space-y-2">
          <label
            htmlFor="email"
            className="text-sm font-semibold text-slate-600"
          >
            Email
          </label>
          <input
            id="email"
            type="email"
            className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-900 placeholder:text-slate-400 focus:border-[#6e56cf] focus:outline-none focus:ring-2 focus:ring-[#6e56cf]/30"
            {...register('email')}
            aria-invalid={errors.email ? 'true' : 'false'}
          />
          {errors.email ? (
            <p className="text-xs text-red-600" role="alert">
              {errors.email.message}
            </p>
          ) : null}
        </div>

        <div className="space-y-2 md:col-span-2">
          <label
            htmlFor="phoneNumber"
            className="text-sm font-semibold text-slate-600"
          >
            Phone Number
          </label>
          <input
            id="phoneNumber"
            type="text"
            className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-900 placeholder:text-slate-400 focus:border-[#6e56cf] focus:outline-none focus:ring-2 focus:ring-[#6e56cf]/30"
            {...register('phoneNumber')}
            aria-invalid={errors.phoneNumber ? 'true' : 'false'}
          />
          {errors.phoneNumber ? (
            <p className="text-xs text-red-600" role="alert">
              {errors.phoneNumber.message}
            </p>
          ) : null}
        </div>
      </div>

      <div className="h-px w-full bg-slate-200" />

      {serverError ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {serverError}
        </div>
      ) : null}

      <div className="flex justify-end">
        <button
          type="submit"
          className="inline-flex items-center rounded-full bg-[#6e56cf] px-8 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-[#5a46b5] focus:outline-none focus:ring-2 focus:ring-[#6e56cf]/40 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={submitting}
        >
          {submitting ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  );
}

export default ProfileForm;
