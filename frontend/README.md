# Frontend — Inventory Management System

React 19 single-page application for the Inventory Management System. Communicates with the Spring Boot backend via a JWT-authenticated REST API.

---

## Prerequisites

| Tool    | Version                   |
| ------- | ------------------------- |
| Node.js | 18+                       |
| npm     | 9+ (bundled with Node.js) |

---

## Setup

Install dependencies and configure the environment:

```bash
npm install
cp .env.example .env
```

Edit `.env` and set the backend API base URL:

```properties
# Local development
REACT_APP_API_BASE_URL=http://localhost:5050/api

# Production (Railway)
# REACT_APP_API_BASE_URL=https://inventory-management-production-6424.up.railway.app/api
```

---

## Running Locally

```bash
npm start
```

Opens at `http://localhost:3000`. The app proxy is **not** configured — the `REACT_APP_API_BASE_URL` environment variable is used for all API calls.

---

## Running Tests

```bash
npm test
```

Launches the test runner in interactive watch mode.

> **Note:** The only test file currently present is `src/App.test.js`, which is the default Create React App placeholder. It is not yet updated for the actual application and will fail unless modified. Frontend testing uses React Testing Library (`@testing-library/react`) and Jest. New component tests should be placed alongside their component in a `.test.jsx` file.

Run tests once without watch mode (e.g., for CI):

```bash
CI=true npm test
```

---

## Linting and Formatting

```bash
npm run lint      # ESLint — check for code quality issues
npm run format    # Prettier — auto-format all source files
```

**Always run `npm run format` before committing.** The project enforces consistent formatting via Prettier.

---

## Production Build

```bash
npm run build
```

Outputs a production-optimised static bundle to `build/`. Deploy this folder to any static hosting provider (Vercel, Netlify, etc.).

For Vercel, the build is triggered automatically from the repository. Set the environment variable `REACT_APP_API_BASE_URL` in the Vercel project settings.

---

## Project Structure

```
src/
├── App.js                    Route definitions (React Router v7)
├── index.js                  Entry point
├── components/               Shared UI components
│   ├── AppLayout.jsx         Sidebar shell wrapping all authenticated pages
│   ├── ProtectedRoute.jsx    Route guards (ProtectedRoute, AdminRoute, ManagerRoute, etc.)
│   ├── Breadcrumbs.jsx       Breadcrumb navigation bar
│   ├── ConfirmationModal.jsx Reusable confirmation dialog for destructive actions
│   ├── LoadingSpinner.jsx    Spinner and CardSkeleton loading states
│   ├── PaginationComponent.jsx  Pagination with configurable page size
│   ├── SearchableSelect.jsx  Keyboard-navigable combobox (used on transaction forms)
│   └── StockBadge.jsx        Out of Stock / Low Stock indicator badge
├── features/                 Domain-specific logic
│   ├── <domain>/api.js       React Query hooks (useXxx, useCreateXxx, etc.)
│   ├── <domain>/schema.js    Zod validation schemas
│   └── <domain>/<Form>.jsx   Form components (React Hook Form + Zod)
├── pages/                    Route-level page components
│   ├── LoginPage.jsx
│   ├── DashboardPage.jsx
│   ├── ProductPage.jsx / ProductCreatePage.jsx / ProductEditPage.jsx
│   ├── CategoryPage.jsx / CategoryCreatePage.jsx / CategoryEditPage.jsx
│   ├── SupplierPage.jsx / SupplierCreatePage.jsx / SupplierEditPage.jsx
│   ├── WarehousePage.jsx / WarehouseCreatePage.jsx / WarehouseEditPage.jsx
│   ├── TransactionsPage.jsx / TransactionDetailsPage.jsx
│   ├── PurchasePage.jsx / SellPage.jsx / ReturnPage.jsx
│   ├── ProposalsPage.jsx / ProposalDetailsPage.jsx
│   ├── NotificationsPage.jsx
│   ├── UserManagementPage.jsx / UserCreatePage.jsx
│   ├── AuditLogsPage.jsx / AuditLogDetailsPage.jsx
│   └── ProfilePage.jsx
└── lib/
    ├── axios.js              Axios instance with JWT interceptor and base URL
    └── download.js           Shared downloadBlob() helper for CSV export
```

---

## Key Conventions

### Authentication

- Login stores `token` and `userRole` in `localStorage`.
- `src/lib/axios.js` automatically attaches `Authorization: Bearer <token>` to every request.
- Route guards (`ProtectedRoute`, `AdminRoute`, `ManagerRoute`, etc.) read `localStorage` and redirect to `/login` if the token is absent or the role is insufficient.

### API Calls

All API communication goes through React Query hooks in `features/<domain>/api.js`. Never call `axios` directly from a page component — always use the provided hooks.

### Forms

- All forms use **React Hook Form** with a **Zod** resolver.
- Schemas live in `features/<domain>/schema.js`.
- Product forms use `FormData` (multipart) — never `JSON.stringify`.

### After stock-changing mutations

After any purchase, sell, or return transaction completes, invalidate these query keys to keep all views in sync:

```js
queryClient.invalidateQueries({ queryKey: ['products'] });
queryClient.invalidateQueries({ queryKey: ['transactions'] });
queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] });
queryClient.invalidateQueries({ queryKey: ['transactions-by-month'] });
```

---

## Environment Variables

| Variable                 | Required | Description                              |
| ------------------------ | -------- | ---------------------------------------- |
| `REACT_APP_API_BASE_URL` | Yes      | Backend API base URL (no trailing slash) |

See `.env.example` for the template.
