import React, { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import CategoryForm from '../features/categories/CategoryForm';
import { categoryFormDefaultValues } from '../features/categories/schema';
import { useCategory, useUpdateCategory } from '../features/categories/api';
import { EmptyState, ErrorState, LoadingState } from '../components/EmptyState';

function getCategoryStatus(category) {
  if (!category) return 'ACTIVE';

  if (typeof category.status === 'string') {
    return category.status.toUpperCase() === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE';
  }

  if (typeof category.isActive === 'boolean') {
    return category.isActive ? 'ACTIVE' : 'INACTIVE';
  }

  if (typeof category.isActive === 'string') {
    return category.isActive.toUpperCase() === 'INACTIVE'
      ? 'INACTIVE'
      : 'ACTIVE';
  }

  return 'ACTIVE';
}

function CategoryEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const {
    data: categoryData,
    isLoading: isLoadingCategory,
    isError: isErrorCategory,
    error: categoryError,
  } = useCategory(id);

  const { mutate, isPending, error: updateError } = useUpdateCategory();
  const [serverError, setServerError] = useState('');

  const defaultValues = useMemo(() => {
    const category = categoryData?.category;
    if (!category) {
      return categoryFormDefaultValues;
    }
    return {
      name: category.name ?? '',
      description: category.description ?? '',
      status: getCategoryStatus(category),
    };
  }, [categoryData]);

  const handleSubmit = (formData) => {
    setServerError('');
    mutate(
      { id, data: formData },
      {
        onSuccess: () => {
          navigate('/categories');
        },
        onError: (err) => {
          setServerError(err.message || 'Failed to update category.');
        },
      }
    );
  };

  if (isLoadingCategory) {
    return (
      <LoadingState
        title="Loading category..."
        message="Please wait while we fetch the category data."
      />
    );
  }

  if (isErrorCategory) {
    return (
      <ErrorState
        title="Failed to load category"
        message={
          categoryError?.message ||
          'An error occurred while loading the category.'
        }
      />
    );
  }

  if (!categoryData) {
    return (
      <EmptyState
        title="Category not found"
        description="The category you're looking for doesn't exist or has been removed."
      />
    );
  }

  return (
    <div className="py-8 px-4 mx-auto max-w-5xl">
      <div className="text-center mb-10">
        <h1 className="text-2xl font-display font-bold text-slate-900">
          Edit Category
        </h1>
        <p className="text-slate-500 mt-2">
          Update the information for {categoryData.category.name}
        </p>
      </div>

      <CategoryForm
        defaultValues={defaultValues}
        submitLabel="Update Category"
        onSubmit={handleSubmit}
        isSubmitting={isPending}
        serverError={serverError || updateError?.message}
      />
    </div>
  );
}

export default CategoryEditPage;
