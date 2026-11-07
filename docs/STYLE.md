# STYLE.md — Frontend Design System Reference

> Authoritative guide for UI patterns, design tokens, accessibility, and component usage.
> Keep this file in sync with `tailwind.config.js` and `src/styles/design-tokens.css`.

---

## 1. Design Tokens

### Color Palette (Tailwind utility → hex)

| Token | Utility class | Hex | Usage |
|---|---|---|---|
| Primary | `bg-primary` / `text-primary` | `#1f5f89` | CTAs, active nav, focus rings |
| Primary Light | `bg-primary-light` | `#2d7ab0` | Hover tints |
| Primary Dark | `bg-primary-dark` | `#154a68` | Button hover states |
| Primary Subtle | `bg-primary-subtle` | `#e8f2f8` | Light tint backgrounds |
| Accent | `bg-accent` / `text-accent` | `#3e3998` | Secondary highlights |
| Accent Light | `bg-accent-light` | `#5a55c4` | Accent hover |
| Success | `text-success` / `bg-success-light` | `#059669` / `#d1fae5` | Positive states |
| Warning | `text-warning` / `bg-warning-light` | `#d97706` / `#fef3c7` | Caution states |
| Error | `text-error` / `bg-error-light` | `#dc2626` / `#fee2e2` | Destructive states |
| Info | `text-info` / `bg-info-light` | `#0284c7` / `#e0f2fe` | Informational states |

### Slate Neutral Scale

Use `slate-*` for all text, borders, and backgrounds — never Tailwind defaults like `gray-*`.

| Utility | Hex | Common use |
|---|---|---|
| `bg-slate-50` | `#f8fafc` | Page/section backgrounds, form inputs |
| `bg-slate-100` | `#f1f5f9` | App shell background |
| `border-slate-200` | `#e2e8f0` | Card/table/input borders |
| `text-slate-500` | `#64748b` | Secondary / placeholder text |
| `text-slate-700` | `#334155` | Body text |
| `text-slate-900` | `#0f172a` | Headings / primary text |

### Shadows

| Tailwind class | Usage |
|---|---|
| `shadow-soft` | Cards, panels, buttons |
| `shadow-card` | Hover elevation on cards |
| `shadow-elevated` | Modals, dropdowns |
| `shadow-focus` | Custom focus ring (3px, primary/20) |

### Border Radius

| Tailwind class | Value | Usage |
|---|---|---|
| `rounded-lg` | 12px | Buttons, inputs, badges |
| `rounded-xl` | 16px | Inner panels, table wrappers |
| `rounded-2xl` | 24px | Page cards (mobile) |
| `rounded-3xl` | 32px | Page cards (desktop) |

---

## 2. Typography

### Font Families

| Tailwind class | Font | When to use |
|---|---|---|
| `font-display` | Outfit | **Headings** (`h1`–`h3`), navigation labels, table column headers, form labels in headings |
| `font-body` | DM Sans | Body text, paragraphs, form field values — this is the default via `tailwind.config.js` |
| `font-mono` | JetBrains Mono | IDs, codes, SKUs, transaction IDs |

### Heading Patterns

Always pair `font-display font-bold` with the appropriate size:

```jsx
// Page title (every page)
<h1 className="text-2xl font-display font-bold text-slate-900">...</h1>

// Section heading inside a card
<h2 className="text-xl font-semibold text-slate-900">...</h2>

// Sub-section / detail card title
<h3 className="text-lg font-display font-bold text-slate-900">...</h3>
```

**Do not** use `text-3xl font-bold` without `font-display` — it renders in DM Sans instead of Outfit.

---

## 3. Color Usage Rules

- **CTAs and active states** — always `bg-primary` / `hover:bg-primary-dark`, never `bg-indigo-*`.
- **Focus rings** — always `focus:ring-primary/20 focus:border-primary`, never `focus:ring-indigo-500`.
- **Form field backgrounds** — always `bg-slate-50`, never `bg-gray-50`.
- **Borders** — always `border-slate-200`, never `border-gray-200`.
- **Error/success backgrounds** — use the semantic tokens: `bg-error-light`, `bg-success-light`, etc.
- **Do not** hardcode hex colors anywhere in JSX. Use Tailwind tokens or CSS variables.

---

## 4. Component Reference

### Button (primary action)

```jsx
<button
  type="button"         // always specify type; default "submit" can cause bugs
  onClick={handler}
  disabled={isPending}
  className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-primary-dark disabled:opacity-60 transition-all"
>
  Label
</button>
```

For icon-only buttons always add `aria-label`:

```jsx
<button type="button" aria-label="Delete item" className="...">
  <svg aria-hidden="true">...</svg>
</button>
```

### ConfirmationModal / useConfirmation

