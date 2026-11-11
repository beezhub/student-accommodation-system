-- GH-494 step 0, GH-439: academic years belong to one company and, optionally, one institution.
--
-- An empty institution_id means every institution the company serves ("All institutions"), so
-- every existing year keeps working unchanged.
ALTER TABLE academic_year ADD COLUMN institution_id BIGINT;

ALTER TABLE academic_year
    ADD CONSTRAINT fk_academic_year_institution
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT;

CREATE INDEX idx_academic_year_institution_id ON academic_year(institution_id);

COMMENT ON COLUMN academic_year.institution_id IS
    'Institution whose calendar this year follows. NULL means every institution the company serves';

-- Names were unique across every company, so a company could not reuse a name another company
-- had taken (GH-439). They are now unique per company and institution. NULLS NOT DISTINCT makes two
-- "All institutions" years of the same company with the same name collide as well.
ALTER TABLE academic_year DROP CONSTRAINT IF EXISTS academic_year_name_key;

ALTER TABLE academic_year
    ADD CONSTRAINT uq_academic_year_tenant_institution_name
    UNIQUE NULLS NOT DISTINCT (tenant_id, institution_id, name);

-- The current year is now found by date, per company and institution. This index allowed one
-- current row across the whole platform. is_current itself stays until group C drops it, so no
-- running instance still reading it fails during a deploy.
DROP INDEX IF EXISTS idx_academic_year_current;

COMMENT ON COLUMN academic_year.is_current IS 'Retired (GH-494): no longer read or written. Dropped in a later release';
