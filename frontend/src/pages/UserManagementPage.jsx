import React, { useMemo, useState } from 'react';
import {
  useCurrentUser,
  useActivateUser,
  useDeleteUser,
  useExportUsers,
  useUpdateUser,
  useUsers,
} from '../features/users/api';
import { parseSortOption } from '../lib/sortUtils';
import PaginationComponent from '../components/PaginationComponent';
import { EmptyState, ErrorState, LoadingState } from '../components/EmptyState';
import { useConfirmation } from '../components/ConfirmationModal';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { useNavigate } from 'react-router-dom';
import { downloadBlob } from '../lib/download';

const ROLE_OPTIONS = ['ADMIN', 'MANAGER', 'STAFF'];

// Check if role dropdown should be disabled for this user
// Admins cannot change their own role or other admins' roles
function isRoleChangeDisabled(currentUser, targetUser) {
  if (!currentUser || !targetUser) return false;
  const isCurrentUserAdmin = currentUser.role === 'ADMIN';
  const isTargetUserAdmin = targetUser.role === 'ADMIN';
  return isCurrentUserAdmin && isTargetUserAdmin;
}

function formatDate(value) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('en-CA');
}

function getUserRows(payload) {
  if (!payload) return [];
  if (Array.isArray(payload.users)) return payload.users;
  if (Array.isArray(payload)) return payload;
  return [];
}

