import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { AppError } from '../types';

/**
 * The access token lives in MEMORY only — never localStorage. The refresh token is an httpOnly,
 * SameSite=Strict cookie the browser manages and JS cannot read. So an XSS payload can steal (at
 * most) the short-lived access token until the tab reloads, and can never read the refresh token.
 */
let accessToken: string | null = null;
export const tokenStore = {
  get: (): string | null => accessToken,
  set: (t: string | null) => { accessToken = t || null; },
  clear: () => { accessToken = null; },
};

/** Shape of the /auth/login and /auth/refresh response body (refresh token is NOT included). */
export interface AuthResult {
  token: string;
  expiresIn?: number;
  userId: string;
  role: string;
  name: string;
  branchId?: string;
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
  withCredentials: true, // send/receive the httpOnly refresh-token cookie on /api/auth/*
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

let onUnauthorized: (() => void) | null = null;
export const setUnauthorizedHandler = (fn: () => void) => { onUnauthorized = fn; };

interface ErrorEnvelope { message?: string; error?: string; ruleCode?: string; fieldErrors?: Record<string, string>; }

export function toAppError(error: unknown): AppError {
  const err = error as AxiosError<ErrorEnvelope>;
  const res = err?.response;
  if (!res) {
    return { status: 0, message: 'Cannot reach the server. Check that the API gateway is running.', fieldErrors: {} };
  }
  const data = (res.data || {}) as ErrorEnvelope;
  return {
    status: res.status,
    message: data.message || data.error || `Request failed (${res.status})`,
    ruleCode: data.ruleCode,
    fieldErrors: data.fieldErrors || {},
  };
}

// Dedupe concurrent refreshes: the first caller kicks off one refresh; others await it.
let refreshing: Promise<AuthResult> | null = null;

/**
 * Exchange the httpOnly refresh cookie for a fresh access token. No token is passed from JS —
 * the browser attaches the cookie automatically (withCredentials). Returns the full result so
 * callers can also restore the session on bootstrap.
 */
export function doRefresh(): Promise<AuthResult> {
  if (!refreshing) {
    refreshing = axios
      .post<AuthResult>(`${api.defaults.baseURL}/auth/refresh`, {},
        { withCredentials: true, headers: { 'Content-Type': 'application/json' } })
      .then((res) => { tokenStore.set(res.data.token); return res.data; })
      .finally(() => { refreshing = null; });
  }
  return refreshing;
}

api.interceptors.response.use(
  (r) => r,
  async (error: AxiosError) => {
    const status = error?.response?.status;
    const original = (error?.config || {}) as InternalAxiosRequestConfig & { _retry?: boolean };
    const url = original.url || '';
    const isAuthCall = url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/logout');

    if (status === 401 && !isAuthCall && !original._retry) {
      original._retry = true;
      try {
        const data = await doRefresh();
        original.headers.Authorization = `Bearer ${data.token}`;
        return api(original);
      } catch {
        if (onUnauthorized) onUnauthorized();
        return Promise.reject(error);
      }
    }
    if (status === 401 && !isAuthCall && onUnauthorized) onUnauthorized();
    return Promise.reject(error);
  },
);

export default api;
