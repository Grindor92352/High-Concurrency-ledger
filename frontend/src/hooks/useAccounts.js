import { useCallback, useEffect, useState } from "react";
import { useAuth } from "./useAuth";

export function useAccounts() {
  const { authFetch, isAuthenticated } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [activeAccountId, setActiveAccountId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const refresh = useCallback(async () => {
    if (!isAuthenticated) return;
    setError(null);
    try {
      const data = await authFetch("/api/accounts/mine");
      setAccounts(data);
      setActiveAccountId((current) => {
        if (current && data.some((a) => a.id === current)) return current;
        return data[0]?.id ?? null;
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [authFetch, isAuthenticated]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const createAccount = useCallback(async () => {
    await authFetch("/api/accounts", { method: "POST" });
    await refresh();
  }, [authFetch, refresh]);

  const activeAccount = accounts.find((a) => a.id === activeAccountId) ?? null;

  return {
    accounts,
    activeAccount,
    activeAccountId,
    setActiveAccountId,
    loading,
    error,
    refresh,
    createAccount,
  };
}
