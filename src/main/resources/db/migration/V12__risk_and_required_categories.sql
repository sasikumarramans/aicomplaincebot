CREATE TABLE risk_assessments (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                  UUID,
    updated_by                  UUID,
    deleted_at                  TIMESTAMPTZ,
    company_id                  UUID NOT NULL REFERENCES companies (id),
    plant_id                    UUID REFERENCES plants (id),
    risk_level                  VARCHAR(16) NOT NULL,
    risk_score                  NUMERIC(5,2) NOT NULL,
    reasoning_text              TEXT,
    contributing_signals_json   TEXT,
    assessed_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_risk_assessments_company_id ON risk_assessments (company_id);
CREATE INDEX idx_risk_assessments_assessed_at ON risk_assessments (assessed_at);

CREATE TABLE facility_type_required_categories (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     UUID,
    updated_by     UUID,
    deleted_at     TIMESTAMPTZ,
    company_id     UUID NOT NULL REFERENCES companies (id),
    facility_type  VARCHAR(128) NOT NULL,
    category_id    UUID NOT NULL REFERENCES certificate_categories (id)
);

CREATE INDEX idx_facility_type_required_categories_company_id ON facility_type_required_categories (company_id);
CREATE UNIQUE INDEX uq_facility_type_required_categories ON facility_type_required_categories (company_id, facility_type, category_id);
