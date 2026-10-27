package com.healthcare.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_profile")
public class PatientProfile {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "first_name_enc", nullable = false, columnDefinition = "bytea")
    private byte[] firstName;

    @Column(name = "last_name_enc", nullable = false, columnDefinition = "bytea")
    private byte[] lastName;

    @Column(name = "date_of_birth_enc", nullable = false, columnDefinition = "bytea")
    private byte[] dateOfBirth;

    @Column(name = "phone_enc", nullable = false, columnDefinition = "bytea")
    private byte[] phone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PatientProfile() {
    }

    public PatientProfile(UUID userId, byte[] firstName, byte[] lastName, byte[] dateOfBirth, byte[] phone) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.createdAt = Instant.now();
        update(firstName, lastName, dateOfBirth, phone);
    }

    public void update(byte[] firstName, byte[] lastName, byte[] dateOfBirth, byte[] phone) {
        this.firstName = firstName.clone();
        this.lastName = lastName.clone();
        this.dateOfBirth = dateOfBirth.clone();
        this.phone = phone.clone();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public byte[] getFirstName() { return firstName.clone(); }
    public byte[] getLastName() { return lastName.clone(); }
    public byte[] getDateOfBirth() { return dateOfBirth.clone(); }
    public byte[] getPhone() { return phone.clone(); }
}
