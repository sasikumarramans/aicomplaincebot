CREATE TABLE compliance_scores (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                  UUID,
    updated_by                  UUID,
    deleted_at                  TIMESTAMPTZ,
    company_id                  UUID NOT NULL REFERENCES companies (id),
    category_group              VARCHAR(128),
    score_value                 NUMERIC(5,2) NOT NULL,
    calculated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    contributing_factors_json   TEXT
);

CREATE INDEX idx_compliance_scores_company_id ON compliance_scores (company_id);
CREATE INDEX idx_compliance_scores_calculated_at ON compliance_scores (calculated_at);
