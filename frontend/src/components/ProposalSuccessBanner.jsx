import React from 'react';
import Button from './Button';

function ProposalSuccessBanner({
  requestType,
  onViewTransactions,
  onSubmitAnother,
}) {
  const requestLabel = requestType === 'sale' ? 'sale' : 'purchase';

  return (
    <div className="flex flex-col items-center justify-center py-16 px-4">
      <div className="w-full max-w-md rounded-2xl border border-blue-100 bg-blue-50 p-8 text-center shadow-sm">
        <div className="mb-4 flex items-center justify-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-blue-100">
            <svg
              className="h-8 w-8 text-primary"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M5 13l4 4L19 7"
              />
            </svg>
          </div>
        </div>
        <h2 className="mb-2 text-xl font-bold text-slate-800">
          Proposal Submitted
        </h2>
        <p className="mb-6 text-sm text-slate-600">
          Your {requestLabel} request has been submitted for manager approval. A{' '}
          <span className="font-semibold text-primary">PENDING</span>{' '}
          transaction has been recorded in your transaction history.
        </p>
        <div className="flex flex-col gap-3 sm:flex-row sm:justify-center">
          <Button variant="primary" onClick={onViewTransactions}>
            View Transactions
          </Button>
          <Button variant="outline" onClick={onSubmitAnother}>
            Submit Another
          </Button>
        </div>
      </div>
    </div>
  );
}

export default ProposalSuccessBanner;
