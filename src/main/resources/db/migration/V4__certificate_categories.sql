-- No rows are pre-seeded here on purpose: categories are fully custom per company,
-- so the same schema serves manufacturing, software, healthcare, or any other vertical.
CREATE TABLE certificate_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_by  UUID,
    deleted_at  TIMESTAMPTZ,
    company_id  UUID NOT NULL REFERENCES companies (id),
    name        VARCHAR(255) NOT NULL,
    group_label VARCHAR(128),
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_certificate_categories_company_id ON certificate_categories (company_id);
CREATE UNIQUE INDEX uq_certificate_categories_company_name ON certificate_categories (company_id, lower(name));
