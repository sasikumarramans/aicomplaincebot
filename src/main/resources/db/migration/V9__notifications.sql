CREATE TABLE notifications (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID,
    updated_by            UUID,
    deleted_at            TIMESTAMPTZ,
    company_id            UUID NOT NULL REFERENCES companies (id),
    recipient_user_id     UUID NOT NULL REFERENCES users (id),
    channel               VARCHAR(32) NOT NULL,
    trigger_type          VARCHAR(32) NOT NULL,
    related_entity_type   VARCHAR(64),
    related_entity_id     UUID,
    escalation_level      VARCHAR(32),
    status                VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    sent_at               TIMESTAMPTZ,
    acknowledged_at       TIMESTAMPTZ,
    subject               VARCHAR(255) NOT NULL,
    body                  TEXT NOT NULL
);

CREATE INDEX idx_notifications_company_id ON notifications (company_id);
CREATE INDEX idx_notifications_recipient_user_id ON notifications (recipient_user_id);
CREATE INDEX idx_notifications_acknowledged_at ON notifications (acknowledged_at);
CREATE INDEX idx_notifications_related_entity ON notifications (related_entity_type, related_entity_id, trigger_type);
