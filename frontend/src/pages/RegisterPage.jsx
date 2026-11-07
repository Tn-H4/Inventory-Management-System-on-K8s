import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate, Link } from 'react-router-dom';
import { z } from 'zod';
import api from '../lib/axios';
import { LoadingSpinner } from '../components/LoadingSpinner';

const registerSchema = z
  .object({
    name: z.string().min(1, 'Name is required'),
    email: z.string().email('Enter a valid email address'),
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
    phoneNumber: z.string().optional(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

const defaultValues = {
  name: '',
  email: '',
  password: '',
  confirmPassword: '',
  phoneNumber: '',
};

function RegisterPage() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(registerSchema),
    defaultValues,
    mode: 'onChange',
  });

  const onSubmit = async (values) => {
    setServerError('');
    try {
      await api.post('/auth/register', {
        name: values.name,
        email: values.email,
        password: values.password,
        phoneNumber: values.phoneNumber || undefined,
      });
      navigate('/login', { state: { registered: true } });
    } catch (error) {
      setServerError(error.message || 'Registration failed. Please try again.');
    }
  };

  const fieldClass = (hasError) =>
    [
      'w-full pl-12 pr-4 py-3 text-sm text-slate-800',
      'bg-slate-50 border-2 border-transparent rounded-xl',
      'placeholder:text-slate-400',
      'focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20',
      'transition-all duration-200',
      hasError ? 'border-error bg-error-light' : '',
    ]
      .filter(Boolean)
      .join(' ');

  const errorMsg = (message) => (
    <p
      className="flex items-center gap-1 text-sm text-error font-medium"
      role="alert"
    >
      <svg
        className="w-4 h-4 flex-shrink-0"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        aria-hidden="true"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
        />
      </svg>
      {message}
    </p>
  );

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-100 via-slate-50 to-slate-100 p-4">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-primary/5 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-accent/5 rounded-full blur-3xl" />
      </div>

      <div className="relative w-full max-w-md">
        {/* Brand */}
        <div className="text-center mb-8 animate-slide-down">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-white shadow-elevated mb-4">
            <img
              src="/favicon.ico"
              alt=""
              className="h-10 w-10"
              aria-hidden="true"
            />
          </div>
          <h1 className="text-3xl font-display font-bold text-slate-900">
            Inventory System
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Warehouse Management Platform
          </p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-3xl shadow-elevated p-8 animate-scale-in">
          <h2 className="text-xl font-display font-semibold text-slate-900 mb-2">
            Create an account
          </h2>
          <p className="text-sm text-slate-500 mb-6">
            New accounts start with Staff access. Contact an administrator to
            request elevated permissions.
          </p>

          <form
            onSubmit={handleSubmit(onSubmit)}
            className="space-y-5"
            noValidate
          >
            {/* Name */}
            <div className="space-y-1.5">
              <label
                htmlFor="name"
                className="block text-sm font-semibold text-slate-700"
              >
                Full Name
              </label>
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
                      strokeWidth={1.5}
                      d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                    />
                  </svg>
                </span>
                <input
                  id="name"
                  type="text"
                  autoComplete="name"
                  placeholder="Enter your full name"
                  className={fieldClass(!!errors.name)}
                  {...register('name')}
                  aria-invalid={errors.name ? 'true' : 'false'}
                />
              </div>
              {errors.name && errorMsg(errors.name.message)}
            </div>

            {/* Email */}
            <div className="space-y-1.5">
              <label
                htmlFor="email"
                className="block text-sm font-semibold text-slate-700"
              >
                Email Address
              </label>
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
                      strokeWidth={1.5}
                      d="M16 12a4 4 0 10-8 0 4 4 0 008 0zm0 0v1.5a2.5 2.5 0 005 0V12a9 9 0 10-9 9m4.5-1.206a8.959 8.959 0 01-4.5 1.207"
                    />
                  </svg>
                </span>
                <input
                  id="email"
                  type="email"
                  autoComplete="email"
                  placeholder="Enter your email"
                  className={fieldClass(!!errors.email)}
                  {...register('email')}
                  aria-invalid={errors.email ? 'true' : 'false'}
                />
              </div>
              {errors.email && errorMsg(errors.email.message)}
            </div>

            {/* Phone (optional) */}
            <div className="space-y-1.5">
              <label
                htmlFor="phoneNumber"
                className="block text-sm font-semibold text-slate-700"
              >
                Phone Number{' '}
                <span className="text-slate-400 font-normal">(optional)</span>
              </label>
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
                      strokeWidth={1.5}
                      d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"
                    />
                  </svg>
                </span>
                <input
                  id="phoneNumber"
                  type="tel"
                  autoComplete="tel"
                  placeholder="Enter your phone number"
                  className={fieldClass(false)}
                  {...register('phoneNumber')}
                />
              </div>
            </div>

            {/* Password */}
            <div className="space-y-1.5">
              <label
                htmlFor="password"
                className="block text-sm font-semibold text-slate-700"
              >
                Password
              </label>
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
                      strokeWidth={1.5}
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
                    />
                  </svg>
                </span>
                <input
                  id="password"
                  type="password"
                  autoComplete="new-password"
                  placeholder="At least 8 characters"
                  className={fieldClass(!!errors.password)}
                  {...register('password')}
                  aria-invalid={errors.password ? 'true' : 'false'}
                />
              </div>
              {errors.password && errorMsg(errors.password.message)}
            </div>

            {/* Confirm Password */}
            <div className="space-y-1.5">
              <label
                htmlFor="confirmPassword"
                className="block text-sm font-semibold text-slate-700"
              >
                Confirm Password
              </label>
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
                      strokeWidth={1.5}
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
                    />
                  </svg>
                </span>
                <input
                  id="confirmPassword"
                  type="password"
                  autoComplete="new-password"
                  placeholder="Re-enter your password"
                  className={fieldClass(!!errors.confirmPassword)}
                  {...register('confirmPassword')}
                  aria-invalid={errors.confirmPassword ? 'true' : 'false'}
                />
              </div>
              {errors.confirmPassword &&
                errorMsg(errors.confirmPassword.message)}
              {serverError && errorMsg(serverError)}
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full flex items-center justify-center gap-2 px-4 py-3.5 text-sm font-bold text-white bg-primary hover:bg-primary-dark rounded-xl shadow-soft hover:shadow-card transition-all duration-200 disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {isSubmitting ? (
                <>
                  <LoadingSpinner size="sm" color="white" />
                  <span>Creating account...</span>
                </>
              ) : (
                'Create Account'
              )}
            </button>
          </form>

          <p className="text-center text-sm text-slate-500 mt-6">
            Already have an account?{' '}
            <Link
              to="/login"
              className="font-semibold text-primary hover:text-primary-dark transition-colors"
            >
              Sign in
            </Link>
          </p>
        </div>

        <p className="text-center text-xs text-slate-400 mt-6">
          This is a project for Web Application Development course, developed by
          Le Thai Minh Tin (ITCSIU24086) and Huynh Ngoc Thuy Tien (ITITIU24065)
        </p>
      </div>
    </div>
  );
}

export default RegisterPage;
