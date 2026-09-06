CREATE TABLE smtp_configuration (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    version BIGINT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    host VARCHAR(255),
    port INTEGER NOT NULL DEFAULT 587 CHECK (port >= 1 AND port <= 65535),
    transport_security VARCHAR(16) NOT NULL DEFAULT 'STARTTLS'
        CHECK (transport_security IN ('NONE', 'STARTTLS', 'SSL_TLS')),
    authentication_required BOOLEAN NOT NULL DEFAULT TRUE,
    sender_address VARCHAR(320),
    username VARCHAR(320),
    encrypted_password VARCHAR(4096),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO smtp_configuration (id, updated_at)
VALUES (1, CURRENT_TIMESTAMP);
