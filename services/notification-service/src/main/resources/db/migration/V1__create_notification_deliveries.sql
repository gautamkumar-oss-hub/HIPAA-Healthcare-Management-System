CREATE TABLE notification_delivery (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    appointment_id_enc BYTEA NOT NULL,
    patient_id_enc BYTEA NOT NULL,
    appointment_at_enc BYTEA NOT NULL,
    channel VARCHAR(16) NOT NULL CHECK (channel IN ('EMAIL', 'SMS')),
    status VARCHAR(16) NOT NULL CHECK (status IN ('SIMULATED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notification_event_channel_unique UNIQUE (event_id, channel)
);