```jsx
import { useConfirmation } from '../components/ConfirmationModal';

const { confirm, modal } = useConfirmation({
  title: 'Delete Item',
  message: 'Are you sure?',
  confirmText: 'Delete',
  cancelText: 'Cancel',
  type: 'danger',          // 'danger' | 'warning' | 'info'
});

// In handler:
const confirmed = await confirm();
if (confirmed) { /* proceed */ }

// In JSX:
return <>{modal}</>;
```

### EmptyState / ErrorState / LoadingState

```jsx
import { EmptyState, ErrorState, LoadingState } from '../components/EmptyState';

// Loading
<LoadingState title="Loading products..." message="Please wait." />

// Error
<ErrorState title="Failed to load" message={error?.message} />

// Empty
<EmptyState title="No products" description="Add a product to get started." />
```

Use these instead of inline spinners or ad-hoc empty messages.

### PaginationComponent

```jsx
import PaginationComponent from '../components/PaginationComponent';

<PaginationComponent
  currentPage={page}
  totalPages={data?.totalPages ?? 1}
  onPageChange={setPage}
/>
```

Note: Categories are 1-indexed; Transactions are 0-indexed (pass `page - 1` to backend).

### Breadcrumbs

Auto-generation works for most routes — no props needed:

```jsx
// In AppLayout.jsx — already rendered automatically for every page
<Breadcrumbs />
```

Override with the `items` prop when you need entity names (e.g., product name instead of "Edit"):

```jsx
<Breadcrumbs
  items={[
    { label: 'Home', href: '/dashboard' },
    { label: 'Products', href: '/products' },
    { label: product.name, href: `/products/${id}/edit`, isLast: true },
  ]}
/>
```

Auto-generator rules:
- Skips numeric segments (e.g., `42` in `/categories/42/edit`)
- Skips UUID segments (e.g., `{uuid}` in `/products/{uuid}/edit`)
- Title-cases hyphenated words: `audit-logs` → `Audit Logs`
- Does **not** skip entity-collection segments anymore (`products`, `categories`, etc. appear in the trail)

---

## 5. Page Layout Pattern

`AppLayout` renders `<Breadcrumbs />` and a white card wrapper around `<Outlet />`:

```jsx
// AppLayout renders:
<div className="w-full bg-white rounded-2xl lg:rounded-3xl shadow-soft p-4 sm:p-8 animate-fade-in">
  <Outlet />
</div>
```

**Pages must NOT add their own outer card wrapper.** Start directly with the page content:

```jsx
// Correct — page content directly
function ProductsPage() {
  return (
    <div>
      <h1 className="text-2xl font-display font-bold text-slate-900">Products</h1>
      ...
    </div>
  );
}

// Wrong — double card nesting
function ProductsPage() {
  return (
    <div className="bg-white rounded-2xl shadow-soft p-8">  // ← do NOT add this
      ...
    </div>
  );
}
```

Exception: `ProductEditPage` uses `w-full min-h-full flex items-center justify-center` as a centering wrapper, which is acceptable.

---

## 6. Form Pattern

### Field components

Use shared form input components from `components/FormInput.jsx`:

```jsx
import { FormInput, FormSelect, FormTextarea, FormCheckbox } from '../components/FormInput';
```

For complex one-off forms (like `SupplierForm`), use raw inputs but apply design tokens:

```jsx
<input
  className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-900
             focus:bg-white focus:ring-2 focus:ring-primary/20 focus:border-primary
             transition-all outline-none"
/>
```

### Form setup

```jsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { mySchema, myDefaults } from './schema';

const { register, handleSubmit, formState: { errors } } = useForm({
  resolver: zodResolver(mySchema),
  defaultValues: myDefaults,
});
```

### Server error display

```jsx
{serverError && (
  <div className="p-3 rounded-lg bg-red-50 border border-red-100 text-sm text-red-600 animate-slide-up">
    {serverError}
  </div>
)}
```

### Submit button

```jsx
<button
  type="submit"
  disabled={isPending}
  className="w-full bg-primary hover:bg-primary-dark text-white font-semibold py-3.5 rounded-lg
             shadow-md transition-all disabled:opacity-70 disabled:cursor-not-allowed
             flex justify-center items-center"
>
  {isPending ? 'Processing...' : submitLabel}
</button>
```

---

## 7. Table Pattern

```jsx
<div className="overflow-x-auto rounded-xl border border-slate-200">
  <table className="min-w-full divide-y divide-slate-200">
    <thead className="bg-slate-50">
      <tr>
        {/* scope="col" is required on every <th> in <thead> — WCAG 1.3.1 */}
        <th scope="col" className="px-6 py-3 text-left text-xs font-display font-semibold uppercase tracking-wider text-slate-700">
          Column Name
        </th>
      </tr>
    </thead>
    <tbody className="divide-y divide-slate-200 bg-white">
      <tr>
        <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">...</td>
      </tr>
    </tbody>
  </table>
</div>
```

