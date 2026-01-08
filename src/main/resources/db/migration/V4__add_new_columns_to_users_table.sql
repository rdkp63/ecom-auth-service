-- =========================================
-- Add profile & account status columns
-- =========================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN,
    ADD COLUMN IF NOT EXISTS account_non_locked BOOLEAN,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;

-- =========================================
-- Set safe default values for existing rows
-- =========================================

UPDATE users
SET
    enabled = TRUE
WHERE enabled IS NULL;

UPDATE users
SET
    account_non_locked = TRUE
WHERE account_non_locked IS NULL;

UPDATE users
SET
    full_name = email
WHERE full_name IS NULL;

UPDATE users
SET
    updated_at = created_at
WHERE updated_at IS NULL;

-- =========================================
-- Enforce NOT NULL constraints
-- =========================================

ALTER TABLE users
    ALTER COLUMN enabled SET NOT NULL,
    ALTER COLUMN account_non_locked SET NOT NULL;

-- =========================================
-- Add default values for future inserts
-- =========================================

ALTER TABLE users
    ALTER COLUMN enabled SET DEFAULT TRUE,
    ALTER COLUMN account_non_locked SET DEFAULT TRUE,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
