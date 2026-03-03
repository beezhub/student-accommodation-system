CREATE TABLE IF NOT EXISTS tenant (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    domain VARCHAR(255),
    config JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_name UNIQUE (name),
    CONSTRAINT uk_tenant_domain UNIQUE (domain)
);

CREATE INDEX IF NOT EXISTS idx_tenant_is_active ON tenant(is_active);

COMMENT ON TABLE tenant IS 'Accommodation providers (landlords) for multi-tenant SaaS deployment';
COMMENT ON COLUMN tenant.config IS 'Tenant-specific configuration overrides (JSON): offer_expiry_days, max_decline_count, required_document_types';
