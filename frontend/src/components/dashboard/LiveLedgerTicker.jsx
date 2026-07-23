import { Activity } from "lucide-react";
import StatusPill from "../common/StatusPill";
import GlowNumber from "../common/GlowNumber";

function formatTime(iso) {
  return new Date(iso).toLocaleTimeString("en-US", { hour12: false });
}

function rowTone(tx) {
  if (tx.transactionType === "DEPOSIT") return "long";
  if (tx.transactionType === "WITHDRAWAL") return "short";
  return "neutral";
}

export default function LiveLedgerTicker({ rows, loading, error }) {
  return (
    <div className="panel flex flex-col">
      <div className="flex items-center justify-between border-b border-hairline/80 px-4 py-3.5 bg-panel2/40">
        <div className="flex items-center gap-2">
          <Activity size={16} className="text-pulse" />
          <span className="panel-label">Recent Activity & Ledger</span>
        </div>
        <StatusPill tone="live" dot="pulse">
          Live Activity
        </StatusPill>
      </div>

      <div className="grid grid-cols-[80px_1fr_1.2fr_auto] gap-2 border-b border-hairline/80 bg-panel2/20 px-4 py-2.5 font-sans text-xs font-semibold text-dim uppercase tracking-wider">
        <span>Time</span>
        <span>Type</span>
        <span>From → To</span>
        <span className="text-right">Amount</span>
      </div>

      <div className="max-h-[380px] flex-1 overflow-y-auto divide-y divide-hairline/40">
        {loading && rows.length === 0 && (
          <div className="px-4 py-8 text-center font-sans text-xs text-dim">Loading transaction history…</div>
        )}

        {error && <div className="px-4 py-8 text-center font-sans text-xs text-short">{error}</div>}

        {!loading && !error && rows.length === 0 && (
          <div className="px-4 py-8 text-center font-sans text-xs text-dim">
            No transaction history recorded for this account.
          </div>
        )}

        {rows.map((tx) => {
          const tone = rowTone(tx);
          const sign = tone === "short" ? "−" : tone === "long" ? "+" : "";

          return (
            <div
              key={tx.id}
              className={`grid grid-cols-[80px_1fr_1.2fr_auto] items-center gap-2 px-4 py-3 text-xs transition-colors hover:bg-panel2/40 ${
                tx.isNew ? `animate-fade-in-row ${tone === "short" ? "animate-flash-red" : "animate-flash-green"}` : ""
              }`}
            >
              <span className="font-mono text-dim">{formatTime(tx.timestamp)}</span>
              <span className="font-sans font-medium text-ink">{tx.transactionType}</span>
              <span className="truncate font-mono text-[11px] text-dim">
                {tx.sourceAccountNumber || "External"} → {tx.destinationAccountNumber || "External"}
              </span>
              <span className="text-right">
                <GlowNumber value={tx.amount} tone={tone} size="text-sm" prefix={sign ? `${sign}$` : "$"} />
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
