import { Lock, LockOpen, ShieldCheck, ShieldX } from "lucide-react";

export default function LockVisualizer({ state = "idle", lockingDetail }) {
  const config = {
    idle: {
      icon: LockOpen,
      ring: "",
      color: "text-dim",
      label: "SECURITY ENGINE READY",
      sub: "Standing by for transaction request",
    },
    locking: {
      icon: Lock,
      ring: "animate-pulse-ring",
      color: "text-warn animate-lock-snap",
      label: "ACQUIRING PESSIMISTIC LOCK",
      sub: lockingDetail || "Locking database rows (SELECT … FOR UPDATE)",
    },
    success: {
      icon: ShieldCheck,
      ring: "",
      color: "text-long",
      label: "TRANSACTION SECURED",
      sub: "Atomic lock released & committed safely",
    },
    error: {
      icon: ShieldX,
      ring: "",
      color: "text-short",
      label: "TRANSACTION REJECTED",
      sub: "Rolled back safely (Insufficient funds / invalid)",
    },
  }[state];

  const Icon = config.icon;

  return (
    <div className="flex items-center gap-3 rounded-lg border border-hairline/80 bg-void/80 px-3.5 py-2.5">
      <div className={`relative flex h-8 w-8 items-center justify-center rounded-full border border-current/30 ${config.ring} ${config.color}`}>
        <Icon size={16} strokeWidth={2} />
      </div>
      <div className="leading-tight">
        <div className={`font-sans text-xs font-semibold tracking-wide ${config.color}`}>{config.label}</div>
        <div className="font-sans text-[11px] text-dim">{config.sub}</div>
      </div>
    </div>
  );
}
