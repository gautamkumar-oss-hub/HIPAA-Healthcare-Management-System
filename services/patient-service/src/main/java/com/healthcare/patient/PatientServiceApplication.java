package com.healthcare.patient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.healthcare.shared.audit.AuditEventPublisher;
import com.healthcare.shared.crypto.FieldEncryption;
import com.healthcare.shared.security.ServiceSecurityConfiguration;

@SpringBootApplication
@Import({ServiceSecurityConfiguration.class, FieldEncryption.class, AuditEventPublisher.class,
        AppointmentAuthorizationClient.class})
public class PatientServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PatientServiceApplication.class, args);
    }
}
