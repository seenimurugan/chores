CREATE TABLE app_user_audit (
  id            BIGSERIAL PRIMARY KEY,
  event_type    VARCHAR(32)  NOT NULL,
  actor_user_id BIGINT       NULL,
  actor_username VARCHAR(64) NULL,
  target_user_id BIGINT      NULL REFERENCES app_user(id) ON DELETE SET NULL,
  source_ip     VARCHAR(64)  NULL,
  outcome       VARCHAR(16)  NOT NULL,
  reason        VARCHAR(128) NULL,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_target_user_id  ON app_user_audit(target_user_id, created_at DESC);
CREATE INDEX idx_audit_actor_user_id   ON app_user_audit(actor_user_id, created_at DESC);
CREATE INDEX idx_audit_event_type_time ON app_user_audit(event_type, created_at DESC);
