-- Drop unused meal plan and budget columns per policy: students do not get meals and do not pay accommodation
ALTER TABLE application
    DROP COLUMN IF EXISTS preferred_meal_plan,
    DROP COLUMN IF EXISTS preferred_budget;

