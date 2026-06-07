-- H2-compatible version of V7: dedup index on notification_log.
CREATE INDEX notification_log_dedup_idx
    ON notification_log(user_id, task_id, channel, sent_at);
