-- Phase 2: notification log table — one row per kid per channel per send attempt.
CREATE TABLE notification_log (
    id       BIGSERIAL PRIMARY KEY,
    user_id  BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    task_id  BIGINT NOT NULL REFERENCES task(id)     ON DELETE CASCADE,
    channel  VARCHAR(16)  NOT NULL,
    status   VARCHAR(16)  NOT NULL,
    error    TEXT         NULL,
    message  TEXT         NULL,
    sent_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX notification_log_user_idx ON notification_log(user_id);
CREATE INDEX notification_log_task_idx ON notification_log(task_id);
