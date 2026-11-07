import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { z } from 'zod';
import api from '../lib/axios';
import { LoadingSpinner } from '../components/LoadingSpinner';

const loginSchema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

const defaultValues = {
  email: '',
  password: '',
};

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname ?? '/dashboard';
  const justRegistered = location.state?.registered ?? false;
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(loginSchema),
    defaultValues,
    mode: 'onChange',
  });

  const onSubmit = async (values) => {
    setServerError('');
    try {
      const response = await api.post('/auth/login', values);

      const token = response.data?.token;
      const role = response.data?.role;

      if (!token || !role) {
        setServerError('Login succeeded but no token or role was returned.');
        return;
      }

      localStorage.setItem('token', token);
      localStorage.setItem('userRole', role);

      if (role !== 'ADMIN') {
        navigate('/dashboard', { replace: true });
      } else {
        // Admins are safe to go back to wherever they came from
        navigate(from === '/login' ? '/dashboard' : from, { replace: true });
      }
    } catch (error) {
      setServerError(error.message || 'Unable to sign in.');
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-100 via-slate-50 to-slate-100 p-4">
      {/* Decorative Background Elements */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-primary/5 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-accent/5 rounded-full blur-3xl" />
      </div>

      <div className="relative w-full max-w-md">
        {/* Logo / Brand Section */}
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

        {/* Login Card */}
        <div className="bg-white rounded-3xl shadow-elevated p-8 animate-scale-in">
          <h2 className="text-xl font-display font-semibold text-slate-900 mb-2">
            Welcome back
          </h2>
          <p className="text-sm text-slate-500 mb-6">
            Sign in to continue to your dashboard
          </p>

          {justRegistered && (
            <div className="flex items-center gap-2 px-4 py-3 mb-4 rounded-xl bg-green-50 border border-green-200 text-green-800 text-sm font-medium">
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
                  d="M5 13l4 4L19 7"
                />
              </svg>
              Account created successfully. You can now sign in.
            </div>
          )}

          <form
            onSubmit={handleSubmit(onSubmit)}
            className="space-y-5"
            noValidate
          >
            {/* Email Field */}
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
                  className={`
                    w-full pl-12 pr-4 py-3 text-sm text-slate-800
                    bg-slate-50 border-2 border-transparent rounded-xl
                    placeholder:text-slate-400
                    focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20
                    transition-all duration-200
                    ${errors.email ? 'border-error bg-error-light' : ''}
                  `}
                  {...register('email')}
                  aria-invalid={errors.email ? 'true' : 'false'}
                />
              </div>
              {errors.email && (
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
                  {errors.email.message}
                </p>
              )}
            </div>

            {/* Password Field */}
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
                  autoComplete="current-password"
                  placeholder="Enter your password"
                  className={`
                    w-full pl-12 pr-4 py-3 text-sm text-slate-800
                    bg-slate-50 border-2 border-transparent rounded-xl
                    placeholder:text-slate-400
                    focus:bg-white focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20
                    transition-all duration-200
                    ${errors.password ? 'border-error bg-error-light' : ''}
                  `}
                  {...register('password')}
                  aria-invalid={errors.password ? 'true' : 'false'}
                />
              </div>
              {errors.password && (
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
                  {errors.password.message}
                </p>
              )}
              {serverError && (
                <p className="flex items-center gap-1 text-sm text-error font-medium">
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
                  {serverError}
                </p>
              )}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full flex items-center justify-center gap-2 px-4 py-3.5 text-sm font-bold text-white bg-primary hover:bg-primary-dark rounded-xl shadow-soft hover:shadow-card transition-all duration-200 disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {isSubmitting ? (
                <>
                  <LoadingSpinner size="sm" color="white" />
                  <span>Signing in...</span>
                </>
              ) : (
                'Sign In'
              )}
            </button>
          </form>
        </div>

        <p className="text-center text-sm text-slate-500 mt-6">
          Don&apos;t have an account?{' '}
          <Link
            to="/register"
            className="font-semibold text-primary hover:text-primary-dark transition-colors"
          >
            Create one
          </Link>
        </p>

        {/* Footer */}
        <p className="text-center text-xs text-slate-400 mt-6">
          This is a project for Web Application Development course, developed by
          Le Thai Minh Tin (ITCSIU24086) and Huynh Ngoc Thuy Tien (ITITIU24065)
        </p>
      </div>
    </div>
  );
}

export default LoginPage;
