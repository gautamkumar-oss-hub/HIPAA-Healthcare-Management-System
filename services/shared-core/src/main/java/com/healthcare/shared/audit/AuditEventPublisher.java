package com.healthcare.shared.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.shared.crypto.FieldEncryption;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class AuditEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final FieldEncryption encryption;

    public AuditEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                               FieldEncryption encryption) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.encryption = encryption;
    }

    public void publish(String userId, String role, String action, String resourceType, String resourceId) {
        AuditEvent event = new AuditEvent(userId, role, action, resourceType, resourceId, Instant.now());
        try {
            String payload = objectMapper.writeValueAsString(event);
            String encryptedPayload = java.util.Base64.getEncoder()
                    .encodeToString(encryption.encrypt(payload));
            kafkaTemplate.send("phi-audit-events", event.eventId(), encryptedPayload)
                    .get(5, TimeUnit.SECONDS);
        } catch (JsonProcessingException | InterruptedException | ExecutionException | TimeoutException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Could not persist audit event", exception);
        }
    }
}
