-- CF-112: audit trail must record the authenticated principal (user id + display name)
-- instead of a client-supplied free-text "changed by" string.

-- 1. Give every user a display name so the audit trail has something human-readable
--    to show alongside the immutable user id.
ALTER TABLE users ADD COLUMN display_name VARCHAR(120);

UPDATE users SET display_name = 'Pat Iyer' WHERE email = 'planner.pat@careflow.local';
UPDATE users SET display_name = 'Jamie Fernandes' WHERE email = 'tech.jamie@careflow.local';
UPDATE users SET display_name = 'Morgan Bose' WHERE email = 'viewer.morgan@careflow.local';
UPDATE users SET display_name = 'Riley D''Souza' WHERE email = 'admin.riley@careflow.local';

-- Fallback for any other pre-existing accounts: derive a readable name from the email
-- local-part so the NOT NULL constraint below can never fail on unexpected rows.
UPDATE users
SET display_name = initcap(replace(split_part(email, '@', 1), '.', ' '))
WHERE display_name IS NULL;

ALTER TABLE users ALTER COLUMN display_name SET NOT NULL;

-- 2. Replace the free-text changed_by column on the audit trail with a real reference
--    to the acting user, plus a denormalized display name captured at the time of the
--    change (so history still reads sensibly if the user is later renamed or removed).
ALTER TABLE work_order_status_history ADD COLUMN changed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE work_order_status_history ADD COLUMN changed_by_display_name VARCHAR(120);

-- Historic/demo rows were written by system integrations and seed scripts, not real
-- authenticated users, so there is no user id to backfill for them — only the label.
UPDATE work_order_status_history SET changed_by_display_name = changed_by;

ALTER TABLE work_order_status_history ALTER COLUMN changed_by_display_name SET NOT NULL;
ALTER TABLE work_order_status_history DROP COLUMN changed_by;

CREATE INDEX idx_work_order_history_actor ON work_order_status_history(changed_by_user_id);