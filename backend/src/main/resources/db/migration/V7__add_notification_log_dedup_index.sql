-- Phase 3: composite index supporting the at-risk scheduler dedup check.
-- Covers: existsByUserIdAndTaskIdAndChannelAndSentAtBetween
CREATE INDEX notification_log_dedup_idx
    ON notification_log(user_id, task_id, channel, sent_at);
