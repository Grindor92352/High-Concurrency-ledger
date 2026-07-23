import { useEffect, useState } from "react";
import { ShieldCheck, UserPlus } from "lucide-react";
import { useAuth } from "../../hooks/useAuth";
import { ApiError } from "../../api/client";
import GlowNumber from "../common/GlowNumber";
import StatusPill from "../common/StatusPill";

export default function AdminPanel() {
  const { authFetch } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("CUSTOMER");
  const [creating, setCreating] = useState(false);
  const [createMessage, setCreateMessage] = useState(null);

  async function loadAccounts() {
    setLoading(true);
    try {
      const data = await authFetch("/api/admin/accounts");
      setAccounts(data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAccounts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleCreateUser(e) {
    e.preventDefault();
    setCreating(true);
    setCreateMessage(null);
    try {
      await authFetch("/api/admin/users", { method: "POST", body: { username, password, role } });
      setCreateMessage({ tone: "long", text: `User "${username}" created as ${role}.` });
      setUsername("");
      setPassword("");
      setRole("CUSTOMER");
    } catch (err) {
      const text = err instanceof ApiError ? err.message : "Failed to create user.";
      setCreateMessage({ tone: "short", text });
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="panel flex flex-col">
      <div className="flex items-center justify-between border-b border-hairline/80 px-4 py-3.5 bg-panel2/40">
        <div className="flex items-center gap-2">
          <ShieldCheck size={16} className="text-warn" />
          <span className="panel-label">Enterprise Admin Control Center</span>
        </div>
        <StatusPill tone="warn">Admin Restricted</StatusPill>
      </div>

      <div className="grid grid-cols-1 divide-y divide-hairline/80 md:grid-cols-2 md:divide-x md:divide-y-0">
        <div className="max-h-[300px] overflow-y-auto">
          <div className="grid grid-cols-[60px_1fr_1fr_auto] gap-2 border-b border-hairline/80 bg-panel2/20 px-4 py-2.5 font-sans text-xs font-semibold text-dim uppercase tracking-wider">
            <span>ID</span>
            <span>Account</span>
            <span>Owner</span>
            <span className="text-right">Balance</span>
          </div>
          {loading && <div className="px-4 py-6 font-sans text-xs text-dim">Loading system accounts…</div>}
          {error && <div className="px-4 py-6 font-sans text-xs text-short">{error}</div>}
          {!loading &&
            !error &&
            accounts.map((a) => (
              <div
                key={a.id}
                className="grid grid-cols-[60px_1fr_1fr_auto] items-center gap-2 border-b border-hairline/40 px-4 py-2.5 hover:bg-panel2/40"
              >
                <span className="font-sans text-xs text-dim">#{a.id}</span>
                <span className="truncate font-mono text-xs text-ink">{a.accountNumber}</span>
                <span className="truncate font-sans text-xs text-dim">{a.ownerUsername}</span>
                <span className="text-right">
                  <GlowNumber value={a.balance} tone="neutral" size="text-xs" prefix="$" />
                </span>
              </div>
            ))}
        </div>

        <form onSubmit={handleCreateUser} className="flex flex-col gap-3.5 p-4">
          <div className="flex items-center gap-2 font-sans text-xs font-semibold text-ink">
            <UserPlus size={14} className="text-pulse" /> Provision New User
          </div>
          <input
            type="text"
            placeholder="Username"
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="input-terminal"
          />
          <input
            type="password"
            placeholder="Password (min 8 chars)"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="input-terminal"
          />
          <select value={role} onChange={(e) => setRole(e.target.value)} className="input-terminal">
            <option value="CUSTOMER font-sans">CUSTOMER ACCOUNT</option>
            <option value="ADMIN font-sans">ADMINISTRATOR</option>
          </select>
          {createMessage && (
            <div className={`font-sans text-xs font-medium ${createMessage.tone === "long" ? "text-long" : "text-short"}`}>
              {createMessage.text}
            </div>
          )}
          <button type="submit" disabled={creating} className="btn-execute">
            {creating ? "Creating User…" : "Create User"}
          </button>
        </form>
      </div>
    </div>
  );
}
