-- Phase 1 of at-risk reminder feature: contact info on kids + tracking fields on tasks.

ALTER TABLE app_user
    ADD COLUMN email            VARCHAR(255) NULL,
    ADD COLUMN telegram_chat_id BIGINT       NULL;

ALTER TABLE task
    ADD COLUMN weekly_target    INT  NULL,
    ADD COLUMN remind_lead_days INT  NOT NULL DEFAULT 0;
