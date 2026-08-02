CREATE TABLE company_signup_requests (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID,
    updated_by           UUID,
    deleted_at           TIMESTAMPTZ,
    company_name         VARCHAR(255) NOT NULL,
    industry_id          UUID REFERENCES industry_types (id),
    registered_address   VARCHAR(500),
    gst_number           VARCHAR(64),
    admin_full_name      VARCHAR(255) NOT NULL,
    admin_email          VARCHAR(255) NOT NULL,
    phone_number         VARCHAR(32),
    message              TEXT,
    status               VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id  UUID REFERENCES users (id),
    reviewed_at          TIMESTAMPTZ,
    rejection_reason     TEXT,
    created_company_id   UUID REFERENCES companies (id)
);

CREATE INDEX idx_company_signup_requests_status ON company_signup_requests (status);
