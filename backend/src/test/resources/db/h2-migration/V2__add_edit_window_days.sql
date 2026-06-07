-- Same as production; no Postgres-specific types.
ALTER TABLE app_user
    ADD COLUMN edit_window_days INT NOT NULL DEFAULT 14;
