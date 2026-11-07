import React from 'react';

/**
 * Highlights matching substrings in a text string.
 * @param {string} text The text to search in.
 * @param {string} term The search term to highlight.
 * @returns {React.ReactNode} The text with matches wrapped in <mark> tags.
 */
export const highlightMatch = (text, term) => {
  if (!text) return null;
  if (!term) return text;
  const escapedTerm = term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const regex = new RegExp(`(${escapedTerm})`, 'gi');
  const parts = text.split(regex);
  return parts.map((part, i) =>
    regex.test(part) ? (
      <mark key={i} className="bg-yellow-100 text-yellow-800 rounded px-0.5">
        {part}
      </mark>
    ) : (
      part
    )
  );
};
