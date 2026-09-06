ALTER TABLE event_configuration
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
