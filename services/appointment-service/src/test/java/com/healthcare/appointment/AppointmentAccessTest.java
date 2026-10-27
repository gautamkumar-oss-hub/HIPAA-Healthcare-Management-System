package com.healthcare.appointment;

import com.healthcare.shared.audit.AuditEventPublisher;
import com.healthcare.shared.audit.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentAccessTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppointmentRepository appointments;

    @MockBean
    AuditEventPublisher audit;

    @MockBean
    DomainEventPublisher events;

    @BeforeEach
    void clearAppointments() {
        appointments.deleteAll();
    }

    @Test
    void bookingIsExclusiveAndAppointmentsAreScopedToTheCaller() throws Exception {
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(86_400);
        Instant end = start.plusSeconds(1800);
        String availability = """
                {"startAt":"%s","endAt":"%s"}
                """.formatted(start, end);

        String availableJson = mockMvc.perform(post("/appointments/availability")
                        .with(jwt().jwt(token -> token.subject(doctorId.toString())
                                .claim("roles", List.of("DOCTOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availability))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID slotId = UUID.fromString(
                com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                        .readTree(availableJson).get("id").asText());
        String booking = "{\"slotId\":\"" + slotId + "\"}";

        mockMvc.perform(post("/appointments")
                        .with(jwt().jwt(token -> token.subject(patientId.toString())
                                .claim("roles", List.of("PATIENT")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").doesNotExist());

        mockMvc.perform(post("/appointments")
                        .with(jwt().jwt(token -> token.subject(otherPatientId.toString())
                                .claim("roles", List.of("PATIENT")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/appointments/me")
                        .with(jwt().jwt(token -> token.subject(patientId.toString())
                                .claim("roles", List.of("PATIENT")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].doctorId").value(doctorId.toString()))
                .andExpect(jsonPath("$[0].patientId").doesNotExist());

        mockMvc.perform(get("/appointments/me")
                        .with(jwt().jwt(token -> token.subject(otherPatientId.toString())
                                .claim("roles", List.of("PATIENT")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/appointments/me")
                        .with(jwt().jwt(token -> token.subject(doctorId.toString())
                                .claim("roles", List.of("DOCTOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(patientId.toString()));

        mockMvc.perform(get("/appointments/assigned/{patientId}", patientId)
                        .with(jwt().jwt(token -> token.subject(doctorId.toString())
                                .claim("roles", List.of("DOCTOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string("true"));

        mockMvc.perform(get("/appointments/assigned/{patientId}", otherPatientId)
                        .with(jwt().jwt(token -> token.subject(doctorId.toString())
                                .claim("roles", List.of("DOCTOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string("false"));
    }
}
