-- Single-column indexes
CREATE INDEX IF NOT EXISTS idx_app_user_tenant_id ON app_user(tenant_id);
CREATE INDEX IF NOT EXISTS idx_building_tenant_id ON building(tenant_id);
CREATE INDEX IF NOT EXISTS idx_room_tenant_id ON room(tenant_id);
CREATE INDEX IF NOT EXISTS idx_application_tenant_id ON application(tenant_id);
CREATE INDEX IF NOT EXISTS idx_room_allocation_tenant_id ON room_allocation(tenant_id);
CREATE INDEX IF NOT EXISTS idx_staged_allocations_tenant_id ON staged_allocations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_document_tenant_id ON document(tenant_id);
CREATE INDEX IF NOT EXISTS idx_notification_tenant_id ON notification(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_notifications_tenant_id ON user_notifications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_system_settings_tenant_id ON system_settings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_allocation_rules_tenant_id ON allocation_rules(tenant_id);
CREATE INDEX IF NOT EXISTS idx_waiting_list_entry_tenant_id ON waiting_list_entry(tenant_id);
CREATE INDEX IF NOT EXISTS idx_activity_logs_tenant_id ON activity_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_academic_year_tenant_id ON academic_year(tenant_id);

-- Composite indexes
CREATE INDEX IF NOT EXISTS idx_application_tenant_status ON application(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_room_tenant_building ON room(tenant_id, building_id);
CREATE INDEX IF NOT EXISTS idx_room_allocation_tenant_status ON room_allocation(tenant_id, status);
