import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ProfileForm from '../features/users/ProfileForm';
import { profileFormDefaultValues } from '../features/users/profileSchema';
import {
  useChangePassword,
  useCurrentUser,
  useUpdateUser,
} from '../features/users/api';
import { ErrorState, LoadingState } from '../components/EmptyState';
import { LoadingSpinner } from '../components/LoadingSpinner';

function ProfilePage() {
  const {
    data: currentUserData,
    isLoading,
    isError,
    error: userError,
  } = useCurrentUser();

  const { mutate, isPending, error: updateError } = useUpdateUser();
  const changePasswordMutation = useChangePassword();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const [pwFields, setPwFields] = useState({
    currentPassword: '',
    newPassword: '',
    confirmNewPassword: '',
  });
  const [pwError, setPwError] = useState('');
  const [pwSuccess, setPwSuccess] = useState('');

  const defaultValues = useMemo(() => {
    if (!currentUserData) {
      return profileFormDefaultValues;
    }
    return {
      name: currentUserData.name ?? '',
      email: currentUserData.email ?? '',
      phoneNumber: currentUserData.phoneNumber ?? '',
      password: '',
      confirmPassword: '',
    };
  }, [currentUserData]);

  const handleSubmit = (formData) => {
    setServerError('');
    setSuccessMessage('');
    const { password, confirmPassword, ...rest } = formData;

    const payload = { ...rest };

    if (password) {
      payload.password = password;
    }

    mutate(
      { id: currentUserData.id, data: payload },
      {
        onSuccess: () => {
          setSuccessMessage('Profile updated successfully.');
        },
        onError: (err) => {
          setServerError(err.message || 'Failed to update profile.');
          setSuccessMessage('');
        },
      }
    );
  };

  const handleChangePassword = (e) => {
    e.preventDefault();
    setPwError('');
    setPwSuccess('');
    if (pwFields.newPassword !== pwFields.confirmNewPassword) {
      setPwError('New passwords do not match.');
      return;
    }
    if (pwFields.newPassword.length < 6) {
      setPwError('New password must be at least 6 characters.');
      return;
    }
    changePasswordMutation.mutate(
      {
        id: currentUserData.id,
        currentPassword: pwFields.currentPassword,
        newPassword: pwFields.newPassword,
      },
      {
        onSuccess: () => {
          setPwSuccess('Password changed successfully.');
          setPwFields({
            currentPassword: '',
            newPassword: '',
            confirmNewPassword: '',
          });
        },
        onError: (err) => {
          setPwError(
            err.response?.data?.message ||
              err.message ||
              'Failed to change password.'
          );
        },
      }
    );
  };

  if (isLoading) {
    return (
      <LoadingState
        title="Loading profile..."
        message="Please wait while we fetch your profile."
      />
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Failed to load profile"
        message={
          userError?.message || 'An error occurred while loading your profile.'
        }
      />
    );
  }

  return (
    <section>
      <header className="space-y-2">
        <h1 className="text-3xl font-semibold text-slate-900">
          Hello, {currentUserData.name}
        </h1>
        <p className="text-sm text-slate-500">
          Keep your personal information up to date and update your password
          when needed.
        </p>
      </header>

      {successMessage && (
        <div className="mt-6 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {successMessage}
        </div>
      )}

      <div className="mt-8">
        <ProfileForm
          defaultValues={defaultValues}
          submitLabel="Update Profile"
          onSubmit={handleSubmit}
          isSubmitting={isPending}
          serverError={serverError || updateError?.message}
        />
      </div>

      <div className="mt-10 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-lg font-semibold text-slate-900">
          Change Password
        </h2>
        {pwSuccess && (
          <div className="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
            {pwSuccess}
          </div>
        )}
        {pwError && (
          <div className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {pwError}
          </div>
        )}
        <form onSubmit={handleChangePassword} className="space-y-4">
          {[
            { label: 'Current Password', key: 'currentPassword' },
            { label: 'New Password', key: 'newPassword' },
            { label: 'Confirm New Password', key: 'confirmNewPassword' },
          ].map(({ label, key }) => (
            <div key={key}>
              <label className="mb-1 block text-sm font-medium text-slate-700">
                {label}
              </label>
              <input
                type="password"
                autocomplete={
                  key === 'currentPassword'
                    ? 'current-password'
                    : 'new-password'
                }
                value={pwFields[key]}
                onChange={(e) =>
                  setPwFields((prev) => ({ ...prev, [key]: e.target.value }))
                }
                required
                className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-900 focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/20"
              />
            </div>
          ))}
          <button
            type="submit"
            disabled={changePasswordMutation.isPending}
            className="rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-dark disabled:opacity-50 transition-all"
          >
            {changePasswordMutation.isPending ? (
              <span className="flex items-center justify-center gap-2">
                <LoadingSpinner size="sm" color="white" />
                <span>Saving...</span>
              </span>
            ) : (
              'Change Password'
            )}
          </button>
        </form>
      </div>
    </section>
  );
}

export default ProfilePage;
