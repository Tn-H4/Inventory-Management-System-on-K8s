import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ProductForm from '../features/products/ProductForm';
import { productFormDefaultValues } from '../features/products/schema';
import { useCreateProduct } from '../features/products/api';
import { useCategories } from '../features/categories/api';
import { useWarehouses } from '../features/warehouses/api';

function ProductCreatePage() {
  const navigate = useNavigate();
  const { mutate, isPending } = useCreateProduct();
  const [serverError, setServerError] = useState('');
  const [imageFile, setImageFile] = useState(null);
  const [imageUrl, setImageUrl] = useState('');

  const { data: categoriesData } = useCategories({ limit: 1000 });
  const categories = categoriesData?.categories ?? [];

  const { data: warehousesData } = useWarehouses();
  const warehouses = warehousesData?.warehouses ?? [];

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImageFile(file);
      setImageUrl(URL.createObjectURL(file));
    }
  };

  const handleSubmit = (formData) => {
    setServerError('');

    const data = new FormData();
    Object.keys(formData).forEach((key) => {
      if (key === 'warehouseId') {
        if (formData[key]) data.append(key, formData[key]);
        return;
      }
      data.append(key, formData[key]);
    });

    if (imageFile) {
      data.append('imageFile', imageFile);
    }

    mutate(data, {
      onSuccess: () => {
        navigate('/products');
      },
      onError: (err) => {
        setServerError(err.message || 'Failed to create product.');
      },
    });
  };

  return (
    <div className="w-full min-h-full flex items-center justify-center">
      <div className="w-full max-w-[1500px] px-10">
        <ProductForm
          defaultValues={productFormDefaultValues}
          submitLabel="Create Product"
          onSubmit={handleSubmit}
          mode="create"
          isSubmitting={isPending}
          serverError={serverError}
          categories={categories}
          warehouses={warehouses}
          onImageChange={handleImageChange}
          imageUrl={imageUrl}
        />
      </div>
    </div>
  );
}

export default ProductCreatePage;
