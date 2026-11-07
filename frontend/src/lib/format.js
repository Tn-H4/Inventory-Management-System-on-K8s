/**
 * Format a number as USD currency string
 * @param {number|string} value - The value to format
 * @returns {string} Formatted currency string (e.g., "$12.50")
 */
export function formatCurrency(value) {
  return `$${Number(value || 0).toFixed(2)}`;
}
