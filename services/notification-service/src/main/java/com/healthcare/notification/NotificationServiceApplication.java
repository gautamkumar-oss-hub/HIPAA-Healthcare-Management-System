package com.healthcare.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.healthcare.shared.crypto.FieldEncryption;

@SpringBootApplication
@Import({AppointmentEventConsumer.class, FieldEncryption.class})
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
