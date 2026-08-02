CREATE TABLE companies (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_by          UUID,
    deleted_at          TIMESTAMPTZ,
    name                VARCHAR(255) NOT NULL,
    gst_number          VARCHAR(32),
    pan_number          VARCHAR(16),
    cin_number          VARCHAR(32),
    registered_address  TEXT,
    industry_type       VARCHAR(128),
    subscription_status VARCHAR(32) NOT NULL DEFAULT 'TRIAL',
    active              BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE plants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ,
    company_id      UUID NOT NULL REFERENCES companies (id),
    name            VARCHAR(255) NOT NULL,
    address         TEXT,
    gst_number      VARCHAR(32),
    plant_type      VARCHAR(64),
    contact_person  VARCHAR(255),
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_plants_company_id ON plants (company_id);

CREATE TABLE users (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         UUID,
    updated_by         UUID,
    deleted_at         TIMESTAMPTZ,
    company_id         UUID REFERENCES companies (id),
    email              VARCHAR(255) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    full_name          VARCHAR(255) NOT NULL,
    role               VARCHAR(32) NOT NULL,
    plant_id           UUID REFERENCES plants (id),
    department         VARCHAR(128),
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    is_temporary       BOOLEAN NOT NULL DEFAULT FALSE,
    access_expires_at  TIMESTAMPTZ,
    last_login_at      TIMESTAMPTZ
);

CREATE INDEX idx_users_company_id ON users (company_id);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_by  UUID,
    deleted_at  TIMESTAMPTZ,
    user_id     UUID NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by    UUID,
    updated_by    UUID,
    deleted_at    TIMESTAMPTZ,
    company_id    UUID,
    user_id       UUID,
    action        VARCHAR(128) NOT NULL,
    entity_type   VARCHAR(128),
    entity_id     UUID,
    details_json  TEXT,
    ip_address    VARCHAR(64),
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_company_id ON audit_logs (company_id);
CREATE INDEX idx_audit_logs_occurred_at ON audit_logs (occurred_at);
