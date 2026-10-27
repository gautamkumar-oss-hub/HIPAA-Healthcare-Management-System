package com.healthcare.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class AppointmentEventConsumerTest {
    @Autowired
    AppointmentEventConsumer consumer;

    @Autowired
    NotificationDeliveryRepository deliveries;

    @Autowired
    FieldEncryption encryption;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void clearDeliveries() {
        deliveries.deleteAll();
    }

    @Test
    void simulatesEmailAndSmsWithoutExposingFieldsInStoredPayloads() throws Exception {
        AppointmentEventConsumer.AppointmentEvent event =
                new AppointmentEventConsumer.AppointmentEvent(UUID.randomUUID().toString(), "BOOKED",
                        UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(), Instant.now());
        String encryptedPayload = Base64.getEncoder().encodeToString(
                encryption.encrypt(objectMapper.writeValueAsString(event)));

        consumer.consume(encryptedPayload);
        consumer.consume(encryptedPayload);

        assertThat(deliveries.count()).isEqualTo(2);
    }
}
