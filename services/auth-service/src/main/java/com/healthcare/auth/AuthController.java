package com.healthcare.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public CurrentUser currentUser(@AuthenticationPrincipal Jwt jwt) {
        return new CurrentUser(UUID.fromString(jwt.getSubject()),
                UserRole.valueOf(jwt.getClaimAsStringList("roles").getFirst()));
    }

    @PostMapping("/register")
    public AuthService.TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.email(), request.password());
    }

    @PostMapping("/login")
    public AuthService.TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password(), request.mfaCode());
    }

    @PostMapping("/refresh")
    public AuthService.TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @PostMapping("/mfa/enroll")
    public AuthService.MfaSetup setupMfa(@Valid @RequestBody MfaSetupRequest request) {
        return authService.setupMfa(request.email(), request.password());
    }

    @PostMapping("/mfa/verify")
    public void verifyMfa(@Valid @RequestBody MfaEnrollmentRequest request) {
        authService.verifyMfa(request.email(), request.password(), request.code());
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 12, max = 128) String password) {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 128) String password,
            @Size(min = 6, max = 6) String mfaCode) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record MfaSetupRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 128) String password) {}
    public record MfaEnrollmentRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 128) String password,
            @NotBlank @Size(min = 6, max = 6) String code) {}
    public record CurrentUser(UUID id, UserRole role) {}
}
