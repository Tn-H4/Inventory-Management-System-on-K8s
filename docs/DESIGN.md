# Design Documentation

## Overview

This document tracks the UI/UX design decisions for the Inventory Management System. The design follows a professional, modern aesthetic with emphasis on usability, accessibility, and consistency.

---

## Design System

### Color Palette

| Token                    | Hex       | Usage                              |
| ------------------------ | --------- | ---------------------------------- |
| `--color-primary`        | `#1f5f89` | Main brand color - Deep Ocean Blue |
| `--color-primary-light`  | `#2d7ab0` | Hover states                       |
| `--color-primary-dark`   | `#154a68` | Active states                      |
| `--color-primary-subtle` | `#e8f2f8` | Backgrounds, highlights            |
| `--color-accent`         | `#3E3998` | Secondary highlights, charts       |
| `--color-success`        | `#059669` | Success states                     |
| `--color-warning`        | `#d97706` | Warning states                     |
| `--color-error`          | `#dc2626` | Error states                       |
| `--color-info`           | `#0284c7` | Info states                        |

### Neutral Scale (Slate)

| Token                 | Hex       | Usage            |
| --------------------- | --------- | ---------------- |
| `--color-neutral-50`  | `#f8fafc` | Page backgrounds |
| `--color-neutral-100` | `#f1f5f9` | Card backgrounds |
| `--color-neutral-200` | `#e2e8f0` | Borders          |
| `--color-neutral-300` | `#cbd5e1` | Disabled states  |
| `--color-neutral-400` | `#94a3b8` | Placeholders     |
| `--color-neutral-500` | `#64748b` | Secondary text   |
| `--color-neutral-600` | `#475569` | Body text        |
| `--color-neutral-700` | `#334155` | Headings         |
| `--color-neutral-800` | `#1e293b` | Dark text        |
| `--color-neutral-900` | `#0f172a` | Primary text     |

### Typography

| Family  | Font           | Usage            |
| ------- | -------------- | ---------------- |
| Display | Outfit         | Headings, titles |
| Body    | DM Sans        | Body text, forms |
| Mono    | JetBrains Mono | Code, monospace  |

**Font Sizes:**

- Display: 5xl (48px), 4xl (36px), 3xl (30px), 2xl (24px)
- Body: xl (20px), lg (18px), base (16px), sm (14px), xs (12px)

### Spacing Scale

| Token        | Value |
| ------------ | ----- |
| `--space-1`  | 4px   |
| `--space-2`  | 8px   |
| `--space-3`  | 12px  |
| `--space-4`  | 16px  |
| `--space-5`  | 20px  |
| `--space-6`  | 24px  |
| `--space-8`  | 32px  |
| `--space-10` | 40px  |
| `--space-12` | 48px  |

### Border Radius

| Token          | Value | Usage           |
| -------------- | ----- | --------------- |
| `--radius-sm`  | 6px   | Small elements  |
| `--radius-md`  | 8px   | Inputs          |
| `--radius-lg`  | 12px  | Buttons, cards  |
| `--radius-xl`  | 16px  | Modals          |
| `--radius-2xl` | 24px  | Page containers |
| `--radius-3xl` | 32px  | Full layouts    |

### Shadows

| Token         | Value         | Usage             |
| ------------- | ------------- | ----------------- |
| `--shadow-sm` | `0 1px 2px`   | Subtle elevation  |
| `--shadow-md` | `0 4px 6px`   | Cards, dropdowns  |
| `--shadow-lg` | `0 10px 15px` | Modals, dialogs   |
| `--shadow-xl` | `0 20px 25px` | Floating elements |

### Animations

| Name      | Duration | Easing   | Usage                |
| --------- | -------- | -------- | -------------------- |
| fadeIn    | 200ms    | ease-out | Modal appearance     |
| slideUp   | 300ms    | ease-out | Form errors          |
| slideDown | 300ms    | ease-out | Dropdowns            |
| scaleIn   | 200ms    | ease-out | Modal scale          |
| skeleton  | 1500ms   | infinite | Loading placeholders |

---

## Components

### 1. Loading States

- **LoadingSpinner**: Circular spinner with size variants (sm/md/lg) and color options
- **LoadingOverlay**: Full-screen backdrop with centered spinner
- **PageLoader**: Initial page loading state
- **Skeleton**: Animated placeholder for content loading

### 2. Form Components

- **FormInput**: Enhanced input with validation states, icons, focus effects
- **FormTextarea**: Multi-line input with same styling
- **FormSelect**: Dropdown with custom arrow, focus states
- **FormCheckbox**: Custom styled checkbox

### 3. Buttons

- **Button**: Primary/Secondary/Danger/Ghost/Outline variants
- **IconButton**: Icon-only buttons
- **FloatingActionButton**: FAB for primary actions

