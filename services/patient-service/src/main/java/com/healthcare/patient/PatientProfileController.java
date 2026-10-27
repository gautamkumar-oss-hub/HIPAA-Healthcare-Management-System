package com.healthcare.patient;

import com.healthcare.shared.audit.AuditEventPublisher;
import com.healthcare.shared.crypto.FieldEncryption;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/patients/me")
@PreAuthorize("hasRole('PATIENT')")
public class PatientProfileController {
    private final PatientProfileRepository profiles;
    private final FieldEncryption encryption;
    private final AuditEventPublisher audit;

    public PatientProfileController(PatientProfileRepository profiles, FieldEncryption encryption,
                                    AuditEventPublisher audit) {
        this.profiles = profiles;
        this.encryption = encryption;
        this.audit = audit;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public PatientProfileResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        PatientProfile profile = profiles.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        audit.publish(userId.toString(), "PATIENT", "READ", "patient_profile", profile.getId().toString());
        return toResponse(profile);
    }

    @PutMapping
    @Transactional
    public PatientProfileResponse saveMyProfile(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PatientProfileRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        PatientProfile profile = profiles.findByUserId(userId).orElse(null);
        String action = profile == null ? "CREATE" : "UPDATE";
        if (profile == null) {
            profile = new PatientProfile(userId, encrypt(request.firstName()), encrypt(request.lastName()),
                    encrypt(request.dateOfBirth().toString()), encrypt(request.phone()));
        } else {
            profile.update(encrypt(request.firstName()), encrypt(request.lastName()),
                    encrypt(request.dateOfBirth().toString()), encrypt(request.phone()));
        }
        PatientProfile saved = profiles.save(profile);
        audit.publish(userId.toString(), "PATIENT", action, "patient_profile", saved.getId().toString());
        return toResponse(saved);
    }

    @DeleteMapping
    @Transactional
    public void deleteMyProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        PatientProfile profile = profiles.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        audit.publish(userId.toString(), "PATIENT", "DELETE", "patient_profile", profile.getId().toString());
        profiles.delete(profile);
    }

    private PatientProfileResponse toResponse(PatientProfile profile) {
        return new PatientProfileResponse(profile.getId(),
                decrypt(profile.getFirstName()),
                decrypt(profile.getLastName()),
                LocalDate.parse(decrypt(profile.getDateOfBirth())),
                decrypt(profile.getPhone()));
    }

    private byte[] encrypt(String value) {
        return encryption.encrypt(value);
    }

    private String decrypt(byte[] value) {
        return encryption.decrypt(value);
    }

    public record PatientProfileRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @jakarta.validation.constraints.NotNull @Past LocalDate dateOfBirth,
            @NotBlank @Pattern(regexp = "[+]?[0-9 ()-]{7,20}") String phone) {
    }

    public record PatientProfileResponse(UUID id, String firstName, String lastName,
                                         LocalDate dateOfBirth, String phone) {
    }
}
