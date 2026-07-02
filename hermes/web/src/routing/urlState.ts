export function readRouteFromUrl() {
  if (typeof window === 'undefined') return { type: '', path: '', view: '' };
  const params = new URLSearchParams(window.location.search);
  return {
    view: params.get('view') || '',
    type: params.get('type') || '',
    path: params.get('path') || ''
  };
}

export function writeRouteToUrl(type: string, path: string, replace = false, view = '') {
  if (typeof window === 'undefined') return;
  const url = new URL(window.location.href);
  if (view) url.searchParams.set('view', view);
  else url.searchParams.delete('view');
  if (type) url.searchParams.set('type', type);
  else url.searchParams.delete('type');
  if (path) url.searchParams.set('path', path);
  else url.searchParams.delete('path');
  const next = `${url.pathname}${url.search}${url.hash}`;
  const current = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  if (next === current) return;
  if (replace) {
    window.history.replaceState({}, '', next);
  } else {
    window.history.pushState({}, '', next);
  }
}
