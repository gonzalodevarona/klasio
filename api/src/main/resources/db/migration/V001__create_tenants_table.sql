CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE tenants (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slug            VARCHAR(60)  NOT NULL,
    name            VARCHAR(150) NOT NULL,
    sport_discipline VARCHAR(100) NOT NULL,
    logo_key        VARCHAR(255),
    contact_email   VARCHAR(255) NOT NULL,
    contact_phone   VARCHAR(30),
    contact_address VARCHAR(500),
    status          VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID         NOT NULL,
    deactivated_at  TIMESTAMPTZ,
    deactivated_by  UUID,

    CONSTRAINT uq_tenants_slug UNIQUE (slug),
    CONSTRAINT chk_tenants_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_name ON tenants(name);
