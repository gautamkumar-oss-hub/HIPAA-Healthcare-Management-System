package com.healthcare.shared.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(String eventId, String userId, String role, String action, String resourceType,
                         String resourceId, Instant timestamp) {
    public AuditEvent(String userId, String role, String action, String resourceType,
                      String resourceId, Instant timestamp) {
        this(UUID.randomUUID().toString(), userId, role, action, resourceType, resourceId, timestamp);
    }
}
