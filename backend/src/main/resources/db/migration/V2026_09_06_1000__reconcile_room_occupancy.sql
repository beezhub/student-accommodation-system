-- GH-136: room.current_occupancy had drifted away from the live allocation count.
--
-- It was maintained as a blind delta on the in-memory value while every capacity decision read the
-- count, and the two had no relationship. The delta could go negative, because the status machine
-- permits a self-transition and a second check-out decremented again. It overwrote MAINTENANCE. And
-- account deletion hard-deleted allocations without touching it at all.
--
-- The code now derives occupancy from the count on every write, so it converges from here. This
-- reconciles what is already stored, because two cases cannot self-heal: a room stranded at FULL is
-- excluded by the scoring strategies, so no future allocation ever triggers a recount, and the
-- admin occupancy dashboards read this column directly.

UPDATE room r
SET current_occupancy = COALESCE((
        SELECT COUNT(*) FROM room_allocation ra
         WHERE ra.room_id = r.id
           AND ra.status IN ('OFFERED', 'ACCEPTED', 'ACTIVE')), 0);

-- Status follows from the reconciled count. MAINTENANCE is left alone: it is an administrative
-- decision about the room rather than a statement about how many students are in it.
UPDATE room r
SET status = CASE
        WHEN r.current_occupancy = 0 THEN 'AVAILABLE'
        WHEN r.capacity IS NOT NULL AND r.current_occupancy >= r.capacity THEN 'FULL'
        ELSE 'OCCUPIED'
    END
WHERE r.status IS DISTINCT FROM 'MAINTENANCE';
