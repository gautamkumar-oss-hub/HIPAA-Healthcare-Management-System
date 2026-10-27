package com.healthcare.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthServiceIntegrationTest {
    @Autowired
    AuthService authService;

    @Autowired
    UserRepository users;

    @Autowired
    RefreshTokenRepository refreshTokens;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    Totp totp;

    @Autowired
    FieldEncryption fieldEncryption;

    @BeforeEach
    void clearUsers() {
        refreshTokens.deleteAll();
        users.deleteAll();
    }

    @Test
    void registrationCreatesPatientAndLoginReturnsSignedTokens() {
        AuthService.TokenResponse registration =
                authService.register(" Patient@Example.test ", "StrongPassword123!");

        assertThat(registration.role()).isEqualTo(UserRole.PATIENT);
        assertThat(registration.accessToken()).isNotBlank();
        assertThat(registration.refreshToken()).isNotBlank();
        assertThat(registration.expiresAt()).isAfter(java.time.Instant.now());
        assertThat(authService.login("patient@example.test", "StrongPassword123!", null).role())
                .isEqualTo(UserRole.PATIENT);
    }

    @Test
    void refreshTokensRotateAndCannotBeReused() {
        AuthService.TokenResponse first =
                authService.register("patient@example.test", "StrongPassword123!");

        AuthService.TokenResponse second = authService.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThatThrownBy(() -> authService.refresh(first.refreshToken()))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void publicRegistrationAlwaysCreatesPatient() {
        authService.register("patient@example.test", "StrongPassword123!");

        assertThat(users.findByEmail("patient@example.test")).get()
                .extracting(AppUser::getRole)
                .isEqualTo(UserRole.PATIENT);
    }

    @Test
    void privilegedUserMustEnableMfaBeforeLogin() {
        users.save(new AppUser("doctor@example.test",
                passwordEncoder.encode("StrongPassword123!"), UserRole.DOCTOR));

        assertThatThrownBy(() -> authService.login("doctor@example.test", "StrongPassword123!", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("MFA enrollment required");
    }

    @Test
    void privilegedUserCanEnrollAndMustPresentTotpAtLogin() {
        users.save(new AppUser("doctor@example.test",
                passwordEncoder.encode("StrongPassword123!"), UserRole.DOCTOR));

        assertThatThrownBy(() -> authService.login("doctor@example.test", "StrongPassword123!", null))
                .isInstanceOf(ResponseStatusException.class);
        AuthService.MfaSetup setup = authService.setupMfa("doctor@example.test", "StrongPassword123!");
        assertThat(setup.provisioningUri()).contains("secret=").contains("algorithm=SHA1");
        assertThat(setup.provisioningUri()).containsPattern("secret=[A-Z2-7]+&");

        byte[] encryptedSecret = users.findByEmail("doctor@example.test").orElseThrow().getMfaSecret();
        String code = totp.currentCode(fieldEncryption.decrypt(encryptedSecret));
        authService.verifyMfa("doctor@example.test", "StrongPassword123!", code);

        assertThat(authService.login("doctor@example.test", "StrongPassword123!", code).role())
                .isEqualTo(UserRole.DOCTOR);
        String wrongCode = (code.charAt(0) == '0' ? "1" : "0") + code.substring(1);
        assertThatThrownBy(() -> authService.login("doctor@example.test", "StrongPassword123!", wrongCode))
                .isInstanceOf(BadCredentialsException.class);
    }
}
