import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  useTransaction,
  useUpdateTransaction,
} from '../features/transactions/api';
import { ErrorState, LoadingState } from '../components/EmptyState';
import { LoadingSpinner } from '../components/LoadingSpinner';

const DetailCard = ({ title, children }) => (
  <div className="h-full rounded-xl border border-slate-200 bg-white p-5 shadow-soft">
    <h3 className="mb-4 text-lg font-display font-bold text-slate-900">
      {title}
    </h3>
    <div className="space-y-3 text-sm">{children}</div>
  </div>
);

const DetailRow = ({ label, value }) => (
  <div className="flex justify-between">
    <span className="text-slate-500">{label}</span>
    <span className="font-semibold text-slate-900">{value}</span>
  </div>
);

function TransactionDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data, isLoading, isError, error } = useTransaction(id);
  const [status, setStatus] = useState('');
  const [serverError, setServerError] = useState('');
  const userRole = localStorage.getItem('userRole');
  const canUpdateStatus = userRole === 'ADMIN' || userRole === 'MANAGER';

  const updateMutation = useUpdateTransaction();

  useEffect(() => {
    if (data?.transaction) {
      setStatus(data.transaction.status);
    }
  }, [data]);

  const handleUpdateStatus = () => {
    setServerError('');
    updateMutation.mutate(
      { id, status },
      {
        onSuccess: () => {
          navigate('/transactions');
        },
        onError: (err) => {
          setServerError(err.message || 'Failed to update status.');
        },
      }
    );
  };

  if (isLoading) {
    return (
      <LoadingState
        title="Loading transaction..."
        message="Please wait while we fetch the transaction details."
      />
    );
  }

  if (isError || !data?.transaction) {
    return (
      <ErrorState
        title="Failed to load transaction"
        message={
          error?.message || 'An error occurred while loading the transaction.'
        }
      />
    );
  }

  const tx = data.transaction;

  return (
    <div className="min-h-[600px] rounded-2xl border border-slate-200 bg-white p-8 shadow-soft">
      <div className="mb-8">
        <h1 className="text-2xl font-display font-bold text-slate-900">
          Transaction Details
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          ID: {tx.id} ({tx.transactionType})
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <DetailCard title="Transaction Information">
          <DetailRow label="Type" value={tx.transactionType} />
          <DetailRow label="Status" value={tx.status} />
          <DetailRow label="Total Products" value={tx.totalProducts} />
          <DetailRow
            label="Total Price"
            value={`$${tx.totalPrice?.toFixed(2)}`}
          />
          <DetailRow
            label="Created At"
            value={new Date(tx.createdAt).toLocaleDateString()}
          />
          <DetailRow label="Description" value={tx.description || 'N/A'} />
          <DetailRow label="Note" value={tx.note || 'N/A'} />
        </DetailCard>

        <DetailCard title="Product Information">
          <DetailRow label="Name" value={tx.product?.name || 'Unknown'} />
          <DetailRow label="SKU" value={tx.product?.sku || 'N/A'} />
          <DetailRow
            label="Price"
            value={tx.product?.price ? `$${tx.product.price}` : 'N/A'}
          />
        </DetailCard>

        <DetailCard title="User Information">
          <DetailRow label="Name" value={tx.user?.name || 'Unknown'} />
          <DetailRow label="Email" value={tx.user?.email || 'N/A'} />
          <DetailRow label="Phone" value={tx.user?.phoneNumber || 'N/A'} />
          <DetailRow label="Role" value={tx.user?.role || 'N/A'} />
        </DetailCard>

        {tx.supplier ? (
          <DetailCard title="Supplier Information">
            <DetailRow label="Name" value={tx.supplier.name} />
            <DetailRow label="Contact" value={tx.supplier.contactInfo} />
            <DetailRow label="Address" value={tx.supplier.address} />
          </DetailCard>
        ) : (
          <div className="hidden lg:block"></div>
        )}

        {canUpdateStatus && (
          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-soft">
            <h3 className="mb-4 text-lg font-display font-bold text-slate-900">
              Update Status
            </h3>
            <div className="space-y-3">
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                aria-label="Transaction status"
                className="w-full rounded-xl border-2 border-transparent bg-slate-50 px-3 py-2.5 text-sm focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/20 transition-all"
              >
                <option value="PENDING">Pending</option>
                <option value="PROCESSING">Processing</option>
                <option value="COMPLETED">Completed</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
              <button
                type="button"
                onClick={handleUpdateStatus}
                disabled={updateMutation.isPending}
                className="w-full rounded-xl bg-accent py-2.5 text-sm font-semibold text-white shadow-soft hover:shadow-card transition-all disabled:opacity-60"
              >
                {updateMutation.isPending ? (
                  <span className="flex items-center justify-center gap-2">
                    <LoadingSpinner size="sm" color="white" />
                    <span>Updating...</span>
                  </span>
                ) : (
                  'Update Status'
                )}
              </button>
              {serverError && (
                <p className="text-xs text-error font-medium">{serverError}</p>
              )}
            </div>
          </div>
        )}

        <div className="flex items-end justify-end">
          <button
            onClick={() => navigate(-1)}
            className="flex items-center gap-2 rounded-xl bg-primary px-6 py-3 text-sm font-bold text-white shadow-soft hover:bg-primary-dark hover:shadow-card transition-all"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            GO BACK
          </button>
        </div>
      </div>
    </div>
  );
}

export default TransactionDetailsPage;
