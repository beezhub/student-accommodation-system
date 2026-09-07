-- GH-152: custom_application_field and custom_field_answer were added by V2026_02_24_0022 with
-- entities and repositories, and never given a way in or out.
--
-- There is no service, no controller, and `ApplicationRequest` has no answers field — so a
-- building owner could not define a field, a student could not answer one, and nothing read an
-- answer. Half a feature, dormant since it was added.
--
-- Removed rather than finished. Completing it is a feature: an admin screen to define fields per
-- building, validation per field type, answers on the submission request, and somewhere for a
-- reviewer to see them. That is worth building when a tenant asks for it, and the tables are
-- cheap to recreate then — this migration is the shape of the schema to restore.
--
-- Dropped in dependency order: the answer table references the field table.
--
-- No data is at risk. Both tables have always been unreachable from the application, so any row
-- in them would have to have been inserted by hand.

DROP TABLE IF EXISTS custom_field_answer;
DROP TABLE IF EXISTS custom_application_field;
