import { createContext, useContext, useEffect, useMemo, useState, useCallback, type ReactNode } from 'react';
import { authApi } from '../api/services';
import { tokenStore, setUnauthorizedHandler, toAppError } from '../api/client';
import type { AppError, Role, SessionUser } from '../types';

const USER_KEY = 'dp_user';

interface AuthContextValue {
  user: SessionUser | null;
  role: Role | undefined;
  isAuthenticated: boolean;
  ready: boolean;
  login: (email: string, password: string) => Promise<{ ok: boolean; error?: AppError }>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(() => {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch { return null; }
  });
  const [ready, setReady] = useState(true);

  const logout = useCallback(() => {
    const refreshToken = tokenStore.getRefresh();
    if (refreshToken) authApi.logout(refreshToken).catch(() => {});
    tokenStore.clear();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => logout());
    setReady(true);
  }, [logout]);

  const login = useCallback(async (email: string, password: string) => {
    try {
      const { data } = await authApi.login(email, password);
      tokenStore.set(data.token);
      tokenStore.setRefresh(data.refreshToken);
      const u: SessionUser = { userId: data.userId, role: data.role, name: data.name, branchId: data.branchId, email };
      localStorage.setItem(USER_KEY, JSON.stringify(u));
      setUser(u);
      return { ok: true };
    } catch (e) {
      return { ok: false, error: toAppError(e) };
    }
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    role: user?.role,
    isAuthenticated: !!user && !!tokenStore.get(),
    ready,
    login,
    logout,
  }), [user, ready, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = (): AuthContextValue => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