### Mobile card pattern

Pair every data table with a mobile card layout shown only on small screens:

```jsx
{/* Desktop table */}
<div className="hidden md:block overflow-x-auto ...">
  <table>...</table>
</div>

{/* Mobile cards */}
<div className="md:hidden space-y-3">
  {items.map(item => (
    <div key={item.id} className="rounded-xl border border-slate-200 bg-white p-4 shadow-soft">
      ...
    </div>
  ))}
</div>
```

---

## 8. Accessibility Checklist

Before committing any UI change, verify:

- [ ] Every `<th>` in `<thead>` has `scope="col"` (WCAG 1.3.1)
- [ ] Every `<input>` / `<select>` / `<textarea>` is either wrapped in a `<label>` or referenced via `htmlFor` / `aria-label` (WCAG 4.1.2)
- [ ] Decorative SVGs inside buttons with `aria-label` have `aria-hidden="true"` on the `<svg>`
- [ ] The last breadcrumb item has `aria-current="page"`
- [ ] Modals have `role="dialog"` + `aria-modal="true"` + `aria-labelledby`
- [ ] Interactive elements meet minimum 44×44px touch target size
- [ ] Disabled buttons use the `disabled` attribute (not just visual styling)
- [ ] Color is never the sole means of conveying information (pair with text/icon)

### Icon-only button template

```jsx
<button type="button" aria-label="Close dialog" className="p-2 ...">
  <svg aria-hidden="true" className="w-5 h-5" ...>...</svg>
</button>
```

---

## 9. Breadcrumb Usage

The auto-generator in `Breadcrumbs.jsx` handles most routes:

| Route | Auto result |
|---|---|
| `/dashboard` | Home |
| `/products` | Home > Products |
| `/products/create` | Home > Products > Create |
| `/products/{uuid}/edit` | Home > Products > Edit |
| `/categories/{id}/edit` | Home > Categories > Edit |
| `/transactions/{id}` | Home > Transactions |
| `/audit-logs` | Home > Audit Logs |

Use the `items` prop only when you need to show an entity's actual name (e.g., the product name on an edit page) rather than a path segment.

---

## 10. Responsive Patterns

### Breakpoints

| Prefix | Min-width | Typical use |
|---|---|---|
| (none) | 0px | Mobile-first base styles |
| `sm:` | 640px | Small tablets |
| `md:` | 768px | Tablets — show tables, hide mobile cards |
| `lg:` | 1024px | Desktop — show sidebar, wider layouts |
| `xl:` | 1280px | Wide desktop |

### Sidebar / hamburger

The sidebar is `hidden lg:flex` (desktop only). The hamburger button is `lg:hidden` in the mobile header. Never show both simultaneously.

### Page padding scaling

```jsx
<div className="p-4 sm:p-6 lg:p-8">
```

### Grid scaling

```jsx
<div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
```

---

## 11. State Patterns (React Query)

```jsx
const { data, isLoading, isError, error } = useMyQuery();

if (isLoading) return <LoadingState title="Loading..." message="Please wait." />;
if (isError || !data) return <ErrorState title="Error" message={error?.message} />;
if (data.items.length === 0) return <EmptyState title="No items" description="..." />;

// Happy path
return <div>...</div>;
```

After mutations that change stock, always invalidate these query keys:
- `['products']`
- `['transactions']`
- `['dashboard-stats']`
- `['transactions-by-month']`

---

## 12. Animation

| Class | When to use |
|---|---|
| `animate-fade-in` | Page card entrance (applied by AppLayout) |
| `animate-slide-up` | Form validation errors appearing |
| `animate-slide-in` | Side panels / drawers |
| `animate-scale-in` | Modals appearing |

Do not add animation to every element — use sparingly for entrance transitions only.

---

## 13. What NOT to Do

| Wrong | Correct |
|---|---|
| `bg-indigo-600` / `bg-indigo-700` | `bg-primary` / `bg-primary-dark` |
| `focus:ring-indigo-500` | `focus:ring-primary/20` |
| `bg-gray-50` / `border-gray-200` | `bg-slate-50` / `border-slate-200` |
| Hardcoded hex `#1f5f89` in JSX | Tailwind token `bg-primary` |
| `text-3xl font-bold` on `<h1>` | `text-2xl font-display font-bold` |
| `<th>` without `scope="col"` | `<th scope="col">` |
| `<input>` without label or `aria-label` | Add `aria-label` or `<label htmlFor>` |
| SVG inside labelled button without `aria-hidden` | Add `aria-hidden="true"` to SVG |
| Outer card wrapper inside a page component | Let AppLayout provide the card |
| `<button>` without `type` attribute | Always specify `type="button"` or `type="submit"` |
