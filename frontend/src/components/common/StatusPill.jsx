const TONES = {
  live: "text-long border-long/40 bg-long/10",
  warn: "text-warn border-warn/40 bg-warn/10",
  danger: "text-short border-short/40 bg-short/10",
  neutral: "text-dim border-hairline bg-panel2",
  pulse: "text-pulse border-pulse/40 bg-pulse/10",
};

export default function StatusPill({ tone = "neutral", children, dot = false }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 border px-2 py-0.5 font-mono text-[10px] uppercase tracking-[0.14em] ${TONES[tone]}`}
    >
      {dot && <span className={`h-1.5 w-1.5 rounded-full ${dot === "pulse" ? "animate-pulse" : ""} bg-current`} />}
      {children}
    </span>
  );
}
