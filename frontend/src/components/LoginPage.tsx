import { useState, type FormEvent } from "react";
import { ApiError, request } from "../api/client";
import { useAuth } from "../auth/AuthContext";

type Mode = "login" | "register" | "enroll";

export function LoginPage() {
  const { signIn, register, logoutError } = useAuth();
  const [mode, setMode] = useState<Mode>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mfaCode, setMfaCode] = useState("");
  const [provisioningUri, setProvisioningUri] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      if (mode === "register") {
        await register(email, password);
      } else if (mode === "enroll") {
        const setup = await request<{ provisioningUri: string }>("/auth/mfa/enroll", undefined, {
          method: "POST",
          body: JSON.stringify({ email, password }),
        });
        setProvisioningUri(setup.provisioningUri);
        setError("Scan or enter the URI in an authenticator app, then verify the 6-digit code.");
      } else {
        await signIn(email, password, mfaCode);
      }
    } catch (cause) {
      if (cause instanceof ApiError && cause.message.includes("MFA enrollment required")) {
        setMode("enroll");
        setError("Staff accounts must enroll an authenticator before signing in.");
      } else {
        setError(cause instanceof Error ? cause.message : "Something went wrong.");
      }
    } finally {
      setBusy(false);
    }
  }

  async function verifyMfa() {
    setBusy(true);
    setError("");
    try {
      await request<void>("/auth/mfa/verify", undefined, {
        method: "POST",
        body: JSON.stringify({ email, password, code: mfaCode }),
      });
      await signIn(email, password, mfaCode);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "MFA verification failed.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-panel">
        <div className="brand-lockup">
          <div className="brand-mark">+</div>
          <span>Careflow</span>
        </div>
        <div className="auth-copy">
          <span className="eyebrow">YOUR CARE, IN SYNC</span>
          <h1>Healthcare that keeps you moving.</h1>
          <p>Appointments, care teams, and the details that matter — together in one place.</p>
        </div>
        <div className="auth-illustration" aria-hidden="true">
          <div className="orbit orbit-one" />
          <div className="orbit orbit-two" />
          <div className="care-card">
            <span className="care-card-icon">✳</span>
            <span>Thoughtful care<br />starts here</span>
          </div>
        </div>
        <p className="auth-footnote">Private by design · Built for better care</p>
      </section>

      <section className="auth-form-wrap">
        <form className="auth-form" onSubmit={submit}>
          <span className="eyebrow">{mode === "register" ? "GET STARTED" : "WELCOME BACK"}</span>
          <h2>{mode === "register" ? "Create your account" : mode === "enroll" ? "Set up your authenticator" : "Sign in to Careflow"}</h2>
          <p className="muted">
            {mode === "register" ? "Your account starts with a secure patient profile." : "A calmer way to manage your healthcare."}
          </p>

          <label htmlFor="email">Email address</label>
          <input id="email" type="email" autoComplete="username" required value={email}
            onChange={(event) => setEmail(event.target.value)} />

          <label htmlFor="password">Password</label>
          <input id="password" type="password"
            autoComplete={mode === "login" ? "current-password" : "new-password"}
            minLength={12} required value={password}
            onChange={(event) => setPassword(event.target.value)} />

          {mode === "login" && (
            <div className="form-extra">
              <label htmlFor="mfaCode">Authenticator code <span className="muted">(staff accounts)</span></label>
              <input id="mfaCode" inputMode="numeric" autoComplete="one-time-code"
                maxLength={6} pattern="[0-9]{6}" value={mfaCode}
                onChange={(event) => setMfaCode(event.target.value)} />
            </div>
          )}

          {mode === "enroll" && (
            <>
              {provisioningUri && <code className="provisioning-uri">{provisioningUri}</code>}
              <label htmlFor="mfaCode">6-digit authenticator code</label>
              <input id="mfaCode" inputMode="numeric" autoComplete="one-time-code"
                maxLength={6} pattern="[0-9]{6}" required value={mfaCode}
                onChange={(event) => setMfaCode(event.target.value)} />
            </>
          )}

          {(error || logoutError) && <p className="form-message" role="alert">{error || logoutError}</p>}

          {mode === "enroll" ? (
            <button type="button" className="button button-primary" disabled={busy || mfaCode.length !== 6}
              onClick={() => void verifyMfa()}>
              {busy ? "Verifying..." : "Verify and sign in"}
            </button>
          ) : (
            <button type="submit" className="button button-primary" disabled={busy}>
              {busy ? "Please wait..." : mode === "register" ? "Create account" : "Continue"}
            </button>
          )}

          <div className="auth-switch">
            {mode === "register" ? "Already have an account?" : "New to Careflow?"}
            <button type="button" className="text-button"
              onClick={() => { setMode(mode === "register" ? "login" : "register"); setError(""); }}>
              {mode === "register" ? "Sign in" : "Create account"}
            </button>
          </div>
        </form>
      </section>
    </main>
  );
}
