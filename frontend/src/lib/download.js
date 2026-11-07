/**
 * Triggers a browser file download for a given Blob.
 * @param {Blob} blob - The data to download.
 * @param {string} filename - The suggested filename for the download.
 */
export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
