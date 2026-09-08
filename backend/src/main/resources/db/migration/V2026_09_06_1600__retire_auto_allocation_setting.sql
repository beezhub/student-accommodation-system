-- GH-207: auto-allocation was gated by two independent switches with no link between them.
--
-- AllocationBatchService.batchAllocateFromWaitingList checked the FF4j `auto-allocation` flag,
-- toggled by a platform admin at /api/admin/features. AutoAllocationService.attemptAutoAllocation,
-- the per-application path, checked this `auto_allocation_enabled` system setting. Neither screen
-- mentioned the other, so an admin who turned the flag on still saw per-application auto-allocation
-- skipped, with nothing to explain why. The feature-flag screen showed the feature as on.
--
-- The FF4j flag is now the single gate for both paths, so this row is a switch that no longer
-- controls anything. Left in place it would be worse than the original defect: a toggle that
-- reports a state the system does not act on.
--
-- Both paths ship disabled either way. The FF4j flag is seeded false by
-- V2026_02_19_2101__seed_reference_data.sql and this setting was seeded false too, so deleting it
-- changes no behaviour on a database where neither has been turned on.
--
-- Tenant override rows are deleted as well as the platform default. The key was platform-only, but
-- GH-127 records that the tenant-scoped endpoint let overrides through before that guard existed,
-- so a database may still carry one.

DELETE FROM system_settings
WHERE setting_key = 'auto_allocation_enabled';
