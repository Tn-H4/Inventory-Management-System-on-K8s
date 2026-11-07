# Contributing Guide — Inventory Management System

This document describes the conventions used in this codebase. All contributors should follow these standards.

---

## Branching Strategy

| Branch type | Naming pattern | Example |
|---|---|---|
| Feature | `feature/<short-description>` | `feature/user-auth` |
| Bug fix | `bugfix/<short-description>` | `bugfix/login-redirect` |
| Documentation | `documentation` | — |
| Configuration | `configuration` | — |

- `master` is the protected main branch — always deployable.
- Open a Pull Request to merge into `master`. Require at least one review if team size > 2.

---

## Commit Message Format

Follow the Conventional Commits specification:

```
type(scope): short description

Optional longer body explaining WHY, not WHAT.
```

**Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

**Examples:**
```
feat(auth): add JWT refresh token endpoint
fix(products): throw RuntimeException instead of swallowing SQLException
docs(readme): add live demo URL and test credentials
refactor(transactions): extract CSV export into CsvExportUtils
```

- Subject line: imperative mood, no period at end, max 72 chars.
- Each commit should represent one logical change.

---

## Backend Conventions (Java / Spring Boot)

### Package Structure

```
org.pdm.backend/
  controller/     REST controllers (thin — delegate to service)
  service/        Business logic interfaces
  service/Impl/   Service implementations
  repository/     Repository interfaces
  repository/Impl/  JDBC implementations
  model/          Plain Java model classes (no @Entity)
  wrappers/       Request/response DTOs and the Response envelope
  enums/          Java enums (UserRole, TransactionType, etc.)
  security/       Spring Security config, JWT filter, CORS
  exception/      Custom exceptions and GlobalExceptionHandler
  config/         Swagger, upload resource serving
  util/           Shared utilities (CsvExportUtils)
```

### Naming

- Classes: `PascalCase` (`ProductServiceImpl`, `CategoryController`)
- Methods/variables: `camelCase` (`findAllPaged`, `stockQuantity`)
- Constants: `UPPER_SNAKE_CASE` (`DEFAULT_PAGE_SIZE`)
- SQL table/column names: `snake_case` (`stock_quantity`, `created_at`)

### Repository Rules

- New repositories must use `JdbcTemplate` (not raw `Connection`/`PreparedStatement`).
- Never use string concatenation to build SQL — always use `?` placeholders.
- Throw `RuntimeException` (or a custom exception) on `SQLException`; never swallow errors silently.

### Service Rules

- Controllers call services; services call repositories. Never skip a layer.
- Business rule validation belongs in the service, not the controller.
- Use `@PreAuthorize` on the controller method when role enforcement is needed.

### Response Envelope

All endpoints return `Response` via `ResponseEntity<Response>`.
`@JsonInclude(NON_NULL)` strips unused fields automatically — only set the fields relevant to the endpoint.

---

## Frontend Conventions (React / JavaScript)

### File Structure

```
frontend/src/
  components/     Shared UI components (AppLayout, ConfirmationModal, etc.)
  features/       Domain-specific API hooks and Zod schemas
    <domain>/
      api.js      React Query hooks
      schema.js   Zod validation schemas
      <Form>.jsx  Form component
  pages/          Route-level page components
  lib/            Axios instance and shared utilities
```

### Naming

- Components and pages: `PascalCase.jsx`
- Hooks: `useCamelCase` (`useProducts`, `useCreateSupplier`)
- Files: `PascalCase.jsx` for components, `camelCase.js` for utilities

### API Calls

- All API calls go through `src/lib/axios.js`. Never use `fetch` directly.
- Import React Query hooks from `features/<domain>/api.js`.
- After any mutation that affects stock (purchase, sell, return), invalidate:
  `['products']`, `['transactions']`, `['dashboard-stats']`, `['transactions-by-month']`.

### Forms

- Use React Hook Form + Zod resolver for all forms.
- Define schema in `features/<domain>/schema.js`; import into the form component.
- Products use `FormData` (multipart) — never `JSON.stringify` for product mutations.

### Styling

- Tailwind CSS utility classes only — no inline `style` props unless strictly necessary.
- Follow the design tokens in `docs/DESIGN.md` for colours, shadows, and typography.
- Run `npm run format` before committing to apply Prettier formatting.

---

## Running Tests

**Backend** (from `Backend/`):
```bash
.\mvnw.cmd test
```

**Frontend** (from `frontend/`):
```bash
npm test
npm run format   # fix formatting before committing
npm run lint     # check ESLint rules
```

---

## Environment Setup

Copy the example files before starting:

```bash
# Backend
cp Backend/src/main/resources/application.properties.example \
   Backend/src/main/resources/application.properties

# Frontend
cp frontend/.env.example frontend/.env
```

See `README.md` for full setup instructions and `docs/DEPLOYMENT.md` for Docker and cloud deployment.
