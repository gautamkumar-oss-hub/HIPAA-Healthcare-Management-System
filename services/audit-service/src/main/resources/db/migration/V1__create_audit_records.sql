CREATE TABLE audit_record (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    user_id_enc BYTEA NOT NULL,
    role VARCHAR(16) NOT NULL,
    action VARCHAR(16) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id_enc BYTEA NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX audit_record_occurred_at_idx ON audit_record(occurred_at DESC);
CREATE INDEX audit_record_action_idx ON audit_record(action);

CREATE FUNCTION prevent_audit_record_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit records are immutable';
END;
$$;

CREATE TRIGGER audit_record_immutable
BEFORE UPDATE OR DELETE ON audit_record
FOR EACH ROW EXECUTE FUNCTION prevent_audit_record_mutation();
