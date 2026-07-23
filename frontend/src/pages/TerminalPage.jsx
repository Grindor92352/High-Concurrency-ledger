import { useAuth } from "../hooks/useAuth";
import { useAccounts } from "../hooks/useAccounts";
import { useLedger } from "../hooks/useLedger";
import TerminalHeader from "../components/layout/TerminalHeader";
import AccountSummaryPanel from "../components/dashboard/AccountSummaryPanel";
import ExecutionModule from "../components/dashboard/ExecutionModule";
import DepositWithdrawModule from "../components/dashboard/DepositWithdrawModule";
import LiveLedgerTicker from "../components/dashboard/LiveLedgerTicker";
import AdminPanel from "../components/dashboard/AdminPanel";
import { useState } from "react";

export default function TerminalPage() {
  const { isAdmin } = useAuth();
  const { accounts, activeAccount, activeAccountId, setActiveAccountId, loading, createAccount, refresh } =
    useAccounts();
  const [creating, setCreating] = useState(false);
  const { rows, loading: ledgerLoading, error: ledgerError, refresh: refreshLedger } = useLedger(activeAccountId);

  async function handleCreateAccount() {
    setCreating(true);
    try {
      await createAccount();
    } finally {
      setCreating(false);
    }
  }

  function handleSettled() {
    refresh();
    refreshLedger();
  }

  return (
    <div className="min-h-screen">
      <TerminalHeader />

      <main className="mx-auto max-w-[1400px] p-5">
        {!loading && accounts.length === 0 ? (
          <div className="mt-16 flex justify-center">
            <div className="w-full max-w-md">
              <AccountSummaryPanel
                accounts={accounts}
                activeAccountId={activeAccountId}
                onSelect={setActiveAccountId}
                onCreateAccount={handleCreateAccount}
                loading={loading}
                creating={creating}
              />
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
            <div className="flex flex-col gap-4 lg:col-span-3">
              <AccountSummaryPanel
                accounts={accounts}
                activeAccountId={activeAccountId}
                onSelect={setActiveAccountId}
                onCreateAccount={handleCreateAccount}
                loading={loading}
                creating={creating}
              />
            </div>

            <div className="flex flex-col gap-4 lg:col-span-4">
              <ExecutionModule activeAccount={activeAccount} onSettled={handleSettled} />
              <DepositWithdrawModule activeAccount={activeAccount} onSettled={handleSettled} />
            </div>

            <div className="lg:col-span-5">
              <LiveLedgerTicker rows={rows} loading={ledgerLoading} error={ledgerError} />
            </div>

            {isAdmin && (
              <div className="lg:col-span-12">
                <AdminPanel />
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}
