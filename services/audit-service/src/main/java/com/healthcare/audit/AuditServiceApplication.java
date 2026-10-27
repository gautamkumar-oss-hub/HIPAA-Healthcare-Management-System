package com.healthcare.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.healthcare.shared.crypto.FieldEncryption;
import com.healthcare.shared.security.ServiceSecurityConfiguration;

@SpringBootApplication
@Import({AuditEventConsumer.class, FieldEncryption.class, ServiceSecurityConfiguration.class})
public class AuditServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