### 4. Feedback Components

- **ConfirmationModal**: Custom modal dialog (replaces window.confirm)
- **EmptyState**: Empty data display with CTA
- **ErrorState**: Error display with retry
- **LoadingState**: Loading display

### 5. Navigation

- **Breadcrumbs**: Auto-generated navigation trail
- **AppLayout**: Sidebar + header layout with mobile hamburger menu

### 6. Tooltips

- **Tooltip**: Hover tooltip with positioning
- **IconTooltip**: Icon-triggered tooltip

---

## Design Principles

### 1. Visual Hierarchy

- Clear distinction between headings and body text
- Consistent spacing and padding
- Purposeful use of color to guide attention

### 2. Consistency

- Same border-radius across similar elements
- Uniform padding (16px base)
- Standardized spacing scale

### 3. Feedback

- Loading states for all async operations
- Clear error messages with specific guidance
- Success confirmations where appropriate

### 4. Accessibility

- Proper focus indicators (2px solid, offset 2px)
- ARIA labels on interactive elements
- Keyboard navigation support
- Color contrast compliant

### 5. Mobile-First

- Responsive layouts using Tailwind breakpoints
- Touch-friendly button sizes (min 44px)
- Mobile navigation with hamburger menu

---

## Page-Specific Decisions

### Login/Register Pages

- Clean, centered card layout
- Icon prefixes in form inputs
- Clear validation feedback
- Professional branding with logo

### Dashboard

- Card-based metric displays
- Interactive charts (Recharts)
- Month/year selectors

### Data Pages (Products, Suppliers, etc.)

- Grid layout for cards
- Search with icon prefix
- Filter dropdowns
- Pagination component
- Empty states with CTAs

### Forms

- Full-width inputs
- Floating validation errors
- Icon prefixes where helpful
- Loading states on submit

---

## Implementation Notes

### Tailwind Configuration

Extended tailwind.config.js with:

- Custom colors (primary, accent, semantic)
- Custom font families (display, body, mono)
- Custom shadows (soft, card, elevated, focus)
- Custom animations
- Z-index scale for layering

### CSS Architecture

- Design tokens in `src/styles/design-tokens.css`
- Tailwind utilities for rapid development
- CSS custom properties for theming
- Animation utilities

---

## Version History

| Date       | Version | Changes                                                                                       |
| ---------- | ------- | --------------------------------------------------------------------------------------------- |
| 2024-05-03 | 1.0.0   | Initial design system                                                                         |
| 2024-05-03 | 1.0.1   | Added Breadcrumbs, Mobile nav                                                                 |
| 2024-05-03 | 1.0.2   | Added Loading components, Modals                                                              |
| 2025-05-03 | 1.1.0   | Page UI overhaul - Added EmptyState, LoadingState, ErrorState, ConfirmationModal to all pages |

---

## Page Updates (v1.1.0)

The following pages have been updated with the new design system components:

### Data Management Pages

- **CategoryPage**: Added EmptyState, ErrorState, LoadingState, ConfirmationModal for delete
- **UserManagementPage**: Added EmptyState, ErrorState, LoadingState, ConfirmationModal for delete
- **TransactionsPage**: Added EmptyState, ErrorState, LoadingState
- **SupplierPage**: Added EmptyState, ErrorState, LoadingState, ConfirmationModal for delete
- **ProductPage**: Added EmptyState, ErrorState, LoadingState, ConfirmationModal for delete

### Form Pages

- **PurchasePage**: Added LoadingState for data loading
- **SellPage**: Added LoadingState for data loading
- **ReturnPage**: Added LoadingState, updated styling with design tokens
- **ProductCreatePage**: Existing form with ProductForm component
- **ProductEditPage**: Added EmptyState, ErrorState, LoadingState, ConfirmationModal for supplier removal
- **SupplierCreatePage**: Existing form with SupplierForm component
- **SupplierEditPage**: Added EmptyState, ErrorState, LoadingState

### Detail Pages

- **TransactionDetailsPage**: Added ErrorState, LoadingState, updated styling with design tokens
- **ProfilePage**: Added ErrorState, LoadingState

### Authentication Pages

- **LoginPage**: Updated with design system, icon prefixes, loading states
- **RegisterPage**: Updated with design system, icon prefixes, loading states

### Common Updates

- All pages now use `shadow-soft` for card containers
- All pages now use `rounded-2xl` for border radius consistency
- All pages now use `font-display` for headings
- All pages use semantic colors: `text-error`, `bg-error-light`, etc.
- `window.confirm()` replaced with `useConfirmation()` modal hook for consistency

---

## Future Considerations

- Dark mode support
- Reduced motion preferences
- Custom theme builder
- Component library documentation site
