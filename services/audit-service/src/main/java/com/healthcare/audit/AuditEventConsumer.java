package com.healthcare.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.shared.audit.AuditEvent;
import com.healthcare.shared.crypto.FieldEncryption;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Component
public class AuditEventConsumer {
    private final AuditRecordRepository records;
    private final FieldEncryption encryption;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(AuditRecordRepository records, FieldEncryption encryption,
                              ObjectMapper objectMapper) {
        this.records = records;
        this.encryption = encryption;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "phi-audit-events")
    @Transactional
    public void consume(String encryptedPayload) {
        try {
            String json = encryption.decrypt(Base64.getDecoder().decode(encryptedPayload));
            AuditEvent event = objectMapper.readValue(json, AuditEvent.class);
            UUID eventId = UUID.fromString(event.eventId());
            if (!records.existsByEventId(eventId)) {
                records.save(new AuditRecord(event, encryption));
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not process audit event", exception);
        }
    }
}
