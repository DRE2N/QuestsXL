export const api = async <T,>(path: string, init?: RequestInit): Promise<T> => {
  const response = await fetch(path, {
    credentials: 'include',
    headers: init?.body ? { 'Content-Type': 'application/json', ...(init.headers || {}) } : init?.headers,
    ...init
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error || data.message || response.statusText);
  return data as T;
};

export const apiRaw = async (path: string, init?: RequestInit): Promise<Response> => {
  const response = await fetch(path, { credentials: 'include', ...init });
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new Error(data.error || data.message || response.statusText);
  }
  return response;
};
