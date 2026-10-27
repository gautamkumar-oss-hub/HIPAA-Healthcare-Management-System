package com.healthcare.patient;

import com.healthcare.shared.audit.AuditEventPublisher;
import com.healthcare.shared.crypto.FieldEncryption;
import org.mockito.Mockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PatientProfileAccessTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    PatientProfileRepository profiles;

    @Autowired
    FieldEncryption encryption;

    @MockBean
    AuditEventPublisher audit;

    @MockBean
    AppointmentAuthorizationClient appointmentAuthorization;

    @BeforeEach
    void clearProfiles() {
        profiles.deleteAll();
    }

    @Test
    void patientCanOnlyReadOwnEncryptedProfile() throws Exception {
        UUID patientOne = UUID.randomUUID();
        UUID patientTwo = UUID.randomUUID();
        String payload = """
                {
                  "firstName": "Synthetic",
                  "lastName": "Patient",
                  "dateOfBirth": "1990-01-02",
                  "phone": "+15555550123"
                }
                """;

        mockMvc.perform(put("/patients/me")
                        .with(jwt().jwt(token -> token.subject(patientOne.toString())
                                .claim("roles", java.util.List.of("PATIENT")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        PatientProfile stored = profiles.findByUserId(patientOne).orElseThrow();
        assertThat(encryption.decrypt(stored.getFirstName())).isEqualTo("Synthetic");
        assertThat(new String(stored.getFirstName(), java.nio.charset.StandardCharsets.UTF_8))
                .doesNotContain("Synthetic");

        mockMvc.perform(get("/patients/me")
                        .with(jwt().jwt(token -> token.subject(patientTwo.toString())
                                .claim("roles", java.util.List.of("PATIENT")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/patients/me")
                        .with(jwt().jwt(token -> token.subject(patientOne.toString())
                                .claim("roles", java.util.List.of("DOCTOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/patients/me")
                        .with(jwt().jwt(token -> token.subject(patientOne.toString())
                                .claim("roles", java.util.List.of("PATIENT")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))))
                .andExpect(status().isOk());
    }

    @Test
    void doctorCanReadOnlyAnAssignedPatientAndGetsMinimumFields() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        profiles.save(new PatientProfile(patientId, encryption.encrypt("Synthetic"),
                encryption.encrypt("Patient"), encryption.encrypt("1990-01-02"),
                encryption.encrypt("+15555550123")));
        Mockito.when(appointmentAuthorization.isAssigned(
                Mockito.eq(patientId), Mockito.anyString())).thenReturn(false, true);

        mockMvc.perform(get("/patients/{id}", patientId)
                        .with(jwt().jwt(token -> token.subject(doctorId.toString())
                                .claim("roles", java.util.List.of("DOCTOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/patients/{id}", patientId)
                        .with(jwt().jwt(token -> token.subject(doctorId.toString())
                                .claim("roles", java.util.List.of("DOCTOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.firstName").value("Synthetic"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.phone").doesNotExist());
    }
}
