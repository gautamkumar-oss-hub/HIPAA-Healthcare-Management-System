package com.healthcare.notification;

import com.healthcare.shared.crypto.FieldEncryption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_delivery")
public class NotificationDelivery {
    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "appointment_id_enc", nullable = false, columnDefinition = "bytea")
    private byte[] appointmentId;

    @Column(name = "patient_id_enc", nullable = false, columnDefinition = "bytea")
    private byte[] patientId;

    @Column(name = "appointment_at_enc", nullable = false, columnDefinition = "bytea")
    private byte[] appointmentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationDelivery() {
    }

    public NotificationDelivery(AppointmentEventConsumer.AppointmentEvent event, NotificationChannel channel,
                                FieldEncryption encryption) {
        this.id = UUID.randomUUID();
        this.eventId = UUID.fromString(event.eventId());
        this.appointmentId = encryption.encrypt(event.appointmentId());
        this.patientId = encryption.encrypt(event.patientId());
        this.appointmentAt = encryption.encrypt(event.startAt().toString());
        this.channel = channel;
        this.status = "SIMULATED";
        this.createdAt = Instant.now();
    }
}
