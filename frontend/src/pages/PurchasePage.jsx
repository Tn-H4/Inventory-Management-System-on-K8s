import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PurchaseForm from '../features/transactions/PurchaseForm';
import { purchaseFormDefaultValues } from '../features/transactions/purchaseSchema';
import { usePurchaseTransaction } from '../features/transactions/api';
import { useProducts } from '../features/products/api';
import { useSuppliers } from '../features/suppliers/api';
import { LoadingState } from '../components/EmptyState';
import ProposalSuccessBanner from '../components/ProposalSuccessBanner';

function PurchasePage() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');
  const [proposalSubmitted, setProposalSubmitted] = useState(false);

  const { mutate, isPending, error } = usePurchaseTransaction();

  const { data: productsData, isLoading: isLoadingProducts } = useProducts({
    page: 1,
    limit: 9999,
  });
  const { data: suppliersData, isLoading: isLoadingSuppliers } = useSuppliers({
    page: 1,
    limit: 9999,
  });

  const products = useMemo(() => productsData?.products ?? [], [productsData]);
  const suppliers = useMemo(
    () => suppliersData?.suppliers ?? [],
    [suppliersData]
  );

  const handleSubmit = (formData) => {
    setServerError('');
    const payload = {
      ...formData,
      transactionType: 'PURCHASE',
    };

    mutate(payload, {
      onSuccess: (data) => {
        if (data?.proposal) {
          setProposalSubmitted(true);
        } else {
          navigate('/transactions');
        }
      },
      onError: (err) => {
        setServerError(err.message || 'Failed to create purchase.');
      },
    });
  };

  const isLoading = isLoadingProducts || isLoadingSuppliers;

  if (proposalSubmitted) {
    return (
      <ProposalSuccessBanner
        requestType="purchase"
        onViewTransactions={() => navigate('/transactions')}
        onSubmitAnother={() => setProposalSubmitted(false)}
      />
    );
  }

  return (
    <div className="space-y-6">
      {isLoading ? (
        <LoadingState
          title="Loading data..."
          message="Please wait while we fetch products and suppliers."
        />
      ) : (
        <PurchaseForm
          defaultValues={purchaseFormDefaultValues}
          submitLabel="Create Purchase"
          onSubmit={handleSubmit}
          isSubmitting={isPending}
          serverError={serverError || error?.message}
          products={products}
          suppliers={suppliers}
        />
      )}
    </div>
  );
}

export default PurchasePage;
