import axios from 'axios';

const TOKEN_KEY = 'dp_token';
const REFRESH_KEY = 'dp_refresh';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  getRefresh: () => localStorage.getItem(REFRESH_KEY),
  setRefresh: (t) => { if (t) localStorage.setItem(REFRESH_KEY, t); },
  clear: () => { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(REFRESH_KEY); },
};

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

// Attach the JWT to every request.
api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Allow the app to react to auth failures (redirect to login) without a hard dependency.
let onUnauthorized = null;
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

// Normalise the backend's ErrorResponse envelope into a predictable shape.
export function toAppError(error) {
  const res = error?.response;
  if (!res) {
    return { status: 0, message: 'Cannot reach the server. Check that the API gateway is running.', fieldErrors: {} };
  }
  const data = res.data || {};
  return {
    status: res.status,
    message: data.message || data.error || `Request failed (${res.status})`,
    ruleCode: data.ruleCode,
    fieldErrors: data.fieldErrors || {},
  };
}

// Dedupe concurrent refreshes: the first 401 kicks off one refresh; others await it.
let refreshing = null;

function doRefresh() {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) return Promise.reject(new Error('no refresh token'));
  if (!refreshing) {
    // Use a bare axios call (not `api`) so this request bypasses the interceptors.
    refreshing = axios
      .post(`${api.defaults.baseURL}/auth/refresh`, { refreshToken },
        { headers: { 'Content-Type': 'application/json' } })
      .then((res) => {
        tokenStore.set(res.data.token);
        tokenStore.setRefresh(res.data.refreshToken);
        return res.data.token;
      })
      .finally(() => { refreshing = null; });
  }
  return refreshing;
}

api.interceptors.response.use(
  (r) => r,
  async (error) => {
    const status = error?.response?.status;
    const original = error?.config || {};
    const url = original.url || '';
    const isAuthCall = url.includes('/auth/login') || url.includes('/auth/refresh');

    // On a 401 for a normal call, try one silent refresh + retry before giving up.
    if (status === 401 && !isAuthCall && !original._retry) {
      original._retry = true;
      try {
        const token = await doRefresh();
        original.headers = original.headers || {};
        original.headers.Authorization = `Bearer ${token}`;
        return api(original);
      } catch {
        if (onUnauthorized) onUnauthorized();
        return Promise.reject(error);
      }
    }
    if (status === 401 && !isAuthCall && onUnauthorized) onUnauthorized();
    return Promise.reject(error);
  }
);

export default api;