const UserManagementPage = () => {
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [sortOption, setSortOption] = useState('id:ASC');
  const [exportError, setExportError] = useState('');
  const { sortBy, sortDir } = parseSortOption(sortOption);

  const { data: currentUserData } = useCurrentUser();
  const { data, isLoading, isError, error, isFetching } = useUsers({
    page,
    limit,
    search,
    role: roleFilter,
    status: statusFilter,
    startDate,
    endDate,
    sortBy,
    sortDir,
  });

  const updateMutation = useUpdateUser();
  const deleteMutation = useDeleteUser();
  const activateMutation = useActivateUser();
  const exportMutation = useExportUsers();

  const userRole = localStorage.getItem('userRole');
  const isAdmin = userRole === 'ADMIN';

  const handleExport = () => {
    setExportError('');
    exportMutation.mutate(undefined, {
      onSuccess: (blob) => {
        downloadBlob(blob, 'users.csv');
      },
      onError: (err) => setExportError(err.message || 'Failed to export CSV.'),
    });
  };

  const navigate = useNavigate();

  // Confirmation modal
  const { confirm, modal: deleteModal } = useConfirmation({
    title: 'Deactivate User',
    message:
      'Are you sure you want to deactivate this user? They will no longer be able to sign in.',
    confirmText: 'Deactivate',
    cancelText: 'Cancel',
    type: 'danger',
  });

  const handleSort = (column) => {
    setSortOption((prev) => {
      const { sortBy: prevCol, sortDir: prevDir } = parseSortOption(prev);
      const dir =
        prevCol === column ? (prevDir === 'ASC' ? 'DESC' : 'ASC') : 'ASC';
      return `${column}:${dir}`;
    });
  };

  const totalPages = data?.totalPages ?? 1;

  const users = useMemo(() => {
    return getUserRows(data);
  }, [data]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setSearch(searchInput.trim());
  };

  const handleRoleChange = (user, nextRole) => {
    if (!user?.id) return;
    updateMutation.mutate({
      id: user.id,
      data: {
        ...user,
        role: nextRole,
      },
    });
  };

  const handleUserAction = async (user) => {
    if (!user?.id) return;

    if (user.isActive === false) {
      activateMutation.mutate(user.id);
    } else {
      const confirmed = await confirm();
      if (confirmed) {
        deleteMutation.mutate(user.id);
      }
    }
  };

  const currentUserId = currentUserData?.id;

  return (
    <div className="p-3 sm:p-6 bg-white rounded-2xl shadow-soft space-y-4 sm:space-y-6">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-b border-slate-100 pb-4 sm:pb-6">
        <div>
          <h1 className="text-xl sm:text-2xl font-display font-bold text-slate-900">
            Users
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5 sm:mt-1">
            Manage accounts and roles
          </p>
        </div>

        <div className="flex items-center gap-2">
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
          <button
            onClick={() => navigate('/users/create')}
            className="flex items-center gap-1.5 sm:gap-2 bg-primary hover:bg-primary-dark text-white px-4 py-2 sm:px-5 sm:py-2.5 rounded-lg sm:rounded-xl text-xs sm:text-sm font-semibold shadow-soft hover:shadow-card transition-all duration-200"
          >
            <span className="text-base sm:text-lg font-bold">+</span>
            Add
          </button>
        </div>
      </header>
      {exportError && (
        <div className="rounded-lg sm:rounded-xl border-2 border-error bg-error-light px-3 py-2 sm:px-4 sm:py-3 text-xs sm:text-sm text-error font-medium">
          {exportError}
        </div>
      )}

      <div className="mb-6 space-y-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
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
              placeholder="Search by name or email..."
              aria-label="Search users"
              className="w-full rounded-xl border-transparent bg-slate-100 py-3 pl-10 pr-4 text-sm text-slate-900 placeholder-slate-400 focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/20"
            />
          </form>
          <div className="flex flex-col sm:flex-row items-center gap-3">
            <div className="flex items-center gap-2">
              <select
                value={roleFilter}
                onChange={(e) => {
                  setRoleFilter(e.target.value);
                  setPage(1);
                }}
                className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-primary focus:ring-2 focus:ring-primary/20"
              >
                <option value="ALL">All Roles</option>
                {ROLE_OPTIONS.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
              <select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(1);
                }}
                className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-primary focus:ring-2 focus:ring-primary/20"
              >
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-x-3 gap-y-2 text-xs sm:text-sm text-slate-600">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-medium">Joined from:</span>
            <input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                setPage(1);
              }}
              className="min-w-0 px-2 py-1 border border-slate-200 rounded-md focus:border-primary focus:outline-none"
            />
            <span className="text-slate-400">to</span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                setPage(1);
              }}
              className="min-w-0 px-2 py-1 border border-slate-200 rounded-md focus:border-primary focus:outline-none"
            />
          </div>
        </div>
      </div>

      {isLoading ? (
        <LoadingState
          title="Loading users..."
          message="Please wait while we fetch your data."
        />
      ) : isError ? (
        <ErrorState
          title="Failed to load users"
          message={error?.message || 'An error occurred while fetching users.'}
        />
      ) : users.length === 0 ? (
        <EmptyState
          title="No users found"
          description={
            search
              ? 'Try adjusting your search query.'
              : 'There are no users yet.'
          }
        />
      ) : (
        <>
          {/* Desktop Table */}
          <div className="hidden md:block overflow-x-auto rounded-xl border border-slate-200">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  {[
                    { label: 'Name', col: 'name' },
                    { label: 'Email', col: 'email' },
                    { label: 'Role', col: 'role' },
                    { label: 'Status', col: 'is_active' },
                    { label: 'Phone', col: 'phone_number' },
                    { label: 'Joined', col: 'created_at' },
                  ].map(({ label, col }) => (
                    <th
                      key={col}
                      onClick={() => handleSort(col)}
                      className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wider text-slate-900 cursor-pointer hover:bg-slate-100 transition-colors"
                    >
                      <div className="flex items-center gap-1">
                        {label}
                        {sortBy === col && (
                          <span>{sortDir === 'ASC' ? '↑' : '↓'}</span>
                        )}
                      </div>
                    </th>
                  ))}
                  <th className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wider text-slate-900">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {users.map((user) => {
                  const isSelf = Boolean(
                    currentUserId && user.id === currentUserId
                  );
                  return (
                    <tr key={user.id} className="hover:bg-slate-50">
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-900">
                        {user.name ?? '—'}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                        {user.email ?? '—'}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm">
                        <select
                          value={user.role ?? 'MANAGER'}
                          onChange={(e) =>
                            handleRoleChange(user, e.target.value)
                          }
                          disabled={
                            updateMutation.isPending ||
                            isRoleChangeDisabled(currentUserData, user)
                          }
                          aria-label={`Role for ${user.name ?? 'user'}`}
                          className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-primary focus:ring-2 focus:ring-primary/20 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          {ROLE_OPTIONS.map((r) => (
                            <option key={r} value={r}>
                              {r}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                            user.isActive === false
                              ? 'bg-slate-100 text-slate-700'
                              : 'bg-success-light text-success'
                          }`}
                        >
                          {user.isActive === false ? 'Deactivated' : 'Active'}
                        </span>
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                        {user.phoneNumber ?? '—'}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                        {formatDate(user.createdAt)}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm">
                        <button
                          type="button"
                          onClick={() => handleUserAction(user)}
                          disabled={
                            (user.isActive === false
                              ? activateMutation.isPending
                              : deleteMutation.isPending) || isSelf
                          }
                          className={`rounded-lg px-3 py-2 min-h-[44px] text-sm font-medium transition-all ${
                            isSelf
                              ? 'cursor-not-allowed bg-slate-100 text-slate-400'
                              : user.isActive === false
                                ? 'bg-success text-white hover:bg-success-dark'
                                : 'bg-error text-white hover:bg-red-700'
                          }`}
                          aria-label={
                            isSelf
                              ? 'Cannot manage your own account'
                              : user.isActive === false
                                ? `Activate ${user.name ?? 'user'}`
                                : `Deactivate ${user.name ?? 'user'}`
                          }
                          title={
                            isSelf
                              ? "You can't manage your own account"
                              : user.isActive === false
                                ? 'Activate user'
                                : 'Deactivate user'
                          }
                        >
                          {(
                            user.isActive === false
                              ? activateMutation.isPending
                              : deleteMutation.isPending
                          ) ? (
                            <LoadingSpinner size="sm" color="white" />
                          ) : user.isActive === false ? (
                            'Activate'
                          ) : (
                            'Deactivate'
                          )}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Mobile Cards */}
          <div className="block md:hidden space-y-4">
            {users.map((user) => {
              const isSelf = Boolean(
                currentUserId && user.id === currentUserId
              );
              return (
                <div
                  key={user.id}
                  className="rounded-xl border border-slate-200 bg-white p-4 shadow-soft"
                >
                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span className="text-xs text-slate-500">Name</span>
                      <span className="text-sm font-medium text-slate-900">
                        {user.name ?? '—'}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-xs text-slate-500">Email</span>
                      <span className="text-sm text-slate-700">
                        {user.email ?? '—'}
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-xs text-slate-500">Role</span>
                      <select
                        value={user.role ?? 'MANAGER'}
                        onChange={(e) => handleRoleChange(user, e.target.value)}
                        disabled={
                          updateMutation.isPending ||
                          isRoleChangeDisabled(currentUserData, user)
                        }
                        aria-label={`Role for ${user.name ?? 'user'}`}
                        className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-900 shadow-sm focus:border-primary focus:ring-2 focus:ring-primary/20 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {ROLE_OPTIONS.map((r) => (
                          <option key={r} value={r}>
                            {r}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-xs text-slate-500">Status</span>
                      <span
                        className={`inline-flex rounded-full px-2 py-0.5 text-xs font-semibold ${
                          user.isActive === false
                            ? 'bg-slate-100 text-slate-700'
                            : 'bg-success-light text-success'
                        }`}
                      >
                        {user.isActive === false ? 'Deactivated' : 'Active'}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-xs text-slate-500">Phone</span>
                      <span className="text-sm text-slate-700">
                        {user.phoneNumber ?? '—'}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-xs text-slate-500">Joined</span>
                      <span className="text-sm text-slate-700">
                        {formatDate(user.createdAt)}
                      </span>
                    </div>
                  </div>
                  <div className="mt-4 pt-3 border-t border-slate-100">
                    <button
                      type="button"
                      onClick={() => handleUserAction(user)}
                      disabled={
                        (user.isActive === false
                          ? activateMutation.isPending
                          : deleteMutation.isPending) || isSelf
                      }
                      className={`w-full py-2 text-sm font-medium transition-all rounded-lg ${
                        isSelf
                          ? 'cursor-not-allowed bg-slate-100 text-slate-400'
                          : user.isActive === false
                            ? 'bg-success text-white hover:bg-success-dark'
                            : 'bg-error text-white hover:bg-red-700'
                      }`}
                      aria-label={
                        isSelf
                          ? 'Cannot manage your own account'
                          : user.isActive === false
                            ? `Activate ${user.name ?? 'user'}`
                            : `Deactivate ${user.name ?? 'user'}`
                      }
                    >
                      {(
                        user.isActive === false
                          ? activateMutation.isPending
                          : deleteMutation.isPending
                      ) ? (
                        <LoadingSpinner size="sm" color="white" />
                      ) : isSelf ? (
                        "Can't manage own account"
                      ) : user.isActive === false ? (
                        'Activate'
                      ) : (
                        'Deactivate'
                      )}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </>
      )}

      {/* Footer Pagination */}
      {totalPages > 1 && (
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
            pageSize={limit}
            onPageSizeChange={(newLimit) => {
              setLimit(newLimit);
              setPage(1);
            }}
          />
        </footer>
      )}

      {/* Delete Confirmation Modal */}
      {deleteModal}
    </div>
  );
};

export default UserManagementPage;
