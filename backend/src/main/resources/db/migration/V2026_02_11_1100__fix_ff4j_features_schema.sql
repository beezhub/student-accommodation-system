-- Fix FF4J features table schema - add missing clazz column
-- This column is required by FF4J 1.9.x for feature storage

ALTER TABLE ff4j_features
ADD COLUMN IF NOT EXISTS clazz VARCHAR(255);

-- Update existing features to use the default FlippingStrategy class if they have a strategy
UPDATE ff4j_features
SET clazz = 'org.ff4j.strategy.' || strategy
WHERE strategy IS NOT NULL AND clazz IS NULL;
