-- V3: Add approval status field to users
-- Mentors start as PENDING_APPROVAL; students and admins start as ACTIVE

ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
