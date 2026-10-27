package com.healthcare.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {
    private final AuthService authService;

    public UserManagementController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public List<AuthService.UserSummary> listUsers() {
        return authService.listUsers();
    }

    @GetMapping("/{id}")
    public AuthService.UserSummary getUser(@PathVariable UUID id) {
        return authService.getUser(id);
    }

    @PostMapping
    public AuthService.UserSummary createUser(@Valid @RequestBody CreateUserRequest request) {
        return authService.createUser(request.email(), request.password(), request.role());
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        if (id.toString().equals(jwt.getSubject())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Administrators cannot delete their own account");
        }
        authService.deleteUser(id);
    }

    public record CreateUserRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 12, max = 128) String password,
            @NotNull UserRole role) {
    }
}
