-- Add tenant_id to building_image
ALTER TABLE building_image ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

ALTER TABLE building_image
    ADD CONSTRAINT fk_building_image_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_building_image_tenant_id ON building_image(tenant_id);

-- Backfill default tenant if any images exist
DO $$
DECLARE
    default_tenant_id BIGINT;
    null_count INTEGER;
BEGIN
    SELECT id INTO default_tenant_id FROM tenant WHERE name = 'Default Tenant';
    
    IF default_tenant_id IS NOT NULL THEN
        UPDATE building_image SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
        
        SELECT COUNT(*) INTO null_count FROM building_image WHERE tenant_id IS NULL;
        IF null_count > 0 THEN 
            RAISE EXCEPTION 'building_image has % rows with NULL tenant_id after backfill', null_count; 
        END IF;
        
        ALTER TABLE building_image ALTER COLUMN tenant_id SET NOT NULL;
    END IF;
END $$;
