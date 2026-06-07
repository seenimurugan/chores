-- H2-compatible version of V8: per-kid reminder time.
ALTER TABLE app_user
    ADD COLUMN reminder_time TIME NOT NULL DEFAULT '06:00:00';
