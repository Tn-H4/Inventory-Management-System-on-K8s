import React, { useState, Suspense } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useCurrentUser } from '../features/users/api';
import NotificationBell from '../features/notifications/NotificationBell';
import { useNotificationStream } from '../features/notifications/useNotificationStream';
import { Breadcrumbs } from './Breadcrumbs';
import { LoadingSpinner } from './LoadingSpinner';

const logout = (navigate) => {
  localStorage.removeItem('token');
  localStorage.removeItem('userRole');
  navigate('/login');
};

const NAV_EXPANDED_STORAGE_KEY = 'inventory_nav_sidebar_expanded';

function loadExpandedGroupsFromStorage() {
  try {
    const raw = sessionStorage.getItem(NAV_EXPANDED_STORAGE_KEY);
    if (!raw) return {};
    const data = JSON.parse(raw);
    if (data === null || typeof data !== 'object' || Array.isArray(data)) {
      return {};
    }
    return data;
  } catch {
    return {};
  }
}

function persistExpandedGroups(next) {
  try {
    sessionStorage.setItem(NAV_EXPANDED_STORAGE_KEY, JSON.stringify(next));
  } catch {
    /* ignore quota / privacy mode */
  }
}

const navGroups = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    roles: ['ADMIN', 'MANAGER', 'STAFF'],
    iconPath:
      'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z',
    children: [
      {
        to: '/dashboard',
        label: 'Dashboard',
        roles: ['ADMIN', 'MANAGER', 'STAFF'],
      },
    ],
  },
  {
    id: 'inventory',
    label: 'Inventory',
    roles: ['ADMIN', 'MANAGER'],
    iconPath: 'M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4',
    children: [
      { to: '/products', label: 'Product', roles: ['ADMIN', 'MANAGER'] },
      { to: '/categories', label: 'Categories', roles: ['ADMIN', 'MANAGER'] },
      { to: '/warehouses', label: 'Warehouses', roles: ['ADMIN', 'MANAGER'] },
      { to: '/suppliers', label: 'Supplier', roles: ['ADMIN', 'MANAGER'] },
    ],
  },
  {
    id: 'orders',
    label: 'Orders',
    roles: ['ADMIN', 'MANAGER', 'STAFF'],
    iconPath:
      'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01',
    children: [
      { to: '/sell', label: 'Sell', roles: ['MANAGER', 'STAFF'] },
      {
        to: '/purchase',
        label: 'Purchase',
        roles: ['MANAGER', 'STAFF'],
      },
      { to: '/return', label: 'Return', roles: ['MANAGER'] },
      {
        to: '/transactions',
        label: 'Transaction',
        roles: ['ADMIN', 'MANAGER', 'STAFF'],
      },
    ],
  },
  {
    id: 'users',
    label: 'Users',
    roles: ['ADMIN'],
    iconPath:
      'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z',
    children: [
      {
        to: '/users',
        label: 'Users',
        roles: ['ADMIN'],
      },
    ],
  },
  {
    id: 'approvals',
    label: 'Approvals',
    roles: ['ADMIN', 'MANAGER'],
    iconPath:
      'M9 12l2 2 4-4m6 2a2 2 0 11-4 0 2 2 0 014 0zm-2 8a2 2 0 11-4 0 2 2 0 014 0z',
    children: [
      {
        to: '/proposals',
        label: 'Proposals',
        roles: ['ADMIN', 'MANAGER'],
      },
      {
        to: '/audit-logs',
        label: 'Audit Logs',
        roles: ['ADMIN'],
      },
    ],
  },
  {
    id: 'profile',
    label: 'Profile',
    roles: ['ADMIN', 'MANAGER', 'STAFF'],
    iconPath:
      'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z',
    children: [
      {
        to: '/profile',
        label: 'Profile',
        roles: ['ADMIN', 'MANAGER', 'STAFF'],
      },
      {
        to: '/notifications',
        label: 'Notifications',
        roles: ['ADMIN', 'MANAGER', 'STAFF'],
      },
    ],
  },
];

const navItemClassName = ({ isActive }) =>
  `flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-200 ${
    isActive
      ? 'bg-white/20 text-white shadow-soft'
      : 'text-white/80 hover:bg-white/10 hover:text-white'
  }`;

const groupButtonClassName =
  'w-full flex items-center justify-between gap-3 px-4 py-3 rounded-xl text-sm font-medium text-white/80 hover:bg-white/10 hover:text-white transition-colors';

function GroupIcon({ iconPath }) {
  return (
    <svg
      className="w-5 h-5 flex-shrink-0"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={1.5}
        d={iconPath}
      />
    </svg>
  );
}

