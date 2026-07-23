# Apex Financial — React Frontend Portal

A modern, high-performance web client for the Apex Financial Core Banking Engine built with **React 18**, **Vite**, and **Tailwind CSS**.

The portal provides an intuitive digital banking dashboard for customers and enterprise administrators while visualizing the backend's real-time transaction processing and pessimistic database locking mechanics.

---

## Features

1. **My Accounts Dashboard** (`AccountSummaryPanel.jsx`) — Displays customer bank accounts with dynamic dollar balance calculation, account numbers, and one-click new account creation.
2. **Fund Transfer Engine** (`ExecutionModule.jsx`) — Execute secure transfers between accounts with client-side idempotency key (`UUID`) generation to prevent duplicate transaction submissions during retries.
3. **Deposits & Withdrawals** (`DepositWithdrawModule.jsx`) — Instantly credit or debit account funds.
4. **Real-Time Security Visualizer** (`LockVisualizer.jsx`) — A live visual indicator (`idle` → `locking` → `success` / `error`) driven directly by the fetch lifecycle of backend transactions, displaying the exact database locking status (`SELECT ... FOR UPDATE`).
5. **Live Activity Stream** (`LiveLedgerTicker.jsx`) — Real-time transaction polling ticker (`GET /api/transactions/account/{id}`) diffing new ledger entries and highlighting inbound credit / outbound debit movements.
6. **Enterprise Admin Control Center** (`AdminPanel.jsx`) — Restricted portal for administrators to audit system-wide accounts and provision new users with custom role assignments (`CUSTOMER` or `ADMIN`).

---

## Technical Stack & Design System

| Token | Color Hex | Used For |
|---|---|---|
| `void` | `#0A0D0F` | Main page background |
| `panel` / `panel2` | `#12161C` / `#171D24` | Modern glassmorphism card backgrounds |
| `hairline` | `#232B35` | Card borders and divider lines |
| `ink` | `#D7DEE5` | Primary headings and text |
| `dim` | `#5B6572` | Secondary labels and microcopy |
| `long` | `#00D68F` | Deposit / credit green accent |
| `short` | `#FF3B5C` | Withdrawal / debit red accent |
| `pulse` | `#22D3EE` | Primary interactive cyan accent |
| `warn` | `#FFB020` | Lock-held pending state / amber warning |

- **Typography**: Inter for prose and UI layout; JetBrains Mono for numeric values, balances, and account IDs (using `font-variant-numeric: tabular-nums`).

---

## Running Locally

```bash
# Copy environment configuration
cp .env.example .env

# Install dependencies
npm install

# Start Vite dev server
npm run dev
```

- Dev server opens on `http://localhost:5173`.
- Make sure the Spring Boot backend server is running on `http://localhost:8080`.
