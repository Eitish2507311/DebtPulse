import axios from 'axios';

/**
 * The access token lives in MEMORY only — never localStorage. The refresh token is an httpOnly,
 * SameSite=Strict cookie the browser manages and JS cannot read. So an XSS payload can steal (at
 * most) the short-lived access token until the tab reloads, and can never read the refresh token.
 */
let accessToken = null;
export const tokenStore = {
  get: () => accessToken,
  set: (t) => { accessToken = t || null; },
  clear: () => { accessToken = null; },
};

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
  withCredentials: true, // send/receive the httpOnly refresh-token cookie on /api/auth/*
});

// Attach the in-memory access token to every request.
api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
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

// Dedupe concurrent refreshes: the first caller kicks off one refresh; others await it.
let refreshing = null;

/**
 * Exchange the httpOnly refresh cookie for a fresh access token. No token is passed from JS —
 * the browser attaches the cookie automatically (withCredentials). Returns the full AuthResponse
 * (token + user profile) so callers can also restore the session on bootstrap.
 */
export function doRefresh() {
  if (!refreshing) {
    // Bare axios (bypasses interceptors); withCredentials so the cookie rides along.
    refreshing = axios
      .post(`${api.defaults.baseURL}/auth/refresh`, {},
        { withCredentials: true, headers: { 'Content-Type': 'application/json' } })
      .then((res) => { tokenStore.set(res.data.token); return res.data; })
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
    const isAuthCall = url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/logout');

    // On a 401 for a normal call, try one silent cookie-based refresh + retry before giving up.
    if (status === 401 && !isAuthCall && !original._retry) {
      original._retry = true;
      try {
        const data = await doRefresh();
        original.headers = original.headers || {};
        original.headers.Authorization = `Bearer ${data.token}`;
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
