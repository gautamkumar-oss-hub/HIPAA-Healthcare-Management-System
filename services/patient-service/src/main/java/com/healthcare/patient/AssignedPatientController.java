package com.healthcare.patient;

import com.healthcare.shared.audit.AuditEventPublisher;
import com.healthcare.shared.crypto.FieldEncryption;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@PreAuthorize("hasRole('DOCTOR')")
public class AssignedPatientController {
    private final PatientProfileRepository profiles;
    private final AppointmentAuthorizationClient authorization;
    private final FieldEncryption encryption;
    private final AuditEventPublisher audit;

    public AssignedPatientController(PatientProfileRepository profiles,
                                     AppointmentAuthorizationClient authorization,
                                     FieldEncryption encryption, AuditEventPublisher audit) {
        this.profiles = profiles;
        this.authorization = authorization;
        this.encryption = encryption;
        this.audit = audit;
    }

    @GetMapping("/{patientId}")
    @Transactional(readOnly = true)
    public AssignedPatientSummary getAssignedPatient(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId) {
        if (!authorization.isAssigned(patientId, jwt.getTokenValue())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        PatientProfile profile = profiles.findByUserId(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        audit.publish(jwt.getSubject(), "DOCTOR", "READ", "patient_profile", profile.getId().toString());
        return new AssignedPatientSummary(patientId, encryption.decrypt(profile.getFirstName()),
                encryption.decrypt(profile.getLastName()),
                LocalDate.parse(encryption.decrypt(profile.getDateOfBirth())));
    }

    public record AssignedPatientSummary(UUID patientId, String firstName, String lastName,
                                         LocalDate dateOfBirth) {}
}
