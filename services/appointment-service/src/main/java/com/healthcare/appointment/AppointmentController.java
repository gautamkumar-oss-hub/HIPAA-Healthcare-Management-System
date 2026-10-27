package com.healthcare.appointment;

import com.healthcare.shared.audit.AuditEventPublisher;
import com.healthcare.shared.audit.DomainEventPublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {
    private final AppointmentRepository appointments;
    private final AuditEventPublisher audit;
    private final DomainEventPublisher events;
    private final UserDirectoryClient userDirectory;

    public AppointmentController(AppointmentRepository appointments, AuditEventPublisher audit,
                                 DomainEventPublisher events, UserDirectoryClient userDirectory) {
        this.appointments = appointments;
        this.audit = audit;
        this.events = events;
        this.userDirectory = userDirectory;
    }

    @PostMapping("/availability")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Transactional
    public AppointmentView createAvailability(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AvailabilityRequest request) {
        validateSlot(request.startAt(), request.endAt());
        String role = role(jwt);
        UUID doctorId = switch (role) {
            case "DOCTOR" -> {
                if (request.doctorId() != null && !request.doctorId().equals(userId(jwt))) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctors can only manage their schedule");
                }
                yield userId(jwt);
            }
            case "ADMIN" -> {
                if (request.doctorId() == null
                        || !userDirectory.isDoctor(request.doctorId(), jwt.getTokenValue())) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found");
                }
                yield request.doctorId();
            }
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role");
        };
        if (appointments.existsOverlappingSlot(doctorId, request.startAt(), request.endAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Availability overlaps an existing slot");
        }
        Appointment slot = appointments.save(new Appointment(doctorId, request.startAt(), request.endAt()));
        audit.publish(userId(jwt).toString(), role, "CREATE", "appointment_slot", slot.getId().toString());
        return view(slot, role);
    }

    @GetMapping("/availability")
    @PreAuthorize("hasRole('PATIENT')")
    @Transactional(readOnly = true)
    public List<AppointmentView> availableSlots(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        Instant start = from == null ? Instant.now() : from;
        Instant end = to == null ? start.plus(Duration.ofDays(30)) : to;
        if (!end.isAfter(start) || Duration.between(start, end).compareTo(Duration.ofDays(90)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid availability date range");
        }
        List<Appointment> slots = appointments.findByStatusAndStartAtBetweenOrderByStartAtAsc(
                AppointmentStatus.AVAILABLE, start, end);
        UUID userId = userId(jwt);
        for (Appointment slot : slots) {
            audit.publish(userId.toString(), "PATIENT", "READ", "appointment_slot", slot.getId().toString());
        }
        return slots.stream().map(slot -> view(slot, "PATIENT")).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Transactional
    public AppointmentView book(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BookRequest request) {
        UUID patientId = userId(jwt);
        Appointment slot = lockSlot(request.slotId());
        if (slot.getStatus() != AppointmentStatus.AVAILABLE || !slot.getStartAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment slot is no longer available");
        }
        slot.book(patientId);
        audit.publish(patientId.toString(), "PATIENT", "CREATE", "appointment", slot.getId().toString());
        publishAppointmentEvent("BOOKED", slot, patientId);
        return view(slot, "PATIENT");
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @Transactional(readOnly = true)
    public List<AppointmentView> myAppointments(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = userId(jwt);
        String role = role(jwt);
        List<Appointment> results = switch (role) {
            case "PATIENT" -> appointments.findByPatientIdOrderByStartAtAsc(userId);
            case "DOCTOR" -> appointments.findByDoctorIdOrderByStartAtAsc(userId);
            case "ADMIN" -> appointments.findAll();
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role");
        };
        for (Appointment appointment : results) {
            audit.publish(userId.toString(), role, "READ", "appointment", appointment.getId().toString());
        }
        return results.stream().map(appointment -> view(appointment, role)).toList();
    }

    @GetMapping("/assigned/{patientId}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Transactional(readOnly = true)
    public boolean verifyPatientAssignment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId) {
        UUID doctorId = userId(jwt);
        boolean assigned = appointments.existsByDoctorIdAndPatientIdAndStatus(
                doctorId, patientId, AppointmentStatus.BOOKED);
        if (assigned) {
            audit.publish(doctorId.toString(), "DOCTOR", "READ",
                    "patient_appointment_assignment", patientId.toString());
        }
        return assigned;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @Transactional
    public AppointmentView cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID userId = userId(jwt);
        String role = role(jwt);
        Appointment appointment = lockSlot(id);
        verifyCanManage(appointment, userId, role);
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only booked appointments can be cancelled");
        }
        UUID patientId = appointment.getPatientId();
        appointment.release();
        audit.publish(userId.toString(), role, "UPDATE", "appointment", appointment.getId().toString());
        publishAppointmentEvent("CANCELLED", appointment, patientId);
        return view(appointment, role);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Transactional
    public void deleteAvailability(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID userId = userId(jwt);
        String role = role(jwt);
        Appointment appointment = lockSlot(id);
        verifyCanManage(appointment, userId, role);
        if (appointment.getStatus() != AppointmentStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booked appointments cannot be deleted");
        }
        appointments.delete(appointment);
        audit.publish(userId.toString(), role, "DELETE", "appointment_slot", id.toString());
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasRole('PATIENT')")
    @Transactional
    public AppointmentView reschedule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                      @Valid @RequestBody BookRequest request) {
        UUID patientId = userId(jwt);
        Appointment current = lockSlot(id);
        if (!patientId.equals(current.getPatientId()) || current.getStatus() != AppointmentStatus.BOOKED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
        }
        Appointment replacement = lockSlot(request.slotId());
        if (replacement.getStatus() != AppointmentStatus.AVAILABLE
                || !replacement.getStartAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment slot is no longer available");
        }
        current.release();
        replacement.book(patientId);
        audit.publish(patientId.toString(), "PATIENT", "UPDATE", "appointment", current.getId().toString());
        audit.publish(patientId.toString(), "PATIENT", "CREATE", "appointment", replacement.getId().toString());
        publishAppointmentEvent("CANCELLED", current, patientId);
        publishAppointmentEvent("BOOKED", replacement, patientId);
        return view(replacement, "PATIENT");
    }

    private Appointment lockSlot(UUID id) {
        return appointments.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
    }

    private void verifyCanManage(Appointment appointment, UUID userId, String role) {
        boolean allowed = switch (role) {
            case "PATIENT" -> userId.equals(appointment.getPatientId());
            case "DOCTOR" -> userId.equals(appointment.getDoctorId());
            case "ADMIN" -> true;
            default -> false;
        };
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
        }
    }

    private void publishAppointmentEvent(String type, Appointment appointment, UUID patientId) {
        AppointmentEvent event = new AppointmentEvent(UUID.randomUUID().toString(), type,
                appointment.getId().toString(), appointment.getDoctorId().toString(),
                patientId.toString(), appointment.getStartAt());
        events.publish("appointment-events", event.eventId(), event);
    }

    private AppointmentView view(Appointment appointment, String role) {
        return new AppointmentView(appointment.getId(), appointment.getDoctorId(),
                "DOCTOR".equals(role) ? appointment.getPatientId() : null,
                appointment.getStartAt(), appointment.getEndAt(), appointment.getStatus());
    }

    private static void validateSlot(Instant start, Instant end) {
        if (!start.isAfter(Instant.now()) || !end.isAfter(start)
                || Duration.between(start, end).compareTo(Duration.ofHours(4)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid appointment time range");
        }
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static String role(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null || roles.size() != 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid role claim");
        }
        return roles.getFirst();
    }

    public record AvailabilityRequest(@NotNull Instant startAt, @NotNull Instant endAt, UUID doctorId) {}
    public record BookRequest(@NotNull UUID slotId) {}
    public record AppointmentView(UUID id, UUID doctorId, UUID patientId, Instant startAt,
                                  Instant endAt, AppointmentStatus status) {}
    public record AppointmentEvent(String eventId, String type, String appointmentId, String doctorId,
                                   String patientId, Instant startAt) {}
}
