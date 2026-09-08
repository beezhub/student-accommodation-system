-- GH-152: `application_review_response_days` was seeded by V2026_04_15_0001 and read by nothing
-- in Java. No job, no guard and no notification consults it, so no application was ever escalated
-- or flagged for sitting past it.
--
-- It was not entirely inert, which is worse than if it had been: the student dashboard read it and
-- told students "You'll receive an update within N business days." That is a service level the
-- system does not keep and has no mechanism to keep — the same shape as the decline limit the UI
-- promised and nothing enforced.
--
-- Removed rather than implemented. Wiring it up means deciding what happens when the deadline
-- passes — remind whom, escalate to whom, and what an administrator is expected to do about it —
-- and that is a feature to design, not a value to read. The dashboard already falls back to
-- "We'll notify you as soon as there's an update", which is true.
--
-- Tenant overrides are deleted alongside the platform default: a tenant row for a key nothing
-- reads is the same problem one level down.

DELETE FROM system_settings WHERE setting_key = 'application_review_response_days';
