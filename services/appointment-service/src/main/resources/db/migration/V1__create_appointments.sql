CREATE TABLE appointment (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL,
    patient_id UUID,
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('AVAILABLE', 'BOOKED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT appointment_valid_time CHECK (end_at > start_at),
    CONSTRAINT appointment_doctor_start_unique UNIQUE (doctor_id, start_at)
);

CREATE INDEX appointment_patient_id_idx ON appointment(patient_id);
CREATE INDEX appointment_doctor_start_idx ON appointment(doctor_id, start_at);
