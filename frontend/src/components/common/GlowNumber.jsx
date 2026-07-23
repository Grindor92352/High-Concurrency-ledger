function formatAmount(value) {
  const n = typeof value === "string" ? parseFloat(value) : value;
  return n.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/**
 * tone: "long" (green, credit/positive), "short" (red, debit/negative),
 * or "neutral" (no color, e.g. a balance that could be either).
 */
export default function GlowNumber({ value, tone = "neutral", prefix = "", size = "text-2xl" }) {
  const toneClass =
    tone === "long" ? "text-long glow-long" : tone === "short" ? "text-short glow-short" : "text-ink";

  return (
    <span className={`font-mono tabular ${size} ${toneClass}`}>
      {prefix}
      {formatAmount(value)}
    </span>
  );
}
