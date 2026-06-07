-- Phase 5: per-kid configurable reminder time.
-- Scheduler gates each kid's send on their own reminderTime (in their timezone),
-- rather than a single global chores.reminder.send-time property.
-- Default 06:00 matches the previous global default, so no existing kid's
-- reminder schedule changes unless an admin explicitly edits it.
ALTER TABLE app_user
    ADD COLUMN reminder_time TIME NOT NULL DEFAULT '06:00:00';
