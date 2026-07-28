import { createContext, useContext, useEffect, useMemo, useState, useCallback, type ReactNode } from 'react';
import { authApi } from '../api/services';
import { tokenStore, setUnauthorizedHandler, toAppError, doRefresh } from '../api/client';
import type { AppError, Role, SessionUser } from '../types';

interface AuthContextValue {
  user: SessionUser | null;
  role: Role | undefined;
  isAuthenticated: boolean;
  ready: boolean;
  login: (email: string, password: string) => Promise<{ ok: boolean; error?: AppError }>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** Best-effort decode of the JWT payload (for the email claim only — never trusted for auth). */
function jwtEmail(token: string): string | undefined {
  try { return JSON.parse(atob(token.split('.')[1])).email; } catch { return undefined; }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null);
  const [ready, setReady] = useState(false);

  const clearSession = useCallback(() => { tokenStore.clear(); setUser(null); }, []);

  const logout = useCallback(() => {
    // Server revokes the session and clears the httpOnly cookie; then clear the in-memory token.
    authApi.logout().catch(() => {});
    clearSession();
  }, [clearSession]);

  useEffect(() => { setUnauthorizedHandler(() => clearSession()); }, [clearSession]);

  // Bootstrap: the in-memory access token is gone after a reload, but the httpOnly refresh cookie
  // may still be valid — try one silent refresh to transparently restore the session.
  useEffect(() => {
    let alive = true;
    doRefresh()
      .then((data) => {
        if (!alive) return;
        setUser({ userId: data.userId, role: data.role as Role, name: data.name,
          branchId: data.branchId ?? '', email: jwtEmail(data.token) ?? '' });
      })
      .catch(() => { if (alive) clearSession(); })
      .finally(() => { if (alive) setReady(true); });
    return () => { alive = false; };
  }, [clearSession]);

  const login = useCallback(async (email: string, password: string) => {
    try {
      const { data } = await authApi.login(email, password);
      tokenStore.set(data.token); // refresh token is set as an httpOnly cookie by the server
      const u: SessionUser = { userId: data.userId, role: data.role, name: data.name, branchId: data.branchId, email };
      setUser(u);
      return { ok: true };
    } catch (e) {
      return { ok: false, error: toAppError(e) };
    }
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    role: user?.role,
    isAuthenticated: !!user,
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
