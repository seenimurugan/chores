-- V4: drop the FK constraint on app_user_audit.target_user_id
--
-- Audit tables are append-only event logs. A FK constraint on target_user_id
-- breaks REQUIRES_NEW audit writes from inside an outer transaction that has
-- not yet committed the referenced user (e.g. UserService.createKid →
-- UserAuditService.recordUserCreate). The inner transaction can't see the
-- uncommitted user → FK violation → outer transaction rollback.
--
-- Drop the FK; keep the column nullable as plain BIGINT. We retain a regular
-- index on target_user_id for forensic queries.

ALTER TABLE app_user_audit DROP CONSTRAINT IF EXISTS app_user_audit_target_user_id_fkey;
