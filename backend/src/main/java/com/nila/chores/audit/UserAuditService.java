package com.nila.chores.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuditService {

    private static final Logger log = LoggerFactory.getLogger(UserAuditService.class);

    private final UserAuditRepository repo;

    public UserAuditService(UserAuditRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void recordLoginAttempt(String username, String sourceIp, String outcome, String reason) {
        log.info("event=audit.write action=recordLoginAttempt actor={} sourceIp={} outcome={} reason={}",
                username, sourceIp, outcome, reason);
        repo.save(new UserAuditEntity(
                outcome.equals("SUCCESS") ? "LOGIN_SUCCESS" : "LOGIN_FAIL",
                null, username, null, sourceIp, outcome, reason));
    }

    @Transactional
    public void recordPasswordReset(Long actorId, String actorUsername, Long targetId, String outcome, String reason) {
        log.info("event=audit.write action=recordPasswordReset actor={} target={} outcome={} reason={}",
                actorId, targetId, outcome, reason);
        repo.save(new UserAuditEntity(
                "PASSWORD_RESET", actorId, actorUsername, targetId, null, outcome, reason));
    }

    @Transactional
    public void recordPasswordChangeSelf(Long userId, String username, String outcome, String reason) {
        log.info("event=audit.write action=recordPasswordChangeSelf actor={} outcome={} reason={}",
                userId, outcome, reason);
        repo.save(new UserAuditEntity(
                "PASSWORD_CHANGE_SELF", userId, username, userId, null, outcome, reason));
    }

    @Transactional
    public void recordUserCreate(Long actorId, String actorUsername, Long targetId, String outcome) {
        log.info("event=audit.write action=recordUserCreate actor={} target={} outcome={}",
                actorId, targetId, outcome);
        repo.save(new UserAuditEntity(
                "USER_CREATE", actorId, actorUsername, targetId, null, outcome, null));
    }

    @Transactional
    public void recordUserDelete(Long actorId, String actorUsername, Long targetId, String outcome) {
        log.info("event=audit.write action=recordUserDelete actor={} target={} outcome={}",
                actorId, targetId, outcome);
        repo.save(new UserAuditEntity(
                "USER_DELETE", actorId, actorUsername, targetId, null, outcome, null));
    }
}
