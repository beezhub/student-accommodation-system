-- TASK-2.05: Add intake_period table for per-building application intake windows
-- Supports FIXED (open/close date window) and ROLLING (always-open) modes

CREATE TABLE IF NOT EXISTS intake_period (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES building(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    name VARCHAR(100) NOT NULL,
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('FIXED', 'ROLLING')),
    open_date TIMESTAMP,
    close_date TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_intake_fixed_dates CHECK (
        mode != 'FIXED' OR (open_date IS NOT NULL AND close_date IS NOT NULL)
    ),
    CONSTRAINT chk_intake_close_after_open CHECK (
        close_date IS NULL OR open_date IS NULL OR close_date > open_date
    )
);

CREATE INDEX IF NOT EXISTS idx_intake_period_building ON intake_period(building_id);
CREATE INDEX IF NOT EXISTS idx_intake_period_tenant ON intake_period(tenant_id);
