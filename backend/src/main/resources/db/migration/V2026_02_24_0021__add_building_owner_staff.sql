-- TASK-2.06: Add building_owner and building_staff tables.
-- Building owner: profile for users with BUILDING_OWNER role (v2.0 replacement for LANDLORD).
-- Building staff: staff members invited by a building owner with scoped permissions.

CREATE TABLE IF NOT EXISTS building_owner (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES app_user(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    business_name VARCHAR(200) NOT NULL,
    contact_phone VARCHAR(20),
    region VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    rejection_reason VARCHAR(500),
    approved_by BIGINT REFERENCES app_user(id),
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_building_owner_user UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_building_owner_tenant ON building_owner(tenant_id);
CREATE INDEX IF NOT EXISTS idx_building_owner_status ON building_owner(status);

CREATE TABLE IF NOT EXISTS building_staff (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    owner_id BIGINT NOT NULL REFERENCES building_owner(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    permission_set VARCHAR(20) NOT NULL CHECK (permission_set IN ('FULL', 'RESTRICTED')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    invited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP,
    invitation_expires_at TIMESTAMP,
    CONSTRAINT uq_staff_user_owner UNIQUE (user_id, owner_id)
);

CREATE INDEX IF NOT EXISTS idx_building_staff_owner ON building_staff(owner_id);
CREATE INDEX IF NOT EXISTS idx_building_staff_tenant ON building_staff(tenant_id);
