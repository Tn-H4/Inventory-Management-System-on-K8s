import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import SupplierForm from '../features/suppliers/SupplierForm';
import { supplierFormDefaultValues } from '../features/suppliers/schema';
import { useCreateSupplier } from '../features/suppliers/api';
import { LoadingState } from '../components/EmptyState';

function SupplierCreatePage() {
  const navigate = useNavigate();
  const { mutate, isPending, error } = useCreateSupplier();
  const [serverError, setServerError] = useState('');

  const handleSubmit = (formData) => {
    setServerError('');
    mutate(formData, {
      onSuccess: () => {
        navigate('/suppliers');
      },
      onError: (err) => {
        setServerError(err.message || 'Failed to create supplier.');
      },
    });
  };

  return (
    <div className="py-8 px-4 mx-auto max-w-5xl">
      <div className="text-center mb-10">
        <h1 className="text-3xl font-bold text-slate-900">
          Create New Supplier
        </h1>
        <p className="text-slate-500 mt-2">
          Fill in the information below to add a new supplier
        </p>
      </div>

      <SupplierForm
        defaultValues={supplierFormDefaultValues}
        submitLabel="Add Supplier"
        onSubmit={handleSubmit}
        isSubmitting={isPending}
        serverError={serverError || error?.message}
      />
    </div>
  );
}

export default SupplierCreatePage;
