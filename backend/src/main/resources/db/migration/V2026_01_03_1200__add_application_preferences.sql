-- Add preference fields to application
ALTER TABLE application
    ADD COLUMN IF NOT EXISTS preferred_accommodation_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS preferred_move_in_date DATE,
    ADD COLUMN IF NOT EXISTS preferred_duration_months INTEGER,
    ADD COLUMN IF NOT EXISTS preferred_meal_plan VARCHAR(50),
    ADD COLUMN IF NOT EXISTS preferred_room_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS preferred_budget NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS special_requirements VARCHAR(500),
    ADD COLUMN IF NOT EXISTS institution_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS course_name VARCHAR(150);

