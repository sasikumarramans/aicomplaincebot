CREATE TABLE employees (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     UUID,
    updated_by     UUID,
    deleted_at     TIMESTAMPTZ,
    company_id     UUID NOT NULL REFERENCES companies (id),
    plant_id       UUID REFERENCES plants (id),
    employee_code  VARCHAR(64),
    full_name      VARCHAR(255) NOT NULL,
    department     VARCHAR(128),
    designation    VARCHAR(128),
    email          VARCHAR(255),
    active         BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_employees_company_id ON employees (company_id);

CREATE TABLE employee_certifications (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID,
    updated_by            UUID,
    deleted_at            TIMESTAMPTZ,
    company_id            UUID NOT NULL REFERENCES companies (id),
    employee_id           UUID NOT NULL REFERENCES employees (id),
    certification_type    VARCHAR(32) NOT NULL,
    file_storage_key      VARCHAR(512),
    original_file_name    VARCHAR(255),
    mime_type             VARCHAR(128),
    issue_date            DATE,
    expiry_date           DATE,
    expiry_bucket         VARCHAR(32) NOT NULL DEFAULT 'NOT_TRACKED',
    uploaded_by_user_id   UUID NOT NULL REFERENCES users (id)
);

CREATE INDEX idx_employee_certifications_company_id ON employee_certifications (company_id);
CREATE INDEX idx_employee_certifications_employee_id ON employee_certifications (employee_id);
CREATE INDEX idx_employee_certifications_expiry_bucket ON employee_certifications (expiry_bucket);
