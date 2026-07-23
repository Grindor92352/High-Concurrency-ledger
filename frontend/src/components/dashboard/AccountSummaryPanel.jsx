import { Wallet, Plus, ChevronRight, CreditCard } from "lucide-react";
import GlowNumber from "../common/GlowNumber";

export default function AccountSummaryPanel({
  accounts,
  activeAccountId,
  onSelect,
  onCreateAccount,
  loading,
  creating,
}) {
  return (
    <div className="panel flex flex-col">
      <div className="flex items-center justify-between border-b border-hairline/80 px-4 py-3.5 bg-panel2/40">
        <div className="flex items-center gap-2">
          <Wallet size={16} className="text-pulse" />
          <span className="panel-label">My Accounts</span>
        </div>
        <button
          onClick={onCreateAccount}
          disabled={creating}
          className="flex items-center gap-1.5 font-sans text-xs font-semibold text-pulse hover:text-pulse/80 transition-colors disabled:opacity-40"
        >
          <Plus size={14} /> Open Account
        </button>
      </div>

      <div className="max-h-[320px] flex-1 divide-y divide-hairline/60 overflow-y-auto">
        {loading && <div className="px-4 py-8 text-center font-sans text-xs text-dim">Loading your accounts…</div>}

        {!loading && accounts.length === 0 && (
          <div className="flex flex-col items-center gap-4 px-6 py-10 text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-pulse/10 text-pulse">
              <CreditCard size={24} />
            </div>
            <div>
              <h4 className="font-sans font-semibold text-sm text-ink">No Bank Accounts Opened</h4>
              <p className="mt-1 font-sans text-xs text-dim leading-relaxed">
                No active bank accounts found. Open your first account to start managing and transferring funds securely.
              </p>
            </div>
            <button onClick={onCreateAccount} disabled={creating} className="btn-execute w-full mt-1">
              {creating ? "Opening Account…" : "Open Account Now"}
            </button>
          </div>
        )}

        {accounts.map((acc) => {
          const isActive = acc.id === activeAccountId;
          return (
            <button
              key={acc.id}
              onClick={() => onSelect(acc.id)}
              className={`flex w-full items-center justify-between px-4 py-3.5 text-left transition-colors ${
                isActive ? "bg-pulse/[0.08] border-l-2 border-pulse" : "hover:bg-panel2/60"
              }`}
            >
              <div>
                <div className="flex items-center gap-2">
                  <span className={`font-sans text-xs font-medium ${isActive ? "text-pulse" : "text-dim"}`}>
                    Account #{acc.id}
                  </span>
                  <span className="font-mono text-xs text-dim/80">{acc.accountNumber}</span>
                </div>
                <div className="mt-1">
                  <GlowNumber value={acc.balance} tone="neutral" size="text-lg" prefix="$" />
                </div>
              </div>
              <ChevronRight size={16} className={isActive ? "text-pulse" : "text-dim/60"} />
            </button>
          );
        })}
      </div>
    </div>
  );
}
