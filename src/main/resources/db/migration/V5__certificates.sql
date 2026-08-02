CREATE TABLE certificates (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by             UUID,
    updated_by             UUID,
    deleted_at             TIMESTAMPTZ,
    company_id             UUID NOT NULL REFERENCES companies (id),
    plant_id               UUID REFERENCES plants (id),
    category_id            UUID NOT NULL REFERENCES certificate_categories (id),
    certificate_name       VARCHAR(255) NOT NULL,
    certificate_number     VARCHAR(255),
    issuing_authority      VARCHAR(255),
    issue_date             DATE,
    expiry_date            DATE,
    license_number         VARCHAR(255),
    has_qr_code            BOOLEAN NOT NULL DEFAULT FALSE,
    has_digital_signature  BOOLEAN NOT NULL DEFAULT FALSE,
    current_version_id     UUID,
    status                 VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    expiry_bucket          VARCHAR(32) NOT NULL DEFAULT 'NOT_TRACKED',
    uploaded_by_user_id    UUID NOT NULL REFERENCES users (id),
    reviewed_by_user_id    UUID REFERENCES users (id),
    reviewed_at            TIMESTAMPTZ,
    ai_confidence_score    DOUBLE PRECISION
);

CREATE INDEX idx_certificates_company_id ON certificates (company_id);
CREATE INDEX idx_certificates_category_id ON certificates (category_id);
CREATE INDEX idx_certificates_plant_id ON certificates (plant_id);
CREATE INDEX idx_certificates_expiry_bucket ON certificates (expiry_bucket);
CREATE INDEX idx_certificates_expiry_date ON certificates (expiry_date);

CREATE TABLE certificate_versions (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                UUID,
    updated_by                UUID,
    deleted_at                TIMESTAMPTZ,
    company_id                UUID NOT NULL REFERENCES companies (id),
    certificate_id             UUID NOT NULL REFERENCES certificates (id),
    version_number             INT NOT NULL,
    file_storage_key           VARCHAR(512) NOT NULL,
    original_file_name         VARCHAR(255) NOT NULL,
    mime_type                  VARCHAR(128) NOT NULL,
    file_size_bytes             BIGINT NOT NULL DEFAULT 0,
    ocr_raw_text                TEXT,
    ai_extracted_fields_json    TEXT,
    uploaded_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    uploaded_by_user_id         UUID NOT NULL REFERENCES users (id),
    is_archived                 BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_certificate_versions_certificate_id ON certificate_versions (certificate_id);
CREATE INDEX idx_certificate_versions_company_id ON certificate_versions (company_id);

ALTER TABLE certificates
    ADD CONSTRAINT fk_certificates_current_version
    FOREIGN KEY (current_version_id) REFERENCES certificate_versions (id);
