package com.healthcare.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.shared.crypto.FieldEncryption;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Component
public class AppointmentEventConsumer {
    private final NotificationDeliveryRepository deliveries;
    private final FieldEncryption encryption;
    private final ObjectMapper objectMapper;

    public AppointmentEventConsumer(NotificationDeliveryRepository deliveries, FieldEncryption encryption,
                                    ObjectMapper objectMapper) {
        this.deliveries = deliveries;
        this.encryption = encryption;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "appointment-events")
    @Transactional
    public void consume(String encryptedPayload) {
        try {
            String json = encryption.decrypt(Base64.getDecoder().decode(encryptedPayload));
            AppointmentEvent event = objectMapper.readValue(json, AppointmentEvent.class);
            UUID eventId = UUID.fromString(event.eventId());
            for (NotificationChannel channel : NotificationChannel.values()) {
                if (!deliveries.existsByEventIdAndChannel(eventId, channel)) {
                    deliveries.save(new NotificationDelivery(event, channel, encryption));
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not process appointment notification", exception);
        }
    }

    public record AppointmentEvent(String eventId, String type, String appointmentId, String doctorId,
                                   String patientId, java.time.Instant startAt) {}
}
