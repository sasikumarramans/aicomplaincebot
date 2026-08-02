CREATE TABLE vendors (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         UUID,
    updated_by         UUID,
    deleted_at         TIMESTAMPTZ,
    company_id         UUID NOT NULL REFERENCES companies (id),
    name               VARCHAR(255) NOT NULL,
    gst_number         VARCHAR(32),
    pan_number         VARCHAR(16),
    contact_email      VARCHAR(255),
    contact_phone      VARCHAR(32),
    compliance_status  VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    active             BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_vendors_company_id ON vendors (company_id);

CREATE TABLE vendor_documents (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID,
    updated_by           UUID,
    deleted_at           TIMESTAMPTZ,
    company_id           UUID NOT NULL REFERENCES companies (id),
    vendor_id            UUID NOT NULL REFERENCES vendors (id),
    category_id          UUID NOT NULL REFERENCES certificate_categories (id),
    file_storage_key     VARCHAR(512) NOT NULL,
    original_file_name   VARCHAR(255) NOT NULL,
    mime_type            VARCHAR(128) NOT NULL,
    issue_date           DATE,
    expiry_date          DATE,
    status               VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    uploaded_by_user_id  UUID NOT NULL REFERENCES users (id),
    reviewed_by_user_id  UUID REFERENCES users (id),
    reviewed_at          TIMESTAMPTZ,
    review_notes         TEXT
);

CREATE INDEX idx_vendor_documents_company_id ON vendor_documents (company_id);
CREATE INDEX idx_vendor_documents_vendor_id ON vendor_documents (vendor_id);
