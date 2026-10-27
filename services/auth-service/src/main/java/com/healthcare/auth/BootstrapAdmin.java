package com.healthcare.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final UserRepository users;
    private final AuthService authService;
    private final String email;
    private final String password;

    public BootstrapAdmin(UserRepository users, AuthService authService,
                          @Value("${healthcare.bootstrap.admin-email:}") String email,
                          @Value("${healthcare.bootstrap.admin-password:}") String password) {
        this.users = users;
        this.authService = authService;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Bootstrap admin email and password must both be configured");
        }
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (!users.existsByEmail(normalizedEmail)) {
            authService.createUser(normalizedEmail, password, UserRole.ADMIN);
        }
    }
}
