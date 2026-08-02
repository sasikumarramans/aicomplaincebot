CREATE TABLE industry_types (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_by  UUID,
    deleted_at  TIMESTAMPTZ,
    name        VARCHAR(128) NOT NULL UNIQUE,
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE companies DROP COLUMN industry_type;
ALTER TABLE companies ADD COLUMN industry_id UUID REFERENCES industry_types (id);
