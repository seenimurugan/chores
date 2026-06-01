package com.nila.chores.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuditService {

    private static final Logger log = LoggerFactory.getLogger(UserAuditService.class);

    private final UserAuditRepository repo;

    public UserAuditService(UserAuditRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginAttempt(String username, String sourceIp, String outcome, String reason) {
        try {
            repo.save(new UserAuditEntity(
                    outcome.equals("SUCCESS") ? "LOGIN_SUCCESS" : "LOGIN_FAIL",
                    null, username, null, sourceIp, outcome, reason));
        } catch (Exception e) {
            log.error("event=audit.write.fail action=recordLoginAttempt actor={} outcome={} error={}",
                    username, outcome, e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPasswordReset(Long actorId, String actorUsername, Long targetId, String outcome, String reason) {
        try {
            repo.save(new UserAuditEntity(
                    "PASSWORD_RESET", actorId, actorUsername, targetId, null, outcome, reason));
        } catch (Exception e) {
            log.error("event=audit.write.fail action=recordPasswordReset actor={} target={} error={}",
                    actorId, targetId, e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPasswordChangeSelf(Long userId, String username, String outcome, String reason) {
        try {
            repo.save(new UserAuditEntity(
                    "PASSWORD_CHANGE_SELF", userId, username, userId, null, outcome, reason));
        } catch (Exception e) {
            log.error("event=audit.write.fail action=recordPasswordChangeSelf actor={} error={}",
                    userId, e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUserCreate(Long actorId, String actorUsername, Long targetId, String outcome) {
        try {
            repo.save(new UserAuditEntity(
                    "USER_CREATE", actorId, actorUsername, targetId, null, outcome, null));
        } catch (Exception e) {
            log.error("event=audit.write.fail action=recordUserCreate actor={} target={} error={}",
                    actorId, targetId, e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUserDelete(Long actorId, String actorUsername, Long targetId, String outcome) {
        try {
            repo.save(new UserAuditEntity(
                    "USER_DELETE", actorId, actorUsername, targetId, null, outcome, null));
        } catch (Exception e) {
            log.error("event=audit.write.fail action=recordUserDelete actor={} target={} error={}",
                    actorId, targetId, e.getMessage(), e);
        }
    }
}
