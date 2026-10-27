package com.healthcare.audit;

import com.healthcare.shared.audit.AuditEvent;
import com.healthcare.shared.crypto.FieldEncryption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_record")
public class AuditRecord {
    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "user_id_enc", nullable = false, columnDefinition = "bytea")
    private byte[] userId;

    @Column(nullable = false, length = 16)
    private String role;

    @Column(nullable = false, length = 16)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 80)
    private String resourceType;

    @Column(name = "resource_id_enc", nullable = false, columnDefinition = "bytea")
    private byte[] resourceId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditRecord() {
    }

    public AuditRecord(AuditEvent event, FieldEncryption encryption) {
        this.id = UUID.randomUUID();
        this.eventId = UUID.fromString(event.eventId());
        this.userId = encryption.encrypt(event.userId());
        this.role = event.role();
        this.action = event.action();
        this.resourceType = event.resourceType();
        this.resourceId = encryption.encrypt(event.resourceId());
        this.occurredAt = event.timestamp();
    }

    public AuditRecordView toView(FieldEncryption encryption) {
        return new AuditRecordView(eventId, encryption.decrypt(userId), role, action, resourceType,
                encryption.decrypt(resourceId), occurredAt);
    }

    public record AuditRecordView(UUID eventId, String userId, String role, String action,
                                  String resourceType, String resourceId, Instant occurredAt) {}
}
