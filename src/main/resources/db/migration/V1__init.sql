-- Baseline: enable extensions used by later migrations (UUID generation, etc).
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
