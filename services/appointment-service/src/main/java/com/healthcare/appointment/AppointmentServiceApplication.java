package com.healthcare.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.healthcare.shared.audit.AuditEventPublisher;
import com.healthcare.shared.audit.DomainEventPublisher;
import com.healthcare.shared.crypto.FieldEncryption;
import com.healthcare.shared.security.ServiceSecurityConfiguration;
import com.healthcare.appointment.UserDirectoryClient;

@SpringBootApplication
@Import({ServiceSecurityConfiguration.class, FieldEncryption.class,
        AuditEventPublisher.class, DomainEventPublisher.class, UserDirectoryClient.class})
public class AppointmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppointmentServiceApplication.class, args);
    }
}
