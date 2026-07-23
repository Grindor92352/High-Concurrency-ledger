import { useCallback, useEffect, useRef, useState } from "react";
import { useAuth } from "./useAuth";

const POLL_INTERVAL_MS = 4000;
const PAGE_SIZE = 25;

/**
 * Drives the "Live Stream Ledger" panel. Polls the paginated history
 * endpoint on an interval and diffs the result against what's already
 * rendered, tagging any transaction id we haven't seen yet as `isNew` so
 * LiveLedgerTicker can play its fade-in/flash animation only on genuinely
 * new rows rather than re-flashing the whole list every tick.
 */
export function useLedger(accountId) {
  const { authFetch, isAuthenticated } = useAuth();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const seenIds = useRef(new Set());

  const poll = useCallback(async () => {
    if (!accountId || !isAuthenticated) return;
    try {
      const page = await authFetch(`/api/transactions/account/${accountId}?page=0&size=${PAGE_SIZE}`);
      const withFlags = page.content.map((tx) => ({
        ...tx,
        isNew: !seenIds.current.has(tx.id),
      }));
      withFlags.forEach((tx) => seenIds.current.add(tx.id));
      setRows(withFlags);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [accountId, authFetch, isAuthenticated]);

  useEffect(() => {
    seenIds.current = new Set();
    setLoading(true);
    poll();
    const interval = setInterval(poll, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [poll]);

  return { rows, loading, error, refresh: poll };
}
