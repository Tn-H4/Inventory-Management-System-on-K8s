import React from 'react';
import { Link, useLocation } from 'react-router-dom';

const NUMERIC_REGEX = /^\d+$/;
const UUID_REGEX =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Breadcrumbs - Navigation breadcrumb trail
 * @param {array} items - Array of { label, href } objects
 * @param {boolean} showHome - Include home link
 */
function Breadcrumbs({ items = [], showHome = true }) {
  const location = useLocation();

  // Auto-generate breadcrumbs from current path
  const autoBreadcrumbs = React.useMemo(() => {
    const pathParts = location.pathname.split('/').filter(Boolean);
    const crumbs = [];
    const seenHrefs = new Set();

    if (showHome) {
      crumbs.push({ label: 'Home', href: '/dashboard' });
      seenHrefs.add('/dashboard');
    }

    let currentPath = '';
    pathParts.forEach((part, index) => {
      currentPath += `/${part}`;

      if (NUMERIC_REGEX.test(part)) return;
      if (UUID_REGEX.test(part)) return;

      // Skip duplicate hrefs (e.g., /dashboard appears both as Home and from path)
      if (seenHrefs.has(currentPath)) {
        return;
      }
      seenHrefs.add(currentPath);

      // Format the label — title-case each hyphen-separated word
      const label = part
        .split('-')
        .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
        .join(' ');

      crumbs.push({ label, href: currentPath });
    });

    return crumbs.map((crumb, index) => ({
      ...crumb,
      isLast: index === crumbs.length - 1,
    }));
  }, [location.pathname, showHome]);

  const displayItems = items.length > 0 ? items : autoBreadcrumbs;

  if (displayItems.length === 0) {
    return null;
  }

  return (
    <nav
      className="flex items-center gap-2 text-sm mb-6"
      aria-label="Breadcrumb"
    >
      <ol className="flex items-center gap-1.5 flex-wrap">
        {displayItems.map((item, index) => {
          const isLast = item.isLast ?? index === displayItems.length - 1;

          return (
            <li key={item.href || index} className="flex items-center">
              {index > 0 && (
                <span className="mx-2 text-slate-400" aria-hidden="true">
                  /
                </span>
              )}

              {isLast ? (
                <span
                  className="font-medium text-slate-700"
                  aria-current="page"
                >
                  {item.label}
                </span>
              ) : (
                <Link
                  to={item.href}
                  className="text-slate-500 hover:text-primary transition-colors"
                >
                  {item.label}
                </Link>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

/**
 * BreadcrumbItem - Individual breadcrumb item
 */
function BreadcrumbItem({ label, href, isLast = false }) {
  if (isLast) {
    return (
      <span className="font-medium text-slate-700" aria-current="page">
        {label}
      </span>
    );
  }

  return (
    <Link
      to={href}
      className="text-slate-500 hover:text-primary transition-colors"
    >
      {label}
    </Link>
  );
}

export { Breadcrumbs, BreadcrumbItem };
export default Breadcrumbs;
