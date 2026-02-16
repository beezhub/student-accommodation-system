-- Add tenant_id columns (nullable initially)
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE building ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE room ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE application ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE room_allocation ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE staged_allocations ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE document ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE user_notifications ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE system_settings ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE allocation_rules ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE waiting_list_entry ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE academic_year ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- Add FK constraints
ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE building
    ADD CONSTRAINT fk_building_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE room
    ADD CONSTRAINT fk_room_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE application
    ADD CONSTRAINT fk_application_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE room_allocation
    ADD CONSTRAINT fk_room_allocation_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE staged_allocations
    ADD CONSTRAINT fk_staged_allocations_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE document
    ADD CONSTRAINT fk_document_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE notification
    ADD CONSTRAINT fk_notification_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE user_notifications
    ADD CONSTRAINT fk_user_notifications_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE system_settings
    ADD CONSTRAINT fk_system_settings_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE allocation_rules
    ADD CONSTRAINT fk_allocation_rules_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE waiting_list_entry
    ADD CONSTRAINT fk_waiting_list_entry_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE activity_logs
    ADD CONSTRAINT fk_activity_logs_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE academic_year
    ADD CONSTRAINT fk_academic_year_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

-- Add comments
COMMENT ON COLUMN app_user.tenant_id IS 'FK to tenant table; admin users scoped to tenant';
COMMENT ON COLUMN building.tenant_id IS 'FK to tenant table; building owner (buildings also have institution_id for shared reference)';
COMMENT ON COLUMN room.tenant_id IS 'FK to tenant table; belongs to building';
COMMENT ON COLUMN application.tenant_id IS 'FK to tenant table; student application scoped to tenant';
COMMENT ON COLUMN room_allocation.tenant_id IS 'FK to tenant table';
COMMENT ON COLUMN staged_allocations.tenant_id IS 'FK to tenant table';
COMMENT ON COLUMN document.tenant_id IS 'FK to tenant table';
COMMENT ON COLUMN notification.tenant_id IS 'FK to tenant table';
COMMENT ON COLUMN user_notifications.tenant_id IS 'FK to tenant table';
COMMENT ON COLUMN system_settings.tenant_id IS 'FK to tenant table; tenant-specific settings';

COMMENT ON COLUMN allocation_rules.tenant_id IS 'FK to tenant table';
COMMENT ON COLUMN waiting_list_entry.tenant_id IS 'FK to tenant table';
COMMENT ON COLUMN activity_logs.tenant_id IS 'FK to tenant table';
COMMENT ON COLUMN academic_year.tenant_id IS 'FK to tenant table';
