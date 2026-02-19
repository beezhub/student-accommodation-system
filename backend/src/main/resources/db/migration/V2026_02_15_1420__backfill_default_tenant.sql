-- Step 1: Insert default tenant (idempotent)
INSERT INTO tenant (id, name, config, is_active)
VALUES (1, 'Default Tenant', '{"offer_expiry_days": 7, "max_decline_count": 2}'::jsonb, true)
ON CONFLICT (name) DO NOTHING;

-- Step 2: Backfill all tables (idempotent via WHERE tenant_id IS NULL)
DO $$
DECLARE
    default_tenant_id BIGINT;
    row_count_app_user BIGINT;
    row_count_building BIGINT;
    row_count_room BIGINT;
    row_count_application BIGINT;
    row_count_room_allocation BIGINT;
    row_count_staged_allocations BIGINT;
    row_count_document BIGINT;
    row_count_notification BIGINT;
    row_count_user_notifications BIGINT;
    row_count_system_settings BIGINT;
    row_count_allocation_rules BIGINT;
    row_count_waiting_list_entry BIGINT;
    row_count_activity_logs BIGINT;
    row_count_academic_year BIGINT;
BEGIN
    SELECT id INTO default_tenant_id FROM tenant WHERE name = 'Default Tenant';

    UPDATE app_user SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_app_user = ROW_COUNT;

    UPDATE building SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_building = ROW_COUNT;

    UPDATE room SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_room = ROW_COUNT;

    UPDATE application SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_application = ROW_COUNT;

    UPDATE room_allocation SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_room_allocation = ROW_COUNT;

    UPDATE staged_allocations SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_staged_allocations = ROW_COUNT;

    UPDATE document SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_document = ROW_COUNT;

    UPDATE notification SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_notification = ROW_COUNT;

    UPDATE user_notifications SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_user_notifications = ROW_COUNT;

    UPDATE system_settings SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_system_settings = ROW_COUNT;

    UPDATE allocation_rules SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_allocation_rules = ROW_COUNT;

    UPDATE waiting_list_entry SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_waiting_list_entry = ROW_COUNT;

    UPDATE activity_logs SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_activity_logs = ROW_COUNT;

    UPDATE academic_year SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_academic_year = ROW_COUNT;

    RAISE NOTICE 'Backfill completed for default tenant (ID: %)', default_tenant_id;
    RAISE NOTICE '  app_user: % rows updated', row_count_app_user;
    RAISE NOTICE '  building: % rows updated', row_count_building;
    RAISE NOTICE '  room: % rows updated', row_count_room;
    RAISE NOTICE '  application: % rows updated', row_count_application;
    RAISE NOTICE '  room_allocation: % rows updated', row_count_room_allocation;
    RAISE NOTICE '  staged_allocations: % rows updated', row_count_staged_allocations;
    RAISE NOTICE '  document: % rows updated', row_count_document;
    RAISE NOTICE '  notification: % rows updated', row_count_notification;
    RAISE NOTICE '  user_notifications: % rows updated', row_count_user_notifications;
    RAISE NOTICE '  system_settings: % rows updated', row_count_system_settings;
    RAISE NOTICE '  allocation_rules: % rows updated', row_count_allocation_rules;
    RAISE NOTICE '  waiting_list_entry: % rows updated', row_count_waiting_list_entry;
    RAISE NOTICE '  activity_logs: % rows updated', row_count_activity_logs;
    RAISE NOTICE '  academic_year: % rows updated', row_count_academic_year;
END $$;

-- Step 3: Verify no NULLs remain
DO $$
DECLARE null_count INT;
BEGIN
    SELECT COUNT(*) INTO null_count FROM app_user WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'app_user has % rows with NULL tenant_id', null_count; END IF;
    
    SELECT COUNT(*) INTO null_count FROM building WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'building has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM room WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'room has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM application WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'application has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM room_allocation WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'room_allocation has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM staged_allocations WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'staged_allocations has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM document WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'document has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM notification WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'notification has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM user_notifications WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'user_notifications has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM system_settings WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'system_settings has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM allocation_rules WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'allocation_rules has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM waiting_list_entry WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'waiting_list_entry has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM activity_logs WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'activity_logs has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM academic_year WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'academic_year has % rows with NULL tenant_id', null_count; END IF;
END $$;

-- Step 4: Add NOT NULL constraints
ALTER TABLE app_user ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE building ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE room ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE application ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE room_allocation ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE staged_allocations ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE document ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE notification ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE user_notifications ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE system_settings ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE allocation_rules ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE waiting_list_entry ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE activity_logs ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE academic_year ALTER COLUMN tenant_id SET NOT NULL;
