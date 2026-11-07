import React from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ErrorState, LoadingState } from '../components/EmptyState';
import { useConfirmation } from '../components/ConfirmationModal';
import { LoadingSpinner } from '../components/LoadingSpinner';
import {
  useApproveProposal,
  useProposal,
  useRejectProposal,
} from '../features/proposals/api';

function formatDate(value) {
  if (!value) return 'N/A';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return 'N/A';
  return d.toLocaleString('en-CA');
}

function formatCurrency(value) {
  if (value === null || value === undefined || value === '') return 'N/A';
  const amount = Number(value);
  if (Number.isNaN(amount)) return 'N/A';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
}

function formatType(type) {
  return type ? type.replaceAll('_', ' ') : 'N/A';
}

function getStatusBadge(status) {
  const styles = {
    PENDING: 'bg-warning-light text-warning',
    APPROVED: 'bg-success-light text-success',
    REJECTED: 'bg-error-light text-error',
  };
  const style = styles[status] || 'bg-slate-100 text-slate-700';
  return (
    <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${style}`}>
      {status || 'N/A'}
    </span>
  );
}

function DetailCard({ title, children }) {
  return (
    <div className="h-full rounded-xl border border-slate-200 bg-white p-5 shadow-soft">
      <h2 className="mb-4 text-lg font-display font-bold text-slate-900">
        {title}
      </h2>
      <div className="space-y-3 text-sm">{children}</div>
    </div>
  );
}

function DetailRow({ label, value }) {
  const displayValue =
    value === null || value === undefined || value === '' ? 'N/A' : value;

  return (
    <div className="flex items-start justify-between gap-4">
      <span className="text-slate-500">{label}</span>
      <span className="max-w-[65%] text-right font-semibold text-slate-900">
        {displayValue}
      </span>
    </div>
  );
}

function ProposalDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data, isLoading, isError, error } = useProposal(id);
  const [actionError, setActionError] = React.useState('');
  const approveMutation = useApproveProposal();
  const rejectMutation = useRejectProposal();
  const canDecideProposal = localStorage.getItem('userRole') === 'MANAGER';

  const { confirm: confirmApprove, modal: approveModal } = useConfirmation({
    title: 'Approve Proposal',
    message:
      'Are you sure you want to approve this proposal? The transaction will be executed.',
    confirmText: 'Approve',
    cancelText: 'Cancel',
    type: 'success',
  });

  const { confirm: confirmReject, modal: rejectModal } = useConfirmation({
    title: 'Reject Proposal',
    message: 'Are you sure you want to reject this proposal?',
    confirmText: 'Reject',
    cancelText: 'Cancel',
    type: 'danger',
  });

  if (isLoading) {
    return (
      <LoadingState
        title="Loading proposal..."
        message="Please wait while we fetch the proposal details."
      />
    );
  }

  if (isError || !data?.proposal) {
    return (
      <ErrorState
        title="Failed to load proposal"
        message={error?.message || 'An error occurred while loading proposal.'}
      />
    );
  }

  const proposal = data.proposal;
  const proposer = proposal.proposedByUser;
  const reviewer = proposal.reviewedByUser;
  const product = proposal.product;
  const supplier = proposal.supplier;
  const hasReviewDetails =
    proposal.reviewedAt ||
    proposal.reviewNote ||
    reviewer ||
    proposal.reviewedBy;
  const isPending = proposal.status === 'PENDING';
  const isMutating = approveMutation.isPending || rejectMutation.isPending;

  const handleApprove = async () => {
    const confirmed = await confirmApprove();
    if (confirmed) {
      try {
        setActionError('');
        await approveMutation.mutateAsync({ id: proposal.id });
      } catch (err) {
        setActionError(err.message || 'Failed to approve proposal.');
      }
    }
  };

  const handleReject = async () => {
    const confirmed = await confirmReject();
    if (confirmed) {
      try {
        setActionError('');
        await rejectMutation.mutateAsync({ id: proposal.id });
      } catch (err) {
        setActionError(err.message || 'Failed to reject proposal.');
      }
    }
  };

  return (
    <div>
      <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="mb-2 flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-display font-bold text-slate-900">
              Proposal Details
            </h1>
            {getStatusBadge(proposal.status)}
          </div>
          <p className="font-mono text-sm text-slate-500">
            #{proposal.id} - {formatType(proposal.transactionType)}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {canDecideProposal && isPending && (
            <>
              <button
                type="button"
                onClick={handleApprove}
                disabled={isMutating}
                className="inline-flex min-h-11 items-center justify-center rounded-lg bg-success px-4 py-2 text-sm font-semibold text-white shadow-sm transition-all hover:opacity-90 disabled:opacity-60"
              >
                {approveMutation.isPending ? (
                  <span className="flex items-center gap-2">
                    <LoadingSpinner size="sm" color="white" />
                    Approving
                  </span>
                ) : (
                  'Approve'
                )}
              </button>
              <button
                type="button"
                onClick={handleReject}
                disabled={isMutating}
                className="inline-flex min-h-11 items-center justify-center rounded-lg bg-error px-4 py-2 text-sm font-semibold text-white shadow-sm transition-all hover:opacity-90 disabled:opacity-60"
              >
                {rejectMutation.isPending ? (
                  <span className="flex items-center gap-2">
                    <LoadingSpinner size="sm" color="white" />
                    Rejecting
                  </span>
                ) : (
                  'Reject'
                )}
              </button>
            </>
          )}
          <button
            type="button"
            onClick={() => navigate('/proposals')}
            className="inline-flex min-h-11 items-center justify-center rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white shadow-sm transition-all hover:bg-primary-dark"
          >
            Back to Proposals
          </button>
        </div>
      </div>

      {actionError && (
        <div className="mb-6 rounded-xl border-2 border-error bg-error-light px-4 py-3 text-sm font-medium text-error">
          {actionError}
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <DetailCard title="Proposal Information">
          <DetailRow
            label="Type"
            value={formatType(proposal.transactionType)}
          />
          <DetailRow label="Status" value={proposal.status} />
          <DetailRow label="Quantity" value={proposal.quantity} />
          <DetailRow
            label="Total Price"
            value={formatCurrency(proposal.totalPrice)}
          />
          <DetailRow
            label="Created At"
            value={formatDate(proposal.createdAt)}
          />
          <DetailRow label="Description" value={proposal.description} />
        </DetailCard>

        <DetailCard title="Employee Information">
          <DetailRow
            label="Name"
            value={proposer?.name || `#${proposal.proposedBy ?? 'N/A'}`}
          />
          <DetailRow label="Email" value={proposer?.email} />
          <DetailRow label="Phone" value={proposer?.phoneNumber} />
          <DetailRow label="Role" value={proposer?.role} />
        </DetailCard>

        <DetailCard title="Product Information">
          <DetailRow label="Name" value={product?.name} />
          <DetailRow label="SKU" value={product?.sku} />
          <DetailRow
            label="Unit Price"
            value={formatCurrency(product?.price)}
          />
          <DetailRow label="Current Stock" value={product?.stockQuantity} />
        </DetailCard>

        {supplier && (
          <DetailCard title="Supplier Information">
            <DetailRow label="Name" value={supplier.name} />
            <DetailRow label="Contact" value={supplier.contactInfo} />
            <DetailRow label="Email" value={supplier.email} />
            <DetailRow label="Address" value={supplier.address} />
          </DetailCard>
        )}

        {hasReviewDetails && (
          <DetailCard title="Review Information">
            <DetailRow
              label="Reviewer"
              value={reviewer?.name || `#${proposal.reviewedBy ?? 'N/A'}`}
            />
            <DetailRow
              label="Reviewed At"
              value={formatDate(proposal.reviewedAt)}
            />
            <DetailRow label="Review Note" value={proposal.reviewNote} />
          </DetailCard>
        )}
      </div>
      {approveModal}
      {rejectModal}
    </div>
  );
}

export default ProposalDetailsPage;
