-- H2-compatible version of the user timezone migration.
ALTER TABLE app_user ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Europe/London';
