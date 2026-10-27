import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { Appointment, AuditPage, Role, StaffUser } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { CardHeading, EmptyState, ErrorText, FormField, Loading } from "./PatientDashboard";
import { formatTime } from "../utils";

export function DoctorDashboard() {
  const { api } = useAuth();
  const queryClient = useQueryClient();
  const schedule = useQuery({
    queryKey: ["my-appointments"],
    queryFn: () => api<Appointment[]>("/appointments/me"),
  });
  const createSlot = useMutation({
    mutationFn: (range: { startAt: string; endAt: string }) =>
      api<Appointment>("/appointments/availability", { method: "POST", body: JSON.stringify(range) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["my-appointments"] }),
  });
  const deleteSlot = useMutation({
    mutationFn: (id: string) => api<void>(`/appointments/${id}`, { method: "DELETE" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["my-appointments"] }),
  });
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");
  const booked = schedule.data?.filter((item) => item.status === "BOOKED") ?? [];
  const available = schedule.data?.filter((item) => item.status === "AVAILABLE") ?? [];

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    createSlot.mutate({ startAt: new Date(startAt).toISOString(), endAt: new Date(endAt).toISOString() }, {
      onSuccess: () => { setStartAt(""); setEndAt(""); },
    });
  }

  return (
    <>
      <section className="welcome-row">
        <div><span className="eyebrow">CLINICIAN WORKSPACE</span><h1>Your schedule, at a glance.</h1>
          <p className="muted">Manage availability and appointments assigned to you.</p></div>
        <div className="date-chip">{new Date().toLocaleDateString("en", { weekday: "long", month: "long", day: "numeric" })}</div>
      </section>
      <section className="stat-grid">
        <Stat label="Booked appointments" value={booked.length} icon="◷" />
        <Stat label="Open times" value={available.length} icon="＋" />
        <Stat label="Assigned patients" value={new Set(booked.map((item) => item.patientId)).size} icon="◉" />
      </section>
      <div className="dashboard-grid">
        <section className="surface-card">
          <CardHeading eyebrow="MY SCHEDULE" title="Appointments" />
          {schedule.isPending ? <Loading /> : schedule.error ? <ErrorText error={schedule.error} /> :
            schedule.data?.length ? <div className="appointment-list">
              {schedule.data.map((appointment) => (
                <article className="appointment-row" key={appointment.id}>
                  <div className="appointment-date">
                    <strong>{new Date(appointment.startAt).toLocaleDateString("en", { day: "2-digit" })}</strong>
                    <span>{new Date(appointment.startAt).toLocaleDateString("en", { month: "short" })}</span>
                  </div>
                  <div className="appointment-info"><strong>{appointment.status === "BOOKED" ? "Patient visit" : "Available slot"}</strong>
                    <span>{formatTime(appointment.startAt)} · {appointment.status === "BOOKED" ? `Patient ${appointment.patientId?.slice(0, 8)}` : "Open"}</span></div>
                  <span className={`status-pill ${appointment.status.toLowerCase()}`}>{appointment.status}</span>
                  {appointment.status === "AVAILABLE" && (
                    <button className="button button-quiet" onClick={() => deleteSlot.mutate(appointment.id)}>Remove</button>
                  )}
                </article>
              ))}
            </div> : <EmptyState title="Your schedule is clear" text="Add availability to let patients book a visit." />}
        </section>
        <section className="surface-card">
          <CardHeading eyebrow="OPEN YOUR CALENDAR" title="Publish availability" />
          <form className="stack-form" onSubmit={submit}>
            <FormField label="Start time" type="datetime-local" value={startAt} onChange={setStartAt} required />
            <FormField label="End time" type="datetime-local" value={endAt} onChange={setEndAt} required />
            {createSlot.error && <ErrorText error={createSlot.error} />}
            <button className="button button-primary" disabled={createSlot.isPending}>
              {createSlot.isPending ? "Publishing..." : "Publish time"}
            </button>
          </form>
        </section>
      </div>
    </>
  );
}

