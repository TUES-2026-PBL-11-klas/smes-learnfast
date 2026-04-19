-- V2: Add mentor-specific profile fields to users table

ALTER TABLE users ADD COLUMN IF NOT EXISTS diploma_info       VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS years_of_experience INTEGER;
ALTER TABLE users ADD COLUMN IF NOT EXISTS field_of_expertise  VARCHAR(200);
ALTER TABLE users ADD COLUMN IF NOT EXISTS motivation_to_teach VARCHAR(1000);
