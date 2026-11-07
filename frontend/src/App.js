import React, { lazy } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import ProtectedRoute, {
  AdminRoute,
  ManagerOnlyRoute,
  ManagerRoute,
  StaffManagerRoute,
} from './components/ProtectedRoute';

// Lazy-loaded pages for code splitting
const LoginPage = lazy(() => import('./pages/LoginPage'));
const RegisterPage = lazy(() => import('./pages/RegisterPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const ProfilePage = lazy(() => import('./pages/ProfilePage'));
const CategoryCreatePage = lazy(() => import('./pages/CategoryCreatePage'));
const CategoryPage = lazy(() => import('./pages/CategoryPage'));
const CategoryEditPage = lazy(() => import('./pages/CategoryEditPage'));
const SupplierPage = lazy(() => import('./pages/SupplierPage'));
const SupplierCreatePage = lazy(() => import('./pages/SupplierCreatePage'));
const SupplierEditPage = lazy(() => import('./pages/SupplierEditPage'));
const ProductPage = lazy(() => import('./pages/ProductPage'));
const ProductCreatePage = lazy(() => import('./pages/ProductCreatePage'));
const ProductEditPage = lazy(() => import('./pages/ProductEditPage'));
const UserManagementPage = lazy(() => import('./pages/UserManagementPage'));
const UserCreatePage = lazy(() => import('./pages/UserCreatePage'));
const WarehousePage = lazy(() => import('./pages/WarehousePage'));
const WarehouseCreatePage = lazy(() => import('./pages/WarehouseCreatePage'));
const WarehouseEditPage = lazy(() => import('./pages/WarehouseEditPage'));
const TransactionsPage = lazy(() => import('./pages/TransactionsPage'));
const TransactionDetailsPage = lazy(
  () => import('./pages/TransactionDetailsPage')
);
const PurchasePage = lazy(() => import('./pages/PurchasePage'));
const SellPage = lazy(() => import('./pages/SellPage'));
const ReturnPage = lazy(() => import('./pages/ReturnPage'));
const NotificationsPage = lazy(() => import('./pages/NotificationsPage'));
const ProposalsPage = lazy(() => import('./pages/ProposalsPage'));
const ProposalDetailsPage = lazy(() => import('./pages/ProposalDetailsPage'));
const AuditLogsPage = lazy(() => import('./pages/AuditLogsPage'));
const AuditLogDetailsPage = lazy(() => import('./pages/AuditLogDetailsPage'));

function App() {
  return (
    <Routes>
      {/* PUBLIC ROUTES */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* PROTECTED ROUTES (for MANAGER and ADMIN) */}
      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/notifications" element={<NotificationsPage />} />

        <Route path="/transactions" element={<TransactionsPage />} />
        <Route path="/transactions/:id" element={<TransactionDetailsPage />} />
        <Route
          path="/purchase"
          element={
            <StaffManagerRoute>
              <PurchasePage />
            </StaffManagerRoute>
          }
        />
        <Route
          path="/sell"
          element={
            <StaffManagerRoute>
              <SellPage />
            </StaffManagerRoute>
          }
        />
        <Route
          path="/return"
          element={
            <ManagerOnlyRoute>
              <ReturnPage />
            </ManagerOnlyRoute>
          }
        />
      </Route>

      {/* MANAGER ROUTES*/}
      <Route
        element={
          <ManagerRoute>
            <AppLayout />
          </ManagerRoute>
        }
      >
        <Route path="/products" element={<ProductPage />} />
        <Route path="/products/new" element={<ProductCreatePage />} />
        <Route path="/products/:id/edit" element={<ProductEditPage />} />

        <Route path="/categories/add" element={<CategoryCreatePage />} />
        <Route path="/categories" element={<CategoryPage />} />
        <Route path="/categories/:id/edit" element={<CategoryEditPage />} />

        <Route path="/suppliers" element={<SupplierPage />} />
        <Route path="/suppliers/new" element={<SupplierCreatePage />} />
        <Route path="/suppliers/:id/edit" element={<SupplierEditPage />} />

        <Route path="/warehouses" element={<WarehousePage />} />
        <Route path="/warehouses/new" element={<WarehouseCreatePage />} />
        <Route path="/warehouses/:id/edit" element={<WarehouseEditPage />} />

        <Route path="/proposals" element={<ProposalsPage />} />
        <Route path="/proposals/:id" element={<ProposalDetailsPage />} />
      </Route>

      {/* ADMIN-ONLY ROUTES */}
      <Route
        element={
          <AdminRoute>
            <AppLayout />
          </AdminRoute>
        }
      >
        <Route path="/products" element={<ProductPage />} />
        <Route path="/products/new" element={<ProductCreatePage />} />
        <Route path="/products/:id/edit" element={<ProductEditPage />} />

        <Route path="/categories/add" element={<CategoryCreatePage />} />
        <Route path="/categories" element={<CategoryPage />} />
        <Route path="/categories/:id/edit" element={<CategoryEditPage />} />

        <Route path="/suppliers" element={<SupplierPage />} />
        <Route path="/suppliers/new" element={<SupplierCreatePage />} />
        <Route path="/suppliers/:id/edit" element={<SupplierEditPage />} />

        <Route path="/users" element={<UserManagementPage />} />
        <Route path="/users/create" element={<UserCreatePage />} />

        <Route path="/warehouses" element={<WarehousePage />} />
        <Route path="/warehouses/new" element={<WarehouseCreatePage />} />
        <Route path="/warehouses/:id/edit" element={<WarehouseEditPage />} />

        <Route path="/proposals" element={<ProposalsPage />} />
        <Route path="/proposals/:id" element={<ProposalDetailsPage />} />
        <Route path="/audit-logs" element={<AuditLogsPage />} />
        <Route path="/audit-logs/:id" element={<AuditLogDetailsPage />} />
      </Route>

      {/* Catch-all route */}
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;
