package com.healthcare.shared.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.shared.crypto.FieldEncryption;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class DomainEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final FieldEncryption encryption;

    public DomainEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                                FieldEncryption encryption) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.encryption = encryption;
    }

    public void publish(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String encryptedPayload = java.util.Base64.getEncoder()
                    .encodeToString(encryption.encrypt(payload));
            kafkaTemplate.send(topic, key, encryptedPayload)
                    .get(5, TimeUnit.SECONDS);
        } catch (JsonProcessingException | InterruptedException | ExecutionException | TimeoutException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Could not publish domain event", exception);
        }
    }
}
