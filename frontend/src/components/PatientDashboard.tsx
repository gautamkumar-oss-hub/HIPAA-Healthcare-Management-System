import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../api/client";
import type { Appointment, PatientProfile, PatientProfileInput } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { formatTime } from "../utils";

export function PatientDashboard() {
  const { api } = useAuth();
  const queryClient = useQueryClient();
  const profile = useQuery({
    queryKey: ["patient-profile"],
    queryFn: () => api<PatientProfile>("/patients/me"),
  });
  const appointments = useQuery({
    queryKey: ["my-appointments"],
    queryFn: () => api<Appointment[]>("/appointments/me"),
  });
  const slots = useQuery({
    queryKey: ["available-slots"],
    queryFn: () => api<Appointment[]>("/appointments/availability"),
  });
  const saveProfile = useMutation({
    mutationFn: (input: PatientProfileInput) => api<PatientProfile>("/patients/me", {
      method: "PUT", body: JSON.stringify(input),
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["patient-profile"] }),
  });
  const bookSlot = useMutation({
    mutationFn: (slotId: string) => api<Appointment>("/appointments", {
      method: "POST", body: JSON.stringify({ slotId }),
    }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["my-appointments"] });
      void queryClient.invalidateQueries({ queryKey: ["available-slots"] });
    },
  });
  const cancel = useMutation({
    mutationFn: (id: string) => api<Appointment>(`/appointments/${id}/cancel`, { method: "POST" }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["my-appointments"] });
      void queryClient.invalidateQueries({ queryKey: ["available-slots"] });
    },
  });
  const reschedule = useMutation({
    mutationFn: ({ id, slotId }: { id: string; slotId: string }) =>
      api<Appointment>(`/appointments/${id}/reschedule`, {
        method: "POST", body: JSON.stringify({ slotId }),
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["my-appointments"] });
      void queryClient.invalidateQueries({ queryKey: ["available-slots"] });
    },
  });

  const isMissingProfile = profile.error instanceof ApiError && profile.error.status === 404;
  const [editing, setEditing] = useState(isMissingProfile);
  const [form, setForm] = useState<PatientProfileInput>({
    firstName: "", lastName: "", dateOfBirth: "", phone: "",
  });
  const booked = appointments.data?.filter((item) => item.status === "BOOKED") ?? [];
  const upcoming = booked.filter((item) => new Date(item.startAt) > new Date());

  function submitProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    saveProfile.mutate(form, { onSuccess: () => setEditing(false) });
  }

  return (
    <>
      <section className="welcome-row">
        <div>
          <span className="eyebrow">PATIENT PORTAL</span>
          <h1>Hello{profile.data ? `, ${profile.data.firstName}` : ""} <span className="wave">✦</span></h1>
          <p className="muted">Your health, your schedule, all in one place.</p>
        </div>
        <div className="date-chip">{new Intl.DateTimeFormat("en", { weekday: "long", month: "long", day: "numeric" }).format(new Date())}</div>
      </section>

      <section className="stat-grid">
        <StatCard label="Upcoming visits" value={appointments.isLoading ? "—" : String(upcoming.length)} icon="◷" />
        <StatCard label="Profile status" value={profile.data ? "Complete" : "Getting started"} icon="◉" />
        <StatCard label="Care team" value="Your clinic" icon="✳" />
      </section>

      <div className="dashboard-grid">
        <section className="surface-card">
          <CardHeading eyebrow="YOUR SCHEDULE" title="Upcoming appointments" />
          {appointments.isPending ? <Loading /> : appointments.error ? <ErrorText error={appointments.error} /> :
            upcoming.length ? <div className="appointment-list">
              {upcoming.map((appointment) => (
                <article className="appointment-row" key={appointment.id}>
                  <div className="appointment-date">
                    <strong>{new Date(appointment.startAt).toLocaleDateString("en", { day: "2-digit" })}</strong>
                    <span>{new Date(appointment.startAt).toLocaleDateString("en", { month: "short" })}</span>
                  </div>
                  <div className="appointment-info">
                    <strong>Care team appointment</strong>
                    <span>{formatTime(appointment.startAt)} · Doctor {appointment.doctorId.slice(0, 8)}</span>
                  </div>
                  <button className="button button-quiet" onClick={() => cancel.mutate(appointment.id)}>Cancel</button>
                  {slots.data && slots.data.length > 0 && (
                    <button className="button button-quiet" onClick={() =>
                      reschedule.mutate({ id: appointment.id, slotId: slots.data![0].id })}>
                      Reschedule
                    </button>
                  )}
                </article>
              ))}
            </div> : <EmptyState title="No upcoming appointments" text="Find a time that works for you below." />}
        </section>

        <section className="surface-card">
          <CardHeading eyebrow="YOUR DETAILS" title="Patient profile" />
          {profile.isPending && !isMissingProfile ? <Loading /> : profile.error && !isMissingProfile ?
            <ErrorText error={profile.error} /> :
            editing || !profile.data ? (
              <form className="stack-form" onSubmit={submitProfile}>
                <div className="field-pair">
                  <FormField label="First name" value={form.firstName}
                    onChange={(value) => setForm({ ...form, firstName: value })} required />
                  <FormField label="Last name" value={form.lastName}
                    onChange={(value) => setForm({ ...form, lastName: value })} required />
                </div>
                <FormField label="Date of birth" type="date" value={form.dateOfBirth}
                  onChange={(value) => setForm({ ...form, dateOfBirth: value })} required />
                <FormField label="Phone number" type="tel" value={form.phone}
                  onChange={(value) => setForm({ ...form, phone: value })} required />
                {saveProfile.error && <ErrorText error={saveProfile.error} />}
                <button className="button button-primary" disabled={saveProfile.isPending}>
                  {saveProfile.isPending ? "Saving..." : "Save profile"}
                </button>
              </form>
            ) : (
              <div className="profile-summary">
                <div className="profile-avatar">{profile.data.firstName[0]}{profile.data.lastName[0]}</div>
                <div><strong>{profile.data.firstName} {profile.data.lastName}</strong>
                  <span>{profile.data.phone}</span></div>
                <button className="text-button" onClick={() => {
                  setForm({ firstName: profile.data!.firstName, lastName: profile.data!.lastName,
                    dateOfBirth: profile.data!.dateOfBirth, phone: profile.data!.phone });
                  setEditing(true);
                }}>Edit</button>
              </div>
            )}
        </section>
      </div>

      <section className="surface-card slots-card">
        <CardHeading eyebrow="FIND A TIME" title="Available appointments" />
        {slots.isPending ? <Loading /> : slots.error ? <ErrorText error={slots.error} /> :
          slots.data?.length ? <div className="slot-grid">
            {slots.data.slice(0, 8).map((slot) => (
              <article className="slot-card" key={slot.id}>
                <span className="slot-date">{new Date(slot.startAt).toLocaleDateString("en", { weekday: "short", month: "short", day: "numeric" })}</span>
                <strong>{formatTime(slot.startAt)}</strong>
                <span className="muted">Doctor {slot.doctorId.slice(0, 8)}</span>
                <button className="button button-outline" disabled={bookSlot.isPending}
                  onClick={() => bookSlot.mutate(slot.id)}>Book this time</button>
              </article>
            ))}
          </div> : <EmptyState title="No open appointments" text="Check back soon for available times." />}
        {bookSlot.error && <ErrorText error={bookSlot.error} />}
      </section>
    </>
  );
}

function StatCard({ label, value, icon }: { label: string; value: string; icon: string }) {
  return <article className="stat-card"><span className="stat-icon">{icon}</span><span className="stat-label">{label}</span><strong>{value}</strong></article>;
}

export function CardHeading({ eyebrow, title }: { eyebrow: string; title: string }) {
  return <div className="card-heading"><span className="eyebrow">{eyebrow}</span><h2>{title}</h2></div>;
}

export function FormField({ label, value, onChange, type = "text", required = false }: {
  label: string; value: string; onChange: (value: string) => void; type?: string; required?: boolean;
}) {
  return <label className="form-field">{label}<input type={type} required={required} value={value}
    onChange={(event) => onChange(event.target.value)} /></label>;
}

export function EmptyState({ title, text }: { title: string; text: string }) {
  return <div className="empty-state"><span className="empty-icon">✳</span><strong>{title}</strong><p>{text}</p></div>;
}

export function Loading() { return <p className="muted">Loading securely...</p>; }

export function ErrorText({ error }: { error: unknown }) {
  return <p className="form-message" role="alert">{error instanceof Error ? error.message : "Unable to load this information."}</p>;
}
