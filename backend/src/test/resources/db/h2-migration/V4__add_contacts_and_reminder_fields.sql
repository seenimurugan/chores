-- H2-compatible version of the contacts + reminder fields migration.
-- H2 requires one ADD COLUMN per ALTER TABLE statement.
ALTER TABLE app_user ADD COLUMN email VARCHAR(255) NULL;
ALTER TABLE app_user ADD COLUMN telegram_chat_id BIGINT NULL;

ALTER TABLE task ADD COLUMN weekly_target INT NULL;
ALTER TABLE task ADD COLUMN remind_lead_days INT NOT NULL DEFAULT 0;
