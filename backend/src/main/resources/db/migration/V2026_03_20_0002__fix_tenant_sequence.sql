-- Fix the tenant id sequence so that auto-generated IDs start above the
-- highest explicitly-inserted seed row.
--
-- The seed migration (V2026_02_19_2101) inserts tenant(id=1) with a literal
-- value, bypassing the BIGSERIAL sequence.  Without this fix the sequence
-- still sits at 1 and the first Hibernate INSERT generates id=1, colliding
-- with the seed row.
--
-- setval(..., false) means the NEXT nextval() call returns the given value
-- (i.e., the current max + 1), rather than the value after it.

SELECT setval(
    pg_get_serial_sequence('tenant', 'id'),
    COALESCE((SELECT MAX(id) FROM tenant), 1) + 1,
    false
);
