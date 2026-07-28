import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { AppError } from '../types';

const TOKEN_KEY = 'dp_token';
const REFRESH_KEY = 'dp_refresh';

export const tokenStore = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  getRefresh: (): string | null => localStorage.getItem(REFRESH_KEY),
  setRefresh: (t?: string | null) => { if (t) localStorage.setItem(REFRESH_KEY, t); },
  clear: () => { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(REFRESH_KEY); },
};

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
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

// Dedupe concurrent refreshes: the first 401 kicks off one refresh; others await it.
let refreshing: Promise<string> | null = null;

function doRefresh(): Promise<string> {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) return Promise.reject(new Error('no refresh token'));
  if (!refreshing) {
    refreshing = axios
      .post<{ token: string; refreshToken: string }>(
        `${api.defaults.baseURL}/auth/refresh`, { refreshToken },
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
  async (error: AxiosError) => {
    const status = error?.response?.status;
    const original = (error?.config || {}) as InternalAxiosRequestConfig & { _retry?: boolean };
    const url = original.url || '';
    const isAuthCall = url.includes('/auth/login') || url.includes('/auth/refresh');

    if (status === 401 && !isAuthCall && !original._retry) {
      original._retry = true;
      try {
        const token = await doRefresh();
        original.headers.Authorization = `Bearer ${token}`;
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
