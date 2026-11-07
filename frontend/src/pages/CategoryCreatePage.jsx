import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import CategoryForm from '../features/categories/CategoryForm';
import { categoryFormDefaultValues } from '../features/categories/schema';
import { useCreateCategory } from '../features/categories/api';
import { LoadingState } from '../components/EmptyState';

function CategoryCreatePage() {
  const navigate = useNavigate();
  const { mutate, isPending, error } = useCreateCategory();
  const [serverError, setServerError] = useState('');

  const handleSubmit = (formData) => {
    setServerError('');
    mutate(formData, {
      onSuccess: () => {
        navigate('/categories');
      },
      onError: (err) => {
        setServerError(err.message || 'Failed to create category.');
      },
    });
  };

  return (
    <div className="py-8 px-4 mx-auto max-w-5xl">
      <div className="text-center mb-10">
        <h1 className="text-3xl font-bold text-slate-900">
          Create New Category
        </h1>
        <p className="text-slate-500 mt-2">
          Fill in the information below to add a new category
        </p>
      </div>

      <CategoryForm
        defaultValues={categoryFormDefaultValues}
        submitLabel="Add Category"
        onSubmit={handleSubmit}
        isSubmitting={isPending}
        serverError={serverError || error?.message}
      />
    </div>
  );
}

export default CategoryCreatePage;