export function AdminDashboard() {
  const { api } = useAuth();
  const queryClient = useQueryClient();
  const users = useQuery({ queryKey: ["users"], queryFn: () => api<StaffUser[]>("/auth/users") });
  const schedule = useQuery({ queryKey: ["my-appointments"], queryFn: () => api<Appointment[]>("/appointments/me") });
  const audit = useQuery({ queryKey: ["audit"], queryFn: () => api<AuditPage>("/audit?page=0&size=8") });
  const createUser = useMutation({
    mutationFn: (input: { email: string; password: string; role: Role }) =>
      api<StaffUser>("/auth/users", { method: "POST", body: JSON.stringify(input) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["users"] }),
  });
  const cancel = useMutation({
    mutationFn: (id: string) => api<Appointment>(`/appointments/${id}/cancel`, { method: "POST" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["my-appointments"] }),
  });
  const createSlot = useMutation({
    mutationFn: (input: { doctorId: string; startAt: string; endAt: string }) =>
      api<Appointment>("/appointments/availability", { method: "POST", body: JSON.stringify(input) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["my-appointments"] }),
  });
  const deleteSlot = useMutation({
    mutationFn: (id: string) => api<void>(`/appointments/${id}`, { method: "DELETE" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["my-appointments"] }),
  });
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role>("DOCTOR");
  const [doctorId, setDoctorId] = useState("");
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");
  const doctors = users.data?.filter((user) => user.role === "DOCTOR") ?? [];

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    createUser.mutate({ email, password, role }, { onSuccess: () => { setEmail(""); setPassword(""); } });
  }

  function submitAvailability(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    createSlot.mutate({
      doctorId,
      startAt: new Date(startAt).toISOString(),
      endAt: new Date(endAt).toISOString(),
    }, { onSuccess: () => { setStartAt(""); setEndAt(""); } });
  }

  return (
    <>
      <section className="welcome-row">
        <div><span className="eyebrow">OPERATIONS</span><h1>Clinic overview.</h1>
          <p className="muted">People, schedules, and access activity — without clinical details.</p></div>
        <div className="date-chip">Administrator</div>
      </section>
      <section className="stat-grid">
        <Stat label="User accounts" value={users.data?.length ?? 0} icon="◉" />
        <Stat label="Scheduled slots" value={schedule.data?.length ?? 0} icon="◷" />
        <Stat label="Recent audit events" value={audit.data?.totalElements ?? 0} icon="◈" />
      </section>
      <div className="dashboard-grid">
        <section className="surface-card">
          <CardHeading eyebrow="ACCESS MANAGEMENT" title="User accounts" />
          {users.isPending ? <Loading /> : users.error ? <ErrorText error={users.error} /> :
            <div className="user-list">{users.data.map((user) => (
              <article className="user-row" key={user.id}>
                <div className="profile-avatar small">{user.email.slice(0, 1).toUpperCase()}</div>
                <div className="user-info"><strong>{user.email}</strong><span>{user.role}</span></div>
                <span className={`status-pill ${user.mfaEnabled ? "booked" : "available"}`}>
                  MFA {user.mfaEnabled ? "on" : "pending"}
                </span>
              </article>
            ))}</div>}
        </section>
        <section className="surface-card">
          <CardHeading eyebrow="INVITE A TEAM MEMBER" title="Create an account" />
          <form className="stack-form" onSubmit={submit}>
            <FormField label="Email address" type="email" value={email} onChange={setEmail} required />
            <FormField label="Temporary password" type="password" value={password} onChange={setPassword} required />
            <label className="form-field">Account role
              <select value={role} onChange={(event) => setRole(event.target.value as Role)}>
                <option value="DOCTOR">Doctor</option><option value="ADMIN">Admin</option><option value="PATIENT">Patient</option>
              </select>
            </label>
            {createUser.error && <ErrorText error={createUser.error} />}
            <button className="button button-primary" disabled={createUser.isPending}>
              {createUser.isPending ? "Creating..." : "Create account"}
            </button>
            <p className="muted form-hint">Doctors and administrators must enroll MFA at first sign-in.</p>
          </form>
        </section>
      </div>
      <section className="surface-card admin-availability">
        <CardHeading eyebrow="SCHEDULE MANAGEMENT" title="Publish doctor availability" />
        <form className="availability-form" onSubmit={submitAvailability}>
          <label className="form-field">Doctor account
            <select value={doctorId} onChange={(event) => setDoctorId(event.target.value)} required>
              <option value="">Select a doctor</option>
              {doctors.map((doctor) => <option value={doctor.id} key={doctor.id}>{doctor.email}</option>)}
            </select>
          </label>
          <FormField label="Start time" type="datetime-local" value={startAt} onChange={setStartAt} required />
          <FormField label="End time" type="datetime-local" value={endAt} onChange={setEndAt} required />
          <button className="button button-primary" disabled={createSlot.isPending || users.isPending}>
            {createSlot.isPending ? "Publishing..." : "Publish time"}
          </button>
        </form>
        {createSlot.error && <ErrorText error={createSlot.error} />}
      </section>
      <div className="dashboard-grid admin-lower">
        <section className="surface-card">
          <CardHeading eyebrow="SCHEDULE OVERSIGHT" title="Upcoming schedule" />
          {schedule.isPending ? <Loading /> : schedule.error ? <ErrorText error={schedule.error} /> :
            schedule.data?.length ? <div className="appointment-list">
              {schedule.data.slice(0, 8).map((appointment) => (
                <article className="appointment-row" key={appointment.id}>
                  <div className="appointment-date"><strong>{new Date(appointment.startAt).toLocaleDateString("en", { day: "2-digit" })}</strong>
                    <span>{new Date(appointment.startAt).toLocaleDateString("en", { month: "short" })}</span></div>
                  <div className="appointment-info"><strong>{appointment.status === "BOOKED" ? "Booked visit" : "Open time"}</strong>
                    <span>Doctor {appointment.doctorId.slice(0, 8)} · {formatTime(appointment.startAt)}</span></div>
                  {appointment.status === "BOOKED" ?
                    <button className="button button-quiet" onClick={() => cancel.mutate(appointment.id)}>Cancel</button> :
                    <button className="button button-quiet" onClick={() => deleteSlot.mutate(appointment.id)}>Remove</button>}
                </article>
              ))}
            </div> : <EmptyState title="No appointments" text="Schedules will appear here as doctors publish availability." />}
        </section>
        <section className="surface-card">
          <CardHeading eyebrow="ACCESS HISTORY" title="Recent audit records" />
          {audit.isPending ? <Loading /> : audit.error ? <ErrorText error={audit.error} /> :
            <div className="audit-list">{audit.data?.content.map((event) => (
              <article className="audit-row" key={event.eventId}>
                <span className="audit-action">{event.action}</span>
                <div><strong>{event.resourceType}</strong><span>{event.role} · {new Date(event.occurredAt).toLocaleString()}</span></div>
              </article>
            ))}</div>}
        </section>
      </div>
    </>
  );
}

function Stat({ label, value, icon }: { label: string; value: number; icon: string }) {
  return <article className="stat-card"><span className="stat-icon">{icon}</span>
    <span className="stat-label">{label}</span><strong>{value}</strong></article>;
}
