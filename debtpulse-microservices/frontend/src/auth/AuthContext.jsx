import { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react';
import { authApi } from '../api/services.js';
import { tokenStore, setUnauthorizedHandler, toAppError } from '../api/client.js';

const USER_KEY = 'dp_user';
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem(USER_KEY)); } catch { return null; }
  });
  const [ready, setReady] = useState(true);

  const logout = useCallback(() => {
    // Best-effort server-side revocation of the refresh-token session, then clear locally.
    const refreshToken = tokenStore.getRefresh();
    if (refreshToken) authApi.logout(refreshToken).catch(() => {});
    tokenStore.clear();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  // Any 401 from the API layer bounces the session.
  useEffect(() => {
    setUnauthorizedHandler(() => logout());
    setReady(true);
  }, [logout]);

  const login = useCallback(async (email, password) => {
    try {
      const { data } = await authApi.login(email, password);
      tokenStore.set(data.token);
      tokenStore.setRefresh(data.refreshToken);
      const u = { userId: data.userId, role: data.role, name: data.name, branchId: data.branchId, email };
      localStorage.setItem(USER_KEY, JSON.stringify(u));
      setUser(u);
      return { ok: true };
    } catch (e) {
      return { ok: false, error: toAppError(e) };
    }
  }, []);

  const value = useMemo(() => ({
    user,
    role: user?.role,
    isAuthenticated: !!user && !!tokenStore.get(),
    ready,
    login,
    logout,
  }), [user, ready, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
