package com.healthcare.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.shared.audit.AuditEvent;
import com.healthcare.shared.crypto.FieldEncryption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuditEventConsumerTest {
    @Autowired
    AuditEventConsumer consumer;

    @Autowired
    AuditRecordRepository records;

    @Autowired
    FieldEncryption encryption;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void clearRecords() {
        records.deleteAll();
    }

    @Test
    void consumesEncryptedAuditEventsIdempotentlyAndEncryptsIdentifiersAtRest() throws Exception {
        AuditEvent event = new AuditEvent(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), "PATIENT", "READ", "patient_profile",
                UUID.randomUUID().toString(), Instant.now());
        String payload = Base64.getEncoder().encodeToString(
                encryption.encrypt(objectMapper.writeValueAsString(event)));

        consumer.consume(payload);
        consumer.consume(payload);

        assertThat(records.count()).isEqualTo(1);
        assertThat(records.findAll().getFirst().toView(encryption).userId()).isEqualTo(event.userId());
    }
}
