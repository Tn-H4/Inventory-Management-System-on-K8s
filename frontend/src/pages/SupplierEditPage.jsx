import React, { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import SupplierForm from '../features/suppliers/SupplierForm';
import { supplierFormDefaultValues } from '../features/suppliers/schema';
import { useSupplier, useUpdateSupplier } from '../features/suppliers/api';
import { EmptyState, ErrorState, LoadingState } from '../components/EmptyState';

function SupplierEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const {
    data: supplierData,
    isLoading: isLoadingSupplier,
    isError: isErrorSupplier,
    error: supplierError,
  } = useSupplier(id);

  const { mutate, isPending, error: updateError } = useUpdateSupplier();
  const [serverError, setServerError] = useState('');

  const defaultValues = useMemo(() => {
    if (!supplierData) {
      return supplierFormDefaultValues;
    }
    return {
      name: supplierData.supplier.name ?? '',
      contactInfo: supplierData.supplier.contactInfo ?? '',
      address: supplierData.supplier.address ?? '',
      email: supplierData.supplier.email ?? '',
      website: supplierData.supplier.website ?? '',
      taxCode: supplierData.supplier.taxCode ?? '',
      status: supplierData.supplier.status ?? 'ACTIVE',
    };
  }, [supplierData]);

  const handleSubmit = (formData) => {
    setServerError('');
    mutate(
      { id, data: formData },
      {
        onSuccess: () => {
          navigate('/suppliers');
        },
        onError: (err) => {
          setServerError(err.message || 'Failed to update supplier.');
        },
      }
    );
  };

  if (isLoadingSupplier) {
    return (
      <LoadingState
        title="Loading supplier..."
        message="Please wait while we fetch the supplier data."
      />
    );
  }

  if (isErrorSupplier) {
    return (
      <ErrorState
        title="Failed to load supplier"
        message={
          supplierError?.message ||
          'An error occurred while loading the supplier.'
        }
      />
    );
  }

  if (!supplierData) {
    return (
      <EmptyState
        title="Supplier not found"
        description="The supplier you're looking for doesn't exist or has been removed."
      />
    );
  }

  return (
    <div className="py-8 px-4 mx-auto max-w-5xl">
      <div className="text-center mb-10">
        <h1 className="text-3xl font-bold text-slate-900">Edit Supplier</h1>
        <p className="text-slate-500 mt-2">
          Update the information for {supplierData.supplier.name}
        </p>
      </div>

      <SupplierForm
        defaultValues={defaultValues}
        submitLabel="Update Supplier"
        onSubmit={handleSubmit}
        isSubmitting={isPending}
        serverError={serverError || updateError?.message}
      />
    </div>
  );
}

export default SupplierEditPage;
