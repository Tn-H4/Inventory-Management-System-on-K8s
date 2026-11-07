import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import SellForm from '../features/transactions/SellForm';
import { sellFormDefaultValues } from '../features/transactions/sellSchema';
import { useSellTransaction } from '../features/transactions/api';
import { useProducts } from '../features/products/api';
import { LoadingState } from '../components/EmptyState';
import ProposalSuccessBanner from '../components/ProposalSuccessBanner';

function SellPage() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');
  const [proposalSubmitted, setProposalSubmitted] = useState(false);

  const { mutate, isPending, error } = useSellTransaction();

  const { data: productsData, isLoading: isLoadingProducts } = useProducts({
    page: 1,
    limit: 9999,
  });
  const products = useMemo(() => productsData?.products ?? [], [productsData]);

  const handleSubmit = (formData) => {
    setServerError('');
    const payload = {
      ...formData,
      transactionType: 'SALE',
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
        setServerError(err.message || 'Failed to create sale.');
      },
    });
  };

  if (proposalSubmitted) {
    return (
      <ProposalSuccessBanner
        requestType="sale"
        onViewTransactions={() => navigate('/transactions')}
        onSubmitAnother={() => setProposalSubmitted(false)}
      />
    );
  }

  return (
    <div className="space-y-6">
      {isLoadingProducts ? (
        <LoadingState
          title="Loading products..."
          message="Please wait while we fetch your products."
        />
      ) : (
        <SellForm
          defaultValues={sellFormDefaultValues}
          submitLabel="Create Sale"
          onSubmit={handleSubmit}
          isSubmitting={isPending}
          serverError={serverError || error?.message}
          products={products}
        />
      )}
    </div>
  );
}

export default SellPage;
