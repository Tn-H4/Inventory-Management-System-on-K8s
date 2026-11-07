import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { downloadBlob } from '../lib/download';
import {
  useProposals,
  useApproveProposal,
  useRejectProposal,
  useExportProposals,
} from '../features/proposals/api';
import { EmptyState, ErrorState, LoadingState } from '../components/EmptyState';
import { useConfirmation } from '../components/ConfirmationModal';
import { LoadingSpinner } from '../components/LoadingSpinner';
import PaginationComponent from '../components/PaginationComponent';

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

function getProposalRows(payload) {
  if (!payload) return [];
  if (Array.isArray(payload.proposals)) return payload.proposals;
  if (Array.isArray(payload)) return payload;
  return [];
}

function getEmployeeName(proposal) {
  return proposal?.proposedByUser?.name || `#${proposal?.proposedBy ?? 'N/A'}`;
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

function ProposalsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [exportError, setExportError] = useState('');

  const { data, isLoading, isError, error } = useProposals({
    page,
    limit: pageSize,
  });

  const approveMutation = useApproveProposal();
  const rejectMutation = useRejectProposal();
  const exportMutation = useExportProposals();

  const userRole = localStorage.getItem('userRole');
  const canDecideProposals = userRole === 'MANAGER';

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

  const proposals = useMemo(() => getProposalRows(data), [data]);
  const totalPages = data?.totalPages || 1;
  const isMutating = approveMutation.isPending || rejectMutation.isPending;

  const handleExport = () => {
    setExportError('');
    exportMutation.mutate(undefined, {
      onSuccess: (blob) => {
        downloadBlob(blob, 'proposals.csv');
      },
      onError: (err) => {
        setExportError(err.message || 'Failed to export CSV.');
      },
    });
  };

  const handlePageSizeChange = (nextPageSize) => {
    setPageSize(nextPageSize);
    setPage(1);
  };

  const handleApprove = async (id) => {
    const confirmed = await confirmApprove();
    if (confirmed) {
      await approveMutation.mutateAsync({ id });
    }
  };

  const handleReject = async (id) => {
    const confirmed = await confirmReject();
    if (confirmed) {
      await rejectMutation.mutateAsync({ id });
    }
  };

  if (isLoading) {
    return (
      <LoadingState
        title="Loading proposals..."
        message="Please wait while we fetch approval requests."
      />
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Failed to load proposals"
        message={error?.message || 'Failed to load proposals.'}
      />
    );
  }

  return (
    <div>
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-display font-bold text-slate-900">
            Proposals
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Review staff-submitted inventory actions before they affect stock.
          </p>
        </div>
        {canDecideProposals && (
          <button
            type="button"
            onClick={handleExport}
            disabled={exportMutation.isPending}
            className="flex items-center gap-1.5 rounded-lg border-2 border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 transition-all duration-200 hover:border-primary hover:text-primary disabled:opacity-60 sm:gap-2 sm:rounded-xl sm:px-4 sm:py-2.5 sm:text-sm"
          >
            {exportMutation.isPending ? 'Exporting...' : 'Export CSV'}
          </button>
        )}
      </div>
      {exportError && (
        <div className="mb-4 rounded-xl border-2 border-error bg-error-light px-4 py-3 text-sm font-medium text-error">
          {exportError}
        </div>
      )}

      {proposals.length === 0 ? (
        <EmptyState
          title="No proposals"
          description="There are no approval requests to review."
        />
      ) : (
        <>
          <div className="hidden overflow-x-auto rounded-xl border border-slate-200 md:block">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700"
                  >
                    ID
                  </th>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700"
                  >
                    Employee
                  </th>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700"
                  >
                    Type
                  </th>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700"
                  >
                    Status
                  </th>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700"
                  >
                    Quantity
                  </th>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700"
                  >
                    Total Price
                  </th>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700"
                  >
                    Created
                  </th>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700"
                  >
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {proposals.map((proposal) => (
                  <tr key={proposal.id} className="hover:bg-slate-50">
                    <td className="whitespace-nowrap px-6 py-4 text-sm font-mono text-slate-700">
                      #{proposal.id}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm font-semibold text-slate-900">
                      {getEmployeeName(proposal)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                      {formatType(proposal.transactionType)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      {getStatusBadge(proposal.status)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                      {proposal.quantity}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                      {formatCurrency(proposal.totalPrice)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                      {formatDate(proposal.createdAt)}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() => navigate(`/proposals/${proposal.id}`)}
                          className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 shadow-sm transition-all hover:bg-slate-50"
                        >
                          Details
                        </button>
                        {canDecideProposals &&
                          proposal.status === 'PENDING' && (
                            <>
                              <button
                                type="button"
                                onClick={() => handleApprove(proposal.id)}
                                disabled={isMutating}
                                className="rounded-lg bg-success px-3 py-2 text-xs font-semibold text-white shadow-sm transition-all hover:opacity-90 disabled:opacity-60"
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
                                onClick={() => handleReject(proposal.id)}
                                disabled={isMutating}
                                className="rounded-lg bg-error px-3 py-2 text-xs font-semibold text-white shadow-sm transition-all hover:opacity-90 disabled:opacity-60"
                              >
                                Reject
                              </button>
                            </>
                          )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="space-y-3 md:hidden">
            {proposals.map((proposal) => (
              <div
                key={proposal.id}
                className="rounded-xl border border-slate-200 bg-white p-4 shadow-soft"
              >
                <div className="mb-3 flex items-start justify-between gap-3">
                  <div>
                    <p className="font-mono text-sm text-slate-500">
                      #{proposal.id}
                    </p>
                    <p className="mt-1 text-base font-semibold text-slate-900">
                      {getEmployeeName(proposal)}
                    </p>
                  </div>
                  {getStatusBadge(proposal.status)}
                </div>
                <div className="space-y-1 text-sm text-slate-600">
                  <p>Type: {formatType(proposal.transactionType)}</p>
                  <p>Quantity: {proposal.quantity}</p>
                  <p>Total: {formatCurrency(proposal.totalPrice)}</p>
                  <p>Created: {formatDate(proposal.createdAt)}</p>
                </div>
                <div className="mt-4 flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => navigate(`/proposals/${proposal.id}`)}
                    className="min-h-11 flex-1 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 shadow-sm transition-all hover:bg-slate-50"
                  >
                    Details
                  </button>
                  {canDecideProposals && proposal.status === 'PENDING' && (
                    <>
                      <button
                        type="button"
                        onClick={() => handleApprove(proposal.id)}
                        disabled={isMutating}
                        className="min-h-11 flex-1 rounded-lg bg-success px-3 py-2 text-sm font-semibold text-white shadow-sm transition-all hover:opacity-90 disabled:opacity-60"
                      >
                        Approve
                      </button>
                      <button
                        type="button"
                        onClick={() => handleReject(proposal.id)}
                        disabled={isMutating}
                        className="min-h-11 flex-1 rounded-lg bg-error px-3 py-2 text-sm font-semibold text-white shadow-sm transition-all hover:opacity-90 disabled:opacity-60"
                      >
                        Reject
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="mt-6">
              <PaginationComponent
                currentPage={page}
                totalPages={totalPages}
                onPageChange={setPage}
                pageSize={pageSize}
                onPageSizeChange={handlePageSizeChange}
              />
            </div>
          )}
        </>
      )}

      {approveModal}
      {rejectModal}
    </div>
  );
}

export default ProposalsPage;
