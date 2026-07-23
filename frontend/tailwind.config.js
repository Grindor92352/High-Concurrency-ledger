/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        // Design tokens for the "HFT Terminal" theme.
        // Named deliberately after their role in the UI, not generic
        // "gray-800" style scales — see README design notes section.
        void: "#0A0D0F",       // page background — deep charcoal, not pure black
        panel: "#12161C",      // card/module background
        panel2: "#171D24",     // slightly raised panel (headers, active rows)
        hairline: "#232B35",   // 1px borders between modules
        dim: "#5B6572",        // muted labels, secondary captions
        ink: "#D7DEE5",        // primary text
        long: "#00D68F",       // credit / inbound / success — "long" position green
        short: "#FF3B5C",      // debit / outbound / rejection — "short" position red
        pulse: "#22D3EE",      // primary interactive accent — cyan terminal glow
        warn: "#FFB020",       // pending / lock-held state — amber
      },
      fontFamily: {
        mono: ["'JetBrains Mono'", "ui-monospace", "monospace"],
        sans: ["'Inter'", "ui-sans-serif", "system-ui", "sans-serif"],
      },
      keyframes: {
        tickerScroll: {
          "0%": { transform: "translateY(0)" },
          "100%": { transform: "translateY(-50%)" },
        },
        pulseRing: {
          "0%, 100%": { boxShadow: "0 0 0 0 rgba(34, 211, 238, 0.45)" },
          "50%": { boxShadow: "0 0 0 6px rgba(34, 211, 238, 0)" },
        },
        lockSnap: {
          "0%": { transform: "rotate(0deg) scale(1)" },
          "40%": { transform: "rotate(-6deg) scale(1.08)" },
          "60%": { transform: "rotate(4deg) scale(1.05)" },
          "100%": { transform: "rotate(0deg) scale(1)" },
        },
        flashGreen: {
          "0%": { backgroundColor: "rgba(0, 214, 143, 0.25)" },
          "100%": { backgroundColor: "rgba(0, 214, 143, 0)" },
        },
        flashRed: {
          "0%": { backgroundColor: "rgba(255, 59, 92, 0.25)" },
          "100%": { backgroundColor: "rgba(255, 59, 92, 0)" },
        },
        fadeInRow: {
          "0%": { opacity: 0, transform: "translateY(-4px)" },
          "100%": { opacity: 1, transform: "translateY(0)" },
        },
      },
      animation: {
        "ticker-scroll": "tickerScroll 18s linear infinite",
        "pulse-ring": "pulseRing 1.4s ease-out infinite",
        "lock-snap": "lockSnap 0.45s cubic-bezier(.36,1.7,.4,1)",
        "flash-green": "flashGreen 1.1s ease-out",
        "flash-red": "flashRed 1.1s ease-out",
        "fade-in-row": "fadeInRow 0.25s ease-out",
      },
    },
  },
  plugins: [],
}
