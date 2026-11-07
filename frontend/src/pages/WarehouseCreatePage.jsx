import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import WarehouseForm from '../features/warehouses/WarehouseForm';
import { warehouseFormDefaultValues } from '../features/warehouses/schema';
import { useCreateWarehouse } from '../features/warehouses/api';
import { LoadingState } from '../components/EmptyState';

function WarehouseCreatePage() {
  const navigate = useNavigate();
  const { mutate, isPending, error } = useCreateWarehouse();
  const [serverError, setServerError] = useState('');

  const handleSubmit = (formData) => {
    setServerError('');
    mutate(formData, {
      onSuccess: () => {
        navigate('/warehouses');
      },
      onError: (err) => {
        setServerError(err.message || 'Failed to create warehouse.');
      },
    });
  };

  return (
    <div className="py-8 px-4 mx-auto max-w-5xl">
      <div className="text-center mb-10">
        <h1 className="text-3xl font-bold text-slate-900">
          Create New Warehouse
        </h1>
        <p className="text-slate-500 mt-2">
          Fill in the information below to add a new warehouse
        </p>
      </div>

      <WarehouseForm
        defaultValues={warehouseFormDefaultValues}
        submitLabel="Add Warehouse"
        onSubmit={handleSubmit}
        isSubmitting={isPending}
        serverError={serverError || error?.message}
      />
    </div>
  );
}

export default WarehouseCreatePage;
