-- Phase 1 timezone: each kid stores an IANA timezone for localised reminder scheduling.
ALTER TABLE app_user
    ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Europe/London';
