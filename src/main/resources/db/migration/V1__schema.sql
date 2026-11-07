CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE user_account (
    id            UUID PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('PATIENT', 'RECEPTIONIST', 'ADMIN')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE patient_profile (
    id           UUID PRIMARY KEY,
    user_id      UUID UNIQUE REFERENCES user_account (id),
    display_name VARCHAR(100) NOT NULL,
    contact      VARCHAR(100),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE provider (
    id            UUID PRIMARY KEY,
    display_name  VARCHAR(100) NOT NULL,
    service_label VARCHAR(100) NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE provider_availability (
    id           UUID PRIMARY KEY,
    provider_id  UUID    NOT NULL REFERENCES provider (id) ON DELETE CASCADE,
    weekday      INTEGER NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    start_time   TIME    NOT NULL,
    end_time     TIME    NOT NULL,
    slot_minutes INTEGER NOT NULL CHECK (slot_minutes BETWEEN 5 AND 240),
    CHECK (end_time > start_time)
);
CREATE INDEX idx_availability_provider_day ON provider_availability (provider_id, weekday);

CREATE TABLE clinic_closure (
    id           UUID PRIMARY KEY,
    closure_date DATE         NOT NULL,
    start_time   TIME,
    end_time     TIME,
    reason_label VARCHAR(100) NOT NULL,
    CHECK ((start_time IS NULL AND end_time IS NULL) OR (start_time IS NOT NULL AND end_time > start_time))
);
CREATE INDEX idx_closure_date ON clinic_closure (closure_date);

CREATE TABLE appointment (
    id          UUID PRIMARY KEY,
    patient_id  UUID        NOT NULL REFERENCES patient_profile (id),
    provider_id UUID        NOT NULL REFERENCES provider (id),
    start_at    TIMESTAMPTZ NOT NULL,
    end_at      TIMESTAMPTZ NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (end_at > start_at),
    -- The database refuses overlapping active bookings for one provider,
    -- even if two requests race past the application-level check.
    CONSTRAINT appointment_no_overlap EXCLUDE USING gist (
        provider_id WITH =,
        tstzrange(start_at, end_at) WITH &&
    ) WHERE (status <> 'CANCELLED')
);
CREATE INDEX idx_appointment_provider_time ON appointment (provider_id, start_at);
CREATE INDEX idx_appointment_patient ON appointment (patient_id, start_at);

CREATE TABLE queue_entry (
    id             UUID PRIMARY KEY,
    appointment_id UUID UNIQUE REFERENCES appointment (id),
    patient_id     UUID        NOT NULL REFERENCES patient_profile (id),
    provider_id    UUID        NOT NULL REFERENCES provider (id),
    queue_date     DATE        NOT NULL,
    checked_in_at  TIMESTAMPTZ,
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_queue_day ON queue_entry (queue_date, provider_id, status);

CREATE TABLE status_event (
    id             UUID PRIMARY KEY,
    queue_entry_id UUID        NOT NULL REFERENCES queue_entry (id),
    from_status    VARCHAR(20),
    to_status      VARCHAR(20) NOT NULL,
    actor_user_id  UUID REFERENCES user_account (id),
    changed_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_status_event_entry ON status_event (queue_entry_id, changed_at);
