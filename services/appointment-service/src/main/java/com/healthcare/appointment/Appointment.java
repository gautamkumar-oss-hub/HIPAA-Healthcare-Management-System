package com.healthcare.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointment")
public class Appointment {
    @Id
    private UUID id;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AppointmentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Appointment() {
    }

    public Appointment(UUID doctorId, Instant startAt, Instant endAt) {
        this.id = UUID.randomUUID();
        this.doctorId = doctorId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = AppointmentStatus.AVAILABLE;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDoctorId() { return doctorId; }
    public UUID getPatientId() { return patientId; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public AppointmentStatus getStatus() { return status; }

    public void book(UUID patientId) {
        this.patientId = patientId;
        this.status = AppointmentStatus.BOOKED;
    }

    public void release() {
        this.patientId = null;
        this.status = AppointmentStatus.AVAILABLE;
    }
}
