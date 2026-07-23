import { useState } from "react";
import { Landmark, Lock } from "lucide-react";
import { useAuth } from "../../hooks/useAuth";
import { ApiError } from "../../api/client";

export default function AuthGate() {
  const { login, register } = useAuth();
  const [mode, setMode] = useState("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      if (mode === "login") {
        await login(username, password);
      } else {
        await register(username, password);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Connection to banking service failed.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4 bg-void">
      <div className="panel w-full max-w-md shadow-2xl shadow-black/40 border border-hairline/80">
        <div className="flex flex-col items-center border-b border-hairline/80 px-6 py-6 text-center bg-panel2/40">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-pulse/15 text-pulse mb-3">
            <Landmark size={24} />
          </div>
          <h2 className="font-sans text-xl font-bold tracking-tight text-ink">
            APEX <span className="text-pulse">FINANCIAL</span>
          </h2>
          <p className="font-sans text-xs text-dim mt-1">Digital Banking & Financial Services Portal</p>
        </div>

        <div className="flex border-b border-hairline/80 font-sans text-xs font-semibold">
          <button
            onClick={() => setMode("login")}
            className={`flex-1 py-3 transition-all border-b-2 ${
              mode === "login" ? "border-pulse text-pulse bg-pulse/[0.06]" : "border-transparent text-dim hover:text-ink"
            }`}
          >
            Sign In
          </button>
          <button
            onClick={() => setMode("register")}
            className={`flex-1 py-3 transition-all border-b-2 ${
              mode === "register" ? "border-pulse text-pulse bg-pulse/[0.06]" : "border-transparent text-dim hover:text-ink"
            }`}
          >
            Register Account
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4 p-6">
          <div className="flex items-center gap-1.5 font-sans text-xs font-medium text-dim">
            <Lock size={14} className="text-pulse" />
            {mode === "login" ? "Sign in with your credentials" : "Create your customer account"}
          </div>

          <div>
            <label className="mb-1 block font-sans text-xs text-dim font-medium">Username</label>
            <input
              type="text"
              required
              autoFocus
              placeholder="Enter your username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="input-terminal"
            />
          </div>

          <div>
            <label className="mb-1 block font-sans text-xs text-dim font-medium">Password</label>
            <input
              type="password"
              required
              minLength={mode === "register" ? 8 : undefined}
              placeholder={mode === "register" ? "At least 8 characters" : "Enter your password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="input-terminal"
            />
          </div>

          {error && <div className="font-sans text-xs font-medium text-short bg-short/10 p-2.5 rounded-lg border border-short/30">{error}</div>}

          <button type="submit" disabled={submitting} className="btn-execute mt-2 py-3 text-sm">
            {submitting ? "Signing In…" : mode === "login" ? "Sign In to Portal" : "Register Account"}
          </button>

          {mode === "register" && (
            <p className="font-sans text-xs leading-relaxed text-dim/80 text-center">
              New customer accounts are automatically provisioned with standard banking features.
            </p>
          )}
        </form>
      </div>
    </div>
  );
}
