import React from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ErrorState, LoadingState } from '../components/EmptyState';
import { useAuditLog } from '../features/auditLogs/api';

function formatDate(value) {
  if (!value) return 'N/A';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return 'N/A';
  return d.toLocaleString('en-CA');
}

function formatLabel(value) {
  return value ? value.replaceAll('_', ' ') : 'N/A';
}

function getActionBadge(action) {
  const styles = {
    PROPOSAL_CREATED: 'bg-info-light text-info',
    PROPOSAL_APPROVED: 'bg-success-light text-success',
    PROPOSAL_REJECTED: 'bg-error-light text-error',
    TRANSACTION_EXECUTED: 'bg-success-light text-success',
    RETURN_EXECUTED: 'bg-warning-light text-warning',
    PURCHASE: 'bg-primary-subtle text-primary',
    SALE: 'bg-info-light text-info',
    RETURN_TO_SUPPLIER: 'bg-warning-light text-warning',
  };
  const style = styles[action] || 'bg-slate-100 text-slate-700';
  return (
    <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${style}`}>
      {formatLabel(action)}
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

function AuditLogDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data, isLoading, isError, error } = useAuditLog(id);

  if (isLoading) {
    return (
      <LoadingState
        title="Loading audit log..."
        message="Please wait while we fetch the audit log details."
      />
    );
  }

  if (isError || !data?.auditLog) {
    return (
      <ErrorState
        title="Failed to load audit log"
        message={error?.message || 'An error occurred while loading audit log.'}
      />
    );
  }

  const log = data.auditLog;
  const actor = log.user;

  return (
    <div>
      <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="mb-2 flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-display font-bold text-slate-900">
              Audit Log Details
            </h1>
            {getActionBadge(log.action)}
          </div>
          <p className="font-mono text-sm text-slate-500">#{log.id}</p>
        </div>
        <button
          type="button"
          onClick={() => navigate('/audit-logs')}
          className="inline-flex min-h-11 items-center justify-center rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white shadow-sm transition-all hover:bg-primary-dark"
        >
          Back to Audit Logs
        </button>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <DetailCard title="Action Information">
          <DetailRow label="Action" value={formatLabel(log.action)} />
          <DetailRow label="Entity Type" value={formatLabel(log.entityType)} />
          <DetailRow label="Entity ID" value={log.entityId} />
          <DetailRow label="Timestamp" value={formatDate(log.createdAt)} />
        </DetailCard>

        <DetailCard title="User Information">
          <DetailRow label="Name" value={actor?.name || `#${log.userId}`} />
          <DetailRow label="Email" value={actor?.email} />
          <DetailRow label="Phone" value={actor?.phoneNumber} />
          <DetailRow label="Role" value={actor?.role} />
        </DetailCard>

        <div className="lg:col-span-2">
          <DetailCard title="Details">
            <p className="whitespace-pre-wrap break-words text-sm font-medium text-slate-900">
              {log.details || 'N/A'}
            </p>
          </DetailCard>
        </div>
      </div>
    </div>
  );
}

export default AuditLogDetailsPage;
