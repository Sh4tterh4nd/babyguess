CREATE TABLE participant_link_delivery (
    id UUID PRIMARY KEY,
    participant_id UUID NOT NULL REFERENCES participant(id),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    failure_code VARCHAR(160)
);

CREATE INDEX participant_link_delivery_status_idx
    ON participant_link_delivery (status, created_at);
