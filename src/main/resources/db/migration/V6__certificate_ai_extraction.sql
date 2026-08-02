ALTER TABLE certificates ALTER COLUMN category_id DROP NOT NULL;
ALTER TABLE certificates ALTER COLUMN certificate_name DROP NOT NULL;

ALTER TABLE certificate_versions
    ADD COLUMN processing_status VARCHAR(32) NOT NULL DEFAULT 'NOT_APPLICABLE';
ALTER TABLE certificate_versions
    ADD COLUMN processing_error TEXT;
