import { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react';
import { authApi } from '../api/services.js';
import { tokenStore, setUnauthorizedHandler, toAppError, doRefresh } from '../api/client.js';

const AuthContext = createContext(null);

/** Best-effort decode of the JWT payload (for the email claim only — never trusted for auth). */
function jwtClaims(token) {
  try { return JSON.parse(atob(token.split('.')[1])); } catch { return {}; }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);

  const clearSession = useCallback(() => { tokenStore.clear(); setUser(null); }, []);

  const logout = useCallback(() => {
    // Server revokes the session and clears the httpOnly cookie; then clear the in-memory token.
    authApi.logout().catch(() => {});
    clearSession();
  }, [clearSession]);

  // Any unrecoverable 401 from the API layer bounces the session.
  useEffect(() => { setUnauthorizedHandler(() => clearSession()); }, [clearSession]);

  // Bootstrap: the in-memory access token is gone after a reload, but the httpOnly refresh cookie
  // may still be valid — try one silent refresh to transparently restore the session.
  useEffect(() => {
    let alive = true;
    doRefresh()
      .then((data) => {
        if (!alive) return;
        setUser({ userId: data.userId, role: data.role, name: data.name,
          branchId: data.branchId, email: jwtClaims(data.token).email });
      })
      .catch(() => { if (alive) clearSession(); })
      .finally(() => { if (alive) setReady(true); });
    return () => { alive = false; };
  }, [clearSession]);

  const login = useCallback(async (email, password) => {
    try {
      const { data } = await authApi.login(email, password);
      tokenStore.set(data.token); // refresh token is set as an httpOnly cookie by the server
      setUser({ userId: data.userId, role: data.role, name: data.name, branchId: data.branchId, email });
      return { ok: true };
    } catch (e) {
      return { ok: false, error: toAppError(e) };
    }
  }, []);

  const value = useMemo(() => ({
    user,
    role: user?.role,
    isAuthenticated: !!user,
    ready,
    login,
    logout,
  }), [user, ready, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
