/**
 * Parses a sort option string in the format "column:direction" (e.g., "name:ASC").
 * @param {string} sortOption - The sort option string.
 * @returns {{ sortBy: string, sortDir: string }} The parsed sort column and direction.
 */
export function parseSortOption(sortOption) {
  if (!sortOption) return { sortBy: 'id', sortDir: 'ASC' };
  const [sortBy, sortDir] = sortOption.split(':');
  return {
    sortBy: sortBy || 'id',
    sortDir: sortDir || 'ASC',
  };
}
