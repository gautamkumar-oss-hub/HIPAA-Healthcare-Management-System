package com.healthcare.appointment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from Appointment appointment where appointment.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") UUID id);

    List<Appointment> findByPatientIdOrderByStartAtAsc(UUID patientId);
    List<Appointment> findByDoctorIdOrderByStartAtAsc(UUID doctorId);
    boolean existsByDoctorIdAndPatientIdAndStatus(UUID doctorId, UUID patientId, AppointmentStatus status);
    List<Appointment> findByStatusAndStartAtBetweenOrderByStartAtAsc(
            AppointmentStatus status, Instant startAt, Instant endAt);

    @Query("""
            select count(appointment) > 0 from Appointment appointment
            where appointment.doctorId = :doctorId
            and appointment.startAt < :endAt
            and appointment.endAt > :startAt
            """)
    boolean existsOverlappingSlot(@Param("doctorId") UUID doctorId,
                                  @Param("startAt") Instant startAt,
                                  @Param("endAt") Instant endAt);
}
