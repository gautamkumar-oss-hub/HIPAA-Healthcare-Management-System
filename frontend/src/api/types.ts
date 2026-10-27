export type Role = "PATIENT" | "DOCTOR" | "ADMIN";

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresAt: string;
  role: Role;
}

export interface PatientProfile {
  id: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  phone: string;
}

export interface PatientProfileInput {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  phone: string;
}

export interface Appointment {
  id: string;
  doctorId: string;
  patientId: string | null;
  startAt: string;
  endAt: string;
  status: "AVAILABLE" | "BOOKED";
}

export interface StaffUser {
  id: string;
  email: string;
  role: Role;
  mfaEnabled: boolean;
}

export interface AuditPage {
  content: Array<{
    eventId: string;
    userId: string;
    role: string;
    action: string;
    resourceType: string;
    resourceId: string;
    occurredAt: string;
  }>;
  totalElements: number;
}
