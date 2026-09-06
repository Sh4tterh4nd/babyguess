CREATE TABLE reveal_email_delivery (
    id UUID PRIMARY KEY,
    participant_id UUID NOT NULL REFERENCES participant(id),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    failure_code VARCHAR(160),
    UNIQUE (participant_id)
);

CREATE INDEX reveal_email_delivery_status_idx
    ON reveal_email_delivery (status, created_at);
