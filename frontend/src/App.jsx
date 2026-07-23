import { AuthProvider } from "./context/AuthContext";
import { useAuth } from "./hooks/useAuth";
import AuthGate from "./components/auth/AuthGate";
import TerminalPage from "./pages/TerminalPage";

function Gate() {
  const { isAuthenticated, initializing } = useAuth();

  if (initializing) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <span className="font-sans text-xs font-semibold tracking-wider uppercase text-dim">AUTHENTICATING SECURE SESSION…</span>
      </div>
    );
  }

  return isAuthenticated ? <TerminalPage /> : <AuthGate />;
}

export default function App() {
  return (
    <AuthProvider>
      <Gate />
    </AuthProvider>
  );
}
