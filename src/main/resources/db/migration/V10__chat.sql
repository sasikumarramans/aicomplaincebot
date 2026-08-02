CREATE TABLE chat_conversations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       UUID,
    updated_by       UUID,
    deleted_at       TIMESTAMPTZ,
    company_id       UUID NOT NULL REFERENCES companies (id),
    user_id          UUID NOT NULL REFERENCES users (id),
    title            VARCHAR(255),
    last_message_at  TIMESTAMPTZ
);

CREATE INDEX idx_chat_conversations_company_id ON chat_conversations (company_id);
CREATE INDEX idx_chat_conversations_user_id ON chat_conversations (user_id);

CREATE TABLE chat_messages (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       UUID,
    updated_by       UUID,
    deleted_at       TIMESTAMPTZ,
    company_id       UUID NOT NULL REFERENCES companies (id),
    conversation_id  UUID NOT NULL REFERENCES chat_conversations (id),
    role             VARCHAR(16) NOT NULL,
    content          TEXT NOT NULL,
    context_json     TEXT
);

CREATE INDEX idx_chat_messages_conversation_id ON chat_messages (conversation_id);
