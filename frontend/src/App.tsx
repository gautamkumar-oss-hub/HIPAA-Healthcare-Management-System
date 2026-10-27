import { useAuth } from "./auth/AuthContext";
import { LoginPage } from "./components/LoginPage";
import { PatientDashboard } from "./components/PatientDashboard";
import { AdminDashboard, DoctorDashboard } from "./components/StaffDashboards";
import { Assistant } from "./components/Assistant";

const roleLabels = {
  PATIENT: "Patient portal",
  DOCTOR: "Clinician workspace",
  ADMIN: "Operations",
};

export default function App() {
  const { session, signOut } = useAuth();
  if (!session) return <LoginPage />;

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-lockup"><div className="brand-mark">+</div><span>Careflow</span></div>
        <div className="nav-section-label">WORKSPACE</div>
        <div className="nav-item active"><span>◫</span>{roleLabels[session.role]}</div>
        <div className="sidebar-spacer" />
        <div className="privacy-note"><span className="privacy-icon">◈</span>
          <strong>Private by design</strong><p>Your information is protected and access is audited.</p>
        </div>
        <div className="sidebar-user">
          <div className="profile-avatar small">{session.role[0]}</div>
          <div><strong>{session.role[0] + session.role.slice(1).toLowerCase()}</strong><span>Secure session</span></div>
          <button className="icon-button logout-button"
            onClick={() => { void signOut().catch(() => undefined); }}
            aria-label="Sign out">↗</button>
        </div>
      </aside>
      <main className="main-content">
        <header className="topbar"><span className="topbar-label">{roleLabels[session.role]}</span>
          <span className="session-indicator"><i /> Secure session</span></header>
        <div className="dashboard-content">
          {session.role === "PATIENT" && <PatientDashboard />}
          {session.role === "DOCTOR" && <DoctorDashboard />}
          {session.role === "ADMIN" && <AdminDashboard />}
        </div>
      </main>
      <Assistant />
    </div>
  );
}
