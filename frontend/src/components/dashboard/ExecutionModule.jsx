import { useState } from "react";
import { SendHorizontal } from "lucide-react";
import { useAuth } from "../../hooks/useAuth";
import { ApiError } from "../../api/client";
import LockVisualizer from "./LockVisualizer";

export default function ExecutionModule({ activeAccount, onSettled }) {
  const { authFetch } = useAuth();
  const [destinationId, setDestinationId] = useState("");
  const [quantity, setQuantity] = useState("");
  const [lockState, setLockState] = useState("idle");
  const [message, setMessage] = useState(null);

  const disabled = !activeAccount || lockState === "locking";

  async function handleExecute(e) {
    e.preventDefault();
    if (!activeAccount || !destinationId || !quantity) return;

    setMessage(null);
    setLockState("locking"); // the backend is about to enter findByIdForUpdate for both rows

    const idempotencyKey =
      typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;

    try {
      await authFetch("/api/transactions/transfer", {
        method: "POST",
        body: {
          sourceAccountId: activeAccount.id,
          destinationAccountId: Number(destinationId),
          amount: Number(quantity),
          idempotencyKey,
        },
      });
      setLockState("success");
      setMessage({ tone: "long", text: `SUCCESS — $${Number(quantity).toFixed(2)} transferred to Account #${destinationId}` });
      setDestinationId("");
      setQuantity("");
      onSettled?.();
    } catch (err) {
      setLockState("error");
      const text = err instanceof ApiError ? err.message : "Transfer failed — network error.";
      setMessage({ tone: "short", text });
    } finally {
      setTimeout(() => setLockState("idle"), 1300);
    }
  }

  return (
    <div className="panel flex flex-col">
      <div className="flex items-center justify-between border-b border-hairline/80 px-4 py-3.5 bg-panel2/40">
        <div className="flex items-center gap-2">
          <SendHorizontal size={16} className="text-pulse" />
          <span className="panel-label">Fund Transfer</span>
        </div>
      </div>

      <form onSubmit={handleExecute} className="flex flex-1 flex-col gap-3.5 p-4">
        <div>
          <label className="mb-1 block font-sans text-xs font-medium text-dim">
            Recipient Account ID
          </label>
          <input
            type="number"
            min="1"
            required
            value={destinationId}
            onChange={(e) => setDestinationId(e.target.value)}
            placeholder="e.g. 2"
            className="input-terminal"
          />
        </div>

        <div>
          <label className="mb-1 block font-sans text-xs font-medium text-dim">
            Transfer Amount ($)
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

        <LockVisualizer state={lockState} lockingDetail="Locking both accounts (asc. id order)" />

        {message && (
          <div className={`font-sans text-xs font-medium ${message.tone === "long" ? "text-long" : "text-short"}`}>
            {message.text}
          </div>
        )}

        <button type="submit" disabled={disabled} className="btn-execute mt-auto">
          Transfer Funds
        </button>
      </form>
    </div>
  );
}
