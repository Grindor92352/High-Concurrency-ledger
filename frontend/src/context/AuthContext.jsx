import { createContext, useCallback, useEffect, useMemo, useState } from "react";
import { api, ApiError } from "../api/client";

export const AuthContext = createContext(null);
const REFRESH_TOKEN_KEY = "ledger.refreshToken";

// Helper to parse JWT payload for username and roles
function decodeJwt(token) {
  try {
    const payload = token.split(".")[1];
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(null);
  const [refreshToken, setRefreshToken] = useState(null);
  const [user, setUser] = useState(null);
  const [initializing, setInitializing] = useState(true);

  const applyTokens = useCallback((auth) => {
    setAccessToken(auth.accessToken);
    setRefreshToken(auth.refreshToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken);
    const claims = decodeJwt(auth.accessToken);
    setUser(claims ? { username: claims.sub, roles: claims.roles || [] } : null);
  }, []);

  const clearAuth = useCallback(() => {
    setAccessToken(null);
    setRefreshToken(null);
    setUser(null);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }, []);

  // Try auto-login if saved refresh token exists in localStorage
  useEffect(() => {
    let cancelled = false;
    const stored = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (!stored) {
      setInitializing(false);
      return;
    }
    (async () => {
      try {
        const auth = await api.refresh(stored);
        if (!cancelled) applyTokens(auth);
      } catch {
        if (!cancelled) clearAuth();
      } finally {
        if (!cancelled) setInitializing(false);
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = useCallback(
    async (username, password) => {
      const auth = await api.login(username, password);
      applyTokens(auth);
    },
    [applyTokens]
  );

  const register = useCallback(
    async (username, password) => {
      const auth = await api.register(username, password);
      applyTokens(auth);
    },
    [applyTokens]
  );

  const logout = useCallback(async () => {
    const token = refreshToken;
    clearAuth();
    if (token) {
      try {
        await api.logout(token);
      } catch {
        // Silently catch error on logout
      }
    }
  }, [refreshToken, clearAuth]);

  // Wrapper for authenticated API calls. Retries once if access token expired (401) using refresh token.
  const authFetch = useCallback(
    async (path, options = {}) => {
      try {
        return await api.request(path, { ...options, token: accessToken });
      } catch (err) {
        if (err instanceof ApiError && err.status === 401 && refreshToken) {
          const auth = await api.refresh(refreshToken);
          applyTokens(auth);
          return api.request(path, { ...options, token: auth.accessToken });
        }
        throw err;
      }
    },
    [accessToken, refreshToken, applyTokens]
  );

  const value = useMemo(
    () => ({
      user,
      initializing,
      isAuthenticated: !!accessToken,
      isAdmin: !!user?.roles?.some((r) => r.includes("ADMIN")),
      login,
      register,
      logout,
      authFetch,
    }),
    [user, accessToken, initializing, login, register, logout, authFetch]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

