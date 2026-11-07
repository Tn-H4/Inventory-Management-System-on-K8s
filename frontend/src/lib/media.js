const apiBaseUrl = process.env.REACT_APP_API_BASE_URL || '';
const mediaBaseUrl = apiBaseUrl.replace(/\/api\/?$/, '');

export function resolveMediaUrl(url) {
  if (!url) return '';
  if (url.startsWith('blob:') || url.startsWith('data:')) return url;
  if (/^https?:\/\//i.test(url)) return url;

  const normalizedUrl = url.startsWith('/') ? url : `/${url}`;
  if (normalizedUrl.startsWith('/products/')) return normalizedUrl;

  return `${mediaBaseUrl}${normalizedUrl}`;
}