function DropdownGroup({
  group,
  visibleChildren,
  expandedGroups,
  onToggleGroup,
  onClose,
}) {
  const isExpanded = Boolean(expandedGroups[group.id]);

  if (visibleChildren.length === 1) {
    const onlyChild = visibleChildren[0];
    return (
      <NavLink to={onlyChild.to} onClick={onClose} className={navItemClassName}>
        <GroupIcon iconPath={group.iconPath} />
        <span>{group.label}</span>
      </NavLink>
    );
  }

  return (
    <div className="space-y-1">
      <button
        type="button"
        className={groupButtonClassName}
        onClick={() => onToggleGroup(group.id)}
      >
        <span className="flex items-center gap-3">
          <GroupIcon iconPath={group.iconPath} />
          <span>{group.label}</span>
        </span>
        <svg
          className={`w-4 h-4 transition-transform duration-200 ${isExpanded ? 'rotate-180' : ''}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M19 9l-7 7-7-7"
          />
        </svg>
      </button>
      {isExpanded && (
        <div className="ml-4 pl-3 border-l border-white/20 space-y-1">
          {visibleChildren.map((child) => (
            <NavLink
              key={child.to}
              to={child.to}
              onClick={onClose}
              className={navItemClassName}
            >
              <span>{child.label}</span>
            </NavLink>
          ))}
        </div>
      )}
    </div>
  );
}

function getVisibleNavigation(currentRole) {
  return navGroups
    .filter((group) => group.roles.includes(currentRole))
    .map((group) => ({
      ...group,
      visibleChildren: group.children.filter((child) =>
        child.roles.includes(currentRole)
      ),
    }))
    .filter((group) => group.visibleChildren.length > 0);
}

function AppLayout() {
  const navigate = useNavigate();
  useNotificationStream();
  const { data: currentUserData, isLoading } = useCurrentUser();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [expandedGroups, setExpandedGroups] = useState(
    loadExpandedGroupsFromStorage
  );

  const handleLogout = () => {
    try {
      sessionStorage.removeItem(NAV_EXPANDED_STORAGE_KEY);
    } catch {
      /* noop */
    }
    logout(navigate);
    setIsMobileMenuOpen(false);
  };

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen(!isMobileMenuOpen);
  };

  const closeMobileMenu = () => {
    setIsMobileMenuOpen(false);
  };

  const toggleGroup = (groupId) => {
    setExpandedGroups((prevState) => {
      const next = { ...prevState, [groupId]: !prevState[groupId] };
      persistExpandedGroups(next);
      return next;
    });
  };

  const initials = (currentUserData?.name || 'User')
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  const currentRole = currentUserData?.role || 'STAFF';
  const visibleNavigation = getVisibleNavigation(currentRole);

  return (
    <div className="flex min-h-screen bg-slate-100">
      {isMobileMenuOpen && (
        <div
          className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-40 lg:hidden animate-fade-in"
          onClick={closeMobileMenu}
        />
      )}

      <aside className="hidden lg:flex lg:flex-col lg:w-64 lg:flex-shrink-0 bg-primary text-white">
        <SidebarContent
          currentUserData={currentUserData}
          isLoading={isLoading}
          initials={initials}
          currentRole={currentRole}
          expandedGroups={expandedGroups}
          onToggleGroup={toggleGroup}
          visibleNavigation={visibleNavigation}
          onLogout={handleLogout}
        />
      </aside>

      <div
        className={`
          fixed inset-y-0 left-0 z-50 w-72 bg-primary text-white transform transition-transform duration-300 ease-out lg:hidden
          ${isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full'}
        `}
      >
        <SidebarContent
          currentUserData={currentUserData}
          isLoading={isLoading}
          initials={initials}
          currentRole={currentRole}
          expandedGroups={expandedGroups}
          onToggleGroup={toggleGroup}
          visibleNavigation={visibleNavigation}
          onLogout={handleLogout}
          onClose={closeMobileMenu}
        />
      </div>

      <div className="w-full flex-1 flex flex-col min-h-screen">
        {/* Mobile header - shows only on screens < lg */}
        <header className="lg:hidden w-full flex items-center justify-between px-4 py-3 bg-white border-b border-slate-200 sticky top-0 z-30">
          <button
            onClick={toggleMobileMenu}
            className="p-2 rounded-lg hover:bg-slate-100 transition-colors"
            aria-label="Open menu"
          >
            <svg
              className="w-6 h-6 text-slate-700"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 6h16M4 12h16M4 18h16"
              />
            </svg>
          </button>
          <h1 className="text-lg font-display font-semibold text-slate-800">
            Inventory System
          </h1>
          <NotificationBell variant="light" />
        </header>

        <main className="flex-1 overflow-x-hidden overflow-y-auto">
          <div className="w-full min-h-full p-4 sm:p-6 lg:p-8">
            <div className="w-full max-w-7xl mx-auto">
              <Breadcrumbs />
              <div className="w-full bg-white rounded-2xl lg:rounded-3xl shadow-soft p-4 sm:p-8 animate-fade-in">
                <Suspense
                  fallback={
                    <div className="flex items-center justify-center p-8">
                      <LoadingSpinner />
                    </div>
                  }
                >
                  <Outlet />
                </Suspense>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

function SidebarContent({
  currentUserData,
  isLoading,
  initials,
  currentRole,
  expandedGroups,
  onToggleGroup,
  visibleNavigation,
  onLogout,
  onClose,
}) {
  return (
    <div className="flex flex-col h-full p-4 sm:p-6">
      <div className="flex flex-col items-center text-center mb-6">
        <div className="relative">
          <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-full border-4 border-white/20 bg-white/10 flex items-center justify-center text-2xl sm:text-3xl font-display font-semibold mb-3">
            {isLoading ? <LoadingSpinner size="sm" color="white" /> : initials}
          </div>
          <div className="absolute -top-1 -right-1">
            <NotificationBell />
          </div>
        </div>
        <h2 className="text-lg font-display font-semibold text-white">
          {isLoading ? 'Loading...' : currentUserData?.name}
        </h2>
        <span className="text-xs text-white/70 mt-1">{currentRole}</span>
      </div>

      <div className="h-px w-full bg-white/20 mb-4" />

      <nav className="flex-1 overflow-y-auto space-y-1">
        {visibleNavigation.map((group) => (
          <DropdownGroup
            key={group.id}
            group={group}
            visibleChildren={group.visibleChildren}
            expandedGroups={expandedGroups}
            onToggleGroup={onToggleGroup}
            onClose={onClose}
          />
        ))}
      </nav>

      <button
        type="button"
        onClick={onLogout}
        className="mt-2 flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium text-white/80 hover:bg-white/10 hover:text-white transition-colors"
      >
        <svg
          className="w-5 h-5 flex-shrink-0"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
          />
        </svg>
        <span>Logout</span>
      </button>
    </div>
  );
}

export default AppLayout;
