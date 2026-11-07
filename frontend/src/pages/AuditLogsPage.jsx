import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { downloadBlob } from '../lib/download';
import { useAuditLogs, useExportAuditLogs } from '../features/auditLogs/api';
import { parseSortOption } from '../lib/sortUtils';
import { EmptyState, ErrorState, LoadingState } from '../components/EmptyState';
import PaginationComponent from '../components/PaginationComponent';

const ACTION_OPTIONS = [
  'PROPOSAL_CREATED',
  'PROPOSAL_APPROVED',
  'PROPOSAL_REJECTED',
  'TRANSACTION_EXECUTED',
  'RETURN_EXECUTED',
  'PURCHASE',
  'SALE',
  'RETURN_TO_SUPPLIER',
];

const ENTITY_TYPE_OPTIONS = [
  'PRODUCT',
  'SUPPLIER',
  'TRANSACTION',
  'USER',
  'CATEGORY',
  'WAREHOUSE',
  'NOTIFICATION',
  'PROPOSAL',
];

function formatDate(value) {
  if (!value) return 'N/A';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return 'N/A';
  return d.toLocaleString('en-CA');
}

function formatLabel(value) {
  return value ? value.replaceAll('_', ' ') : 'N/A';
}

function getAuditLogRows(payload) {
  if (!payload) return [];
  if (Array.isArray(payload.auditLogs)) return payload.auditLogs;
  if (Array.isArray(payload)) return payload;
  return [];
}

