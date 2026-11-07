import React, { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import WarehouseForm from '../features/warehouses/WarehouseForm';
import { warehouseFormDefaultValues } from '../features/warehouses/schema';
import { useWarehouse, useUpdateWarehouse } from '../features/warehouses/api';
import { EmptyState, ErrorState, LoadingState } from '../components/EmptyState';

function getWarehouseStatus(warehouse) {
  if (!warehouse) return 'ACTIVE';

  if (typeof warehouse.status === 'string') {
    return warehouse.status.toUpperCase() === 'INACTIVE'
      ? 'INACTIVE'
      : 'ACTIVE';
  }

  if (typeof warehouse.isActive === 'boolean') {
    return warehouse.isActive ? 'ACTIVE' : 'INACTIVE';
  }

  if (typeof warehouse.isActive === 'string') {
    return warehouse.isActive.toUpperCase() === 'INACTIVE'
      ? 'INACTIVE'
      : 'ACTIVE';
  }

  return 'ACTIVE';
}

function WarehouseEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const {
    data: warehouseData,
    isLoading: isLoadingWarehouse,
    isError: isErrorWarehouse,
    error: warehouseError,
  } = useWarehouse(id);

  const { mutate, isPending, error: updateError } = useUpdateWarehouse();
  const [serverError, setServerError] = useState('');

  const defaultValues = useMemo(() => {
    const warehouse = warehouseData?.warehouse;
    if (!warehouse) {
      return warehouseFormDefaultValues;
    }
    return {
      name: warehouse.name ?? '',
      location: warehouse.location ?? '',
      address: warehouse.address ?? '',
      capacity: `${warehouse.capacity ?? ''}`,
      managerName: warehouse.managerName ?? '',
      status: getWarehouseStatus(warehouse),
    };
  }, [warehouseData]);

  const handleSubmit = (formData) => {
    setServerError('');
    mutate(
      { id, data: formData },
      {
        onSuccess: () => {
          navigate('/warehouses');
        },
        onError: (err) => {
          setServerError(err.message || 'Failed to update warehouse.');
        },
      }
    );
  };

  if (isLoadingWarehouse) {
    return (
      <LoadingState
        title="Loading warehouse..."
        message="Please wait while we fetch the warehouse data."
      />
    );
  }

  if (isErrorWarehouse) {
    return (
      <ErrorState
        title="Failed to load warehouse"
        message={
          warehouseError?.message ||
          'An error occurred while loading the warehouse.'
        }
      />
    );
  }

  if (!warehouseData) {
    return (
      <EmptyState
        title="Warehouse not found"
        description="The warehouse you're looking for doesn't exist or has been removed."
      />
    );
  }

  return (
    <div className="py-8 px-4 mx-auto max-w-5xl">
      <div className="text-center mb-10">
        <h1 className="text-3xl font-bold text-slate-900">Edit Warehouse</h1>
        <p className="text-slate-500 mt-2">
          Update the information for {warehouseData.warehouse.name}
        </p>
      </div>

      <WarehouseForm
        defaultValues={defaultValues}
        submitLabel="Update Warehouse"
        onSubmit={handleSubmit}
        isSubmitting={isPending}
        serverError={serverError || updateError?.message}
      />
    </div>
  );
}

export default WarehouseEditPage;
