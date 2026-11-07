import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import UserForm from '../features/users/UserForm';
import { userFormDefaultValues } from '../features/users/schema';
import { useCreateUser } from '../features/users/api';
import { LoadingState } from '../components/EmptyState';

function UserCreatePage() {
  const navigate = useNavigate();
  const { mutate, isPending, error } = useCreateUser();
  const [serverError, setServerError] = useState('');

  const handleSubmit = (formData) => {
    setServerError('');
    mutate(formData, {
      onSuccess: () => {
        navigate('/users');
      },
      onError: (err) => {
        setServerError(err.message || 'Failed to create user.');
      },
    });
  };

  return (
    <div className="py-8 px-4 mx-auto max-w-5xl">
      <div className="text-center mb-10">
        <h1 className="text-3xl font-bold text-slate-900">Create New User</h1>
        <p className="text-slate-500 mt-2">
          Fill in the information below to add a new user
        </p>
      </div>

      <UserForm
        defaultValues={userFormDefaultValues}
        submitLabel="Add Create"
        onSubmit={handleSubmit}
        isSubmitting={isPending}
        serverError={serverError || error?.message}
      />
    </div>
  );
}

export default UserCreatePage;