function getActorName(log) {
  return log?.user?.name || `#${log?.userId ?? 'N/A'}`;
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

function AuditLogsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [exportError, setExportError] = useState('');

  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [actionFilter, setActionFilter] = useState('ALL');
  const [entityTypeFilter, setEntityTypeFilter] = useState('ALL');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [sortOption, setSortOption] = useState('created_at:DESC');
  const { sortBy, sortDir } = parseSortOption(sortOption);

  const { data, isLoading, isError, error, isFetching } = useAuditLogs({
    page,
    limit: pageSize,
    search,
    action: actionFilter,
    entityType: entityTypeFilter,
    startDate,
    endDate,
    sortBy,
    sortDir,
  });

  const exportMutation = useExportAuditLogs();
  const userRole = localStorage.getItem('userRole');
  const isAdmin = userRole === 'ADMIN';

  const handleExport = () => {
    setExportError('');
    exportMutation.mutate(undefined, {
      onSuccess: (blob) => {
        downloadBlob(blob, 'audit-logs.csv');
      },
      onError: (err) => setExportError(err.message || 'Failed to export CSV.'),
    });
  };

  const handleSort = (col) => {
    setSortOption((prev) => {
      const { sortBy: prevCol, sortDir: prevDir } = parseSortOption(prev);
      const dir =
        prevCol === col ? (prevDir === 'ASC' ? 'DESC' : 'ASC') : 'ASC';
      return `${col}:${dir}`;
    });
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setSearch(searchInput.trim());
    setPage(1);
  };

  const auditLogs = useMemo(() => getAuditLogRows(data), [data]);
  const totalPages = data?.totalPages || 1;

  const handlePageSizeChange = (nextPageSize) => {
    setPageSize(nextPageSize);
    setPage(1);
  };

  if (isLoading) {
    return (
      <LoadingState
        title="Loading audit logs..."
        message="Please wait while we fetch activity history."
      />
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Failed to load audit logs"
        message={error?.message || 'Failed to load audit logs.'}
      />
    );
  }

  const SORTABLE_COLS = [
    { label: 'ID', col: 'id' },
    { label: 'Action', col: 'action' },
    { label: 'Entity', col: 'entity_type' },
    { label: 'Timestamp', col: 'created_at' },
  ];

  return (
    <div>
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-display font-bold text-slate-900">
            Audit Logs
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Review administrator-visible system activity by user, action, and
            affected record.
          </p>
        </div>
        {isAdmin && (
          <button
            type="button"
            onClick={handleExport}
            disabled={exportMutation.isPending}
            className="flex items-center gap-1.5 sm:gap-2 text-slate-700 bg-white border-2 border-slate-200 hover:border-primary hover:text-primary px-3 py-2 sm:px-4 sm:py-2.5 rounded-lg sm:rounded-xl text-xs sm:text-sm font-semibold transition-all duration-200 disabled:opacity-60"
          >
            {exportMutation.isPending ? 'Exporting…' : 'Export CSV'}
          </button>
        )}
      </div>

      {exportError && (
        <div className="mb-4 rounded-xl border-2 border-error bg-error-light px-4 py-3 text-sm text-error font-medium">
          {exportError}
        </div>
      )}

      {/* Filter bar */}
      <div className="mb-6 space-y-3">
        {/* Row 1: search + dropdowns */}
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <form
            onSubmit={handleSearchSubmit}
            className="relative w-full max-w-md"
          >
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
              <svg
                className="h-5 w-5 text-slate-400"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
            </div>
            <input
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search details..."
              aria-label="Search audit log details"
              className="w-full rounded-xl border-transparent bg-slate-100 py-3 pl-10 pr-4 text-sm text-slate-900 placeholder-slate-400 focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/20"
            />
          </form>
          <div className="flex flex-wrap items-center gap-2">
            <select
              value={actionFilter}
              onChange={(e) => {
                setActionFilter(e.target.value);
                setPage(1);
              }}
              className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-primary focus:ring-2 focus:ring-primary/20"
            >
              <option value="ALL">All Actions</option>
              {ACTION_OPTIONS.map((a) => (
                <option key={a} value={a}>
                  {formatLabel(a)}
                </option>
              ))}
            </select>
            <select
              value={entityTypeFilter}
              onChange={(e) => {
                setEntityTypeFilter(e.target.value);
                setPage(1);
              }}
              className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-primary focus:ring-2 focus:ring-primary/20"
            >
              <option value="ALL">All Entity Types</option>
              {ENTITY_TYPE_OPTIONS.map((et) => (
                <option key={et} value={et}>
                  {formatLabel(et)}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Row 2: date range */}
        <div className="flex flex-wrap items-center gap-x-3 gap-y-2 text-xs sm:text-sm text-slate-600">
          <span className="font-medium">From:</span>
          <input
            type="date"
            value={startDate}
            max={endDate || undefined}
            onChange={(e) => {
              setStartDate(e.target.value);
              setPage(1);
            }}
            className="min-w-0 border border-slate-200 rounded-md px-2 py-1 focus:border-primary focus:outline-none"
          />
          <span className="text-slate-400">to</span>
          <input
            type="date"
            value={endDate}
            min={startDate || undefined}
            onChange={(e) => {
              setEndDate(e.target.value);
              setPage(1);
            }}
            className="min-w-0 border border-slate-200 rounded-md px-2 py-1 focus:border-primary focus:outline-none"
          />
        </div>
      </div>

      {auditLogs.length === 0 ? (
        <EmptyState
          title="No audit logs"
          description="There is no recorded system activity matching your filters."
        />
      ) : (
        <>
          <div className="hidden overflow-x-auto rounded-xl border border-slate-200 md:block">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  {SORTABLE_COLS.map(({ label, col }) => (
                    <th
                      key={col}
                      scope="col"
                      onClick={() => handleSort(col)}
                      className="px-6 py-3 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700 cursor-pointer hover:bg-slate-100 transition-colors"
                    >
                      <div className="flex items-center gap-1">
                        {label}
                        {sortBy === col && (
                          <span>{sortDir === 'ASC' ? '↑' : '↓'}</span>
                        )}
                      </div>
                    </th>
                  ))}
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700"
                  >
                    User
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
                {auditLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50">
                    <td className="whitespace-nowrap px-6 py-4 text-sm font-mono text-slate-700">
                      #{log.id}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      {getActionBadge(log.action)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                      {formatLabel(log.entityType)} #{log.entityId}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                      {formatDate(log.createdAt)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm font-semibold text-slate-900">
                      {getActorName(log)}
                    </td>
                    <td className="px-6 py-4">
                      <button
                        type="button"
                        onClick={() => navigate(`/audit-logs/${log.id}`)}
                        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 shadow-sm transition-all hover:bg-slate-50"
                      >
                        Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="space-y-3 md:hidden">
            {auditLogs.map((log) => (
              <div
                key={log.id}
                className="rounded-xl border border-slate-200 bg-white p-4 shadow-soft"
              >
                <div className="mb-3 flex items-start justify-between gap-3">
                  <div>
                    <p className="font-mono text-sm text-slate-500">
                      #{log.id}
                    </p>
                    <p className="mt-1 text-base font-semibold text-slate-900">
                      {getActorName(log)}
                    </p>
                  </div>
                  {getActionBadge(log.action)}
                </div>
                <div className="space-y-1 text-sm text-slate-600">
                  <p>
                    Entity: {formatLabel(log.entityType)} #{log.entityId}
                  </p>
                  <p>Timestamp: {formatDate(log.createdAt)}</p>
                </div>
                <button
                  type="button"
                  onClick={() => navigate(`/audit-logs/${log.id}`)}
                  className="mt-4 min-h-11 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 shadow-sm transition-all hover:bg-slate-50"
                >
                  Details
                </button>
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="mt-6">
              <footer className="flex flex-col sm:flex-row items-center justify-between gap-3 sm:gap-4 pt-3 sm:pt-4 border-t border-slate-100">
                <span className="text-xs sm:text-sm text-slate-500 whitespace-nowrap">
                  Page {page} of {totalPages}
                  {isFetching && !isLoading && (
                    <span className="ml-1.5 sm:ml-2 text-primary animate-pulse">
                      (Updating...)
                    </span>
                  )}
                </span>
                <PaginationComponent
                  currentPage={page}
                  totalPages={totalPages}
                  onPageChange={setPage}
                  pageSize={pageSize}
                  onPageSizeChange={handlePageSizeChange}
                />
              </footer>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default AuditLogsPage;
