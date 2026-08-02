CREATE TABLE audit_folders (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID,
    updated_by            UUID,
    deleted_at            TIMESTAMPTZ,
    company_id            UUID NOT NULL REFERENCES companies (id),
    generated_by_user_id  UUID NOT NULL REFERENCES users (id),
    generated_at          TIMESTAMPTZ,
    date_range_start      DATE,
    date_range_end        DATE,
    file_storage_key      VARCHAR(512),
    status                VARCHAR(32) NOT NULL DEFAULT 'GENERATING',
    failure_reason        TEXT,
    expires_at            TIMESTAMPTZ
);

CREATE INDEX idx_audit_folders_company_id ON audit_folders (company_id);
