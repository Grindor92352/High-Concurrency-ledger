import { useEffect, useState } from "react";
import { Landmark, LogOut } from "lucide-react";
import { useAuth } from "../../hooks/useAuth";
import { API_BASE_URL } from "../../api/client";
import StatusPill from "../common/StatusPill";

export default function TerminalHeader() {
  const { user, isAdmin, logout } = useAuth();
  const [connected, setConnected] = useState(null); // null = checking, true/false after first check

  useEffect(() => {
    let cancelled = false;
    async function ping() {
      try {
        const res = await fetch(`${API_BASE_URL}/actuator/health`);
        if (!cancelled) setConnected(res.ok);
      } catch {
        if (!cancelled) setConnected(false);
      }
    }
    ping();
    const id = setInterval(ping, 15000);
    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, []);

  return (
    <header className="flex items-center justify-between border-b border-hairline/80 bg-panel/90 backdrop-blur-md px-6 py-3.5 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-pulse/15 text-pulse">
          <Landmark size={20} />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <span className="font-sans text-base font-bold tracking-tight text-ink">
              APEX <span className="text-pulse">FINANCIAL</span>
            </span>
            <StatusPill
              tone={connected === null ? "neutral" : connected ? "live" : "danger"}
              dot={connected ? "pulse" : false}
            >
              {connected === null ? "Connecting…" : connected ? "System Online" : "Network Offline"}
            </StatusPill>
          </div>
          <p className="text-[11px] text-dim font-sans hidden sm:block">Digital Banking & Financial Services</p>
        </div>
      </div>

      <div className="flex items-center gap-4">
        {user && (
          <div className="flex items-center gap-2.5 bg-void/60 px-3 py-1.5 rounded-lg border border-hairline/60">
            <span className="font-sans text-xs font-medium text-ink">{user.username}</span>
            <StatusPill tone={isAdmin ? "warn" : "pulse"}>{isAdmin ? "Administrator" : "Customer Account"}</StatusPill>
          </div>
        )}
        <button
          onClick={logout}
          className="flex items-center gap-2 rounded-lg border border-hairline/80 px-3 py-1.5 font-sans text-xs font-semibold text-dim transition-all hover:border-short/50 hover:bg-short/10 hover:text-short"
        >
          <LogOut size={14} /> Sign Out
        </button>
      </div>
    </header>
  );
}
