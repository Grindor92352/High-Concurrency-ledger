import { useState } from "react";
import { Banknote } from "lucide-react";
import { useAuth } from "../../hooks/useAuth";
import { ApiError } from "../../api/client";
import LockVisualizer from "./LockVisualizer";

export default function DepositWithdrawModule({ activeAccount, onSettled }) {
  const { authFetch } = useAuth();
  const [mode, setMode] = useState("deposit"); // "deposit" | "withdraw"
  const [quantity, setQuantity] = useState("");
  const [lockState, setLockState] = useState("idle");
  const [message, setMessage] = useState(null);

  const disabled = !activeAccount || lockState === "locking";

  async function handleExecute(e) {
    e.preventDefault();
    if (!activeAccount || !quantity) return;

    setMessage(null);
    setLockState("locking");

    const idempotencyKey =
      typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;

    const path = mode === "deposit" ? "/api/transactions/deposit" : "/api/transactions/withdraw";

    try {
      await authFetch(path, {
        method: "POST",
        body: { accountId: activeAccount.id, amount: Number(quantity), idempotencyKey },
      });
      setLockState("success");
      setMessage({
        tone: "long",
        text: `SUCCESS — ${mode === "deposit" ? "Credited" : "Debited"} $${Number(quantity).toFixed(2)}`,
      });
      setQuantity("");
      onSettled?.();
    } catch (err) {
      setLockState("error");
      const text = err instanceof ApiError ? err.message : "Transaction failed — network error.";
      setMessage({ tone: "short", text });
    } finally {
      setTimeout(() => setLockState("idle"), 1300);
    }
  }

  return (
    <div className="panel flex flex-col">
      <div className="flex items-center justify-between border-b border-hairline/80 px-4 py-3.5 bg-panel2/40">
        <div className="flex items-center gap-2">
          <Banknote size={16} className="text-pulse" />
          <span className="panel-label">Deposit & Withdrawal</span>
        </div>
        <div className="flex rounded-lg overflow-hidden border border-hairline/80 font-sans text-xs font-semibold">
          <button
            type="button"
            onClick={() => setMode("deposit")}
            className={`px-3 py-1 transition-all ${mode === "deposit" ? "bg-long text-void" : "text-dim hover:text-ink"}`}
          >
            Deposit
          </button>
          <button
            type="button"
            onClick={() => setMode("withdraw")}
            className={`px-3 py-1 transition-all ${mode === "withdraw" ? "bg-short text-white" : "text-dim hover:text-ink"}`}
          >
            Withdraw
          </button>
        </div>
      </div>

      <form onSubmit={handleExecute} className="flex flex-1 flex-col gap-3.5 p-4">
        <div>
          <label className="mb-1 block font-sans text-xs font-medium text-dim">
            {mode === "deposit" ? "Deposit Amount ($)" : "Withdrawal Amount ($)"}
          </label>
          <input
            type="number"
            min="0.01"
            step="0.01"
            required
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            placeholder="0.00"
            className="input-terminal tabular"
          />
        </div>

        <LockVisualizer state={lockState} lockingDetail="Locking account row safely" />

        {message && (
          <div className={`font-sans text-xs font-medium ${message.tone === "long" ? "text-long" : "text-short"}`}>
            {message.text}
          </div>
        )}

        <button
          type="submit"
          disabled={disabled}
          className={`btn-execute mt-auto ${
            mode === "withdraw" ? "!bg-short !text-white hover:!bg-short/90" : ""
          }`}
        >
          {mode === "deposit" ? "Confirm Deposit" : "Confirm Withdrawal"}
        </button>
      </form>
    </div>
  );
}
