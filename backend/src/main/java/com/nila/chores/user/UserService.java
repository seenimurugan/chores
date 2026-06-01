package com.nila.chores.user;

import com.nila.chores.audit.UserAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final UserAuditService auditService;

    public UserService(UserRepository users, PasswordEncoder encoder, UserAuditService auditService) {
        this.users = users;
        this.encoder = encoder;
        this.auditService = auditService;
    }

    public List<User> listKids() {
        return users.findByRoleOrderByDisplayNameAsc(User.Role.KID);
    }

    @Transactional
    public User createKid(Long actorId, String actorUsername,
                          String username, String password, String displayName, String avatarColor) {
        log.info("event=user.create actor={} target.username={} action=create-kid",
                actorId, username);
        if (users.existsByUsername(username)) {
            log.warn("event=user.create actor={} target.username={} outcome=fail reason=username-conflict",
                    actorId, username);
            auditService.recordUserCreate(actorId, actorUsername, null, "FAIL");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        try {
            User u = new User();
            u.setUsername(username);
            u.setPasswordHash(encoder.encode(password));
            u.setDisplayName(displayName);
            u.setRole(User.Role.KID);
            if (avatarColor != null && !avatarColor.isBlank()) u.setAvatarColor(avatarColor);
            User saved = users.save(u);
            log.info("event=user.create actor={} target={} target.username={} outcome=success",
                    actorId, saved.getId(), username);
            auditService.recordUserCreate(actorId, actorUsername, saved.getId(), "SUCCESS");
            return saved;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("event=user.create actor={} target.username={} outcome=fail reason=db-error error={}",
                    actorId, username, e.getMessage(), e);
            auditService.recordUserCreate(actorId, actorUsername, null, "FAIL");
            throw e;
        }
    }

    @Transactional
    public void resetPassword(Long actorId, String actorUsername, Long targetUserId, String newPassword) {
        log.info("event=password.reset actor={} target={} action=reset-password", actorId, targetUserId);
        User u = users.findById(targetUserId).orElse(null);
        if (u == null) {
            log.warn("event=password.reset actor={} target={} outcome=fail reason=user-not-found",
                    actorId, targetUserId);
            auditService.recordPasswordReset(actorId, actorUsername, targetUserId, "FAIL", "user-not-found");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        try {
            u.setPasswordHash(encoder.encode(newPassword));
            log.info("event=password.reset actor={} target={} target.username={} outcome=success",
                    actorId, targetUserId, u.getUsername());
            auditService.recordPasswordReset(actorId, actorUsername, targetUserId, "SUCCESS", null);
        } catch (Exception e) {
            log.error("event=password.reset actor={} target={} outcome=fail reason=db-error error={}",
                    actorId, targetUserId, e.getMessage(), e);
            auditService.recordPasswordReset(actorId, actorUsername, targetUserId, "FAIL", "db-error");
            throw e;
        }
    }

    @Transactional
    public void deleteKid(Long actorId, String actorUsername, Long targetUserId) {
        log.info("event=user.delete actor={} target={} action=delete-kid", actorId, targetUserId);
        User u = users.findById(targetUserId).orElse(null);
        if (u == null) {
            log.warn("event=user.delete actor={} target={} outcome=fail reason=user-not-found",
                    actorId, targetUserId);
            auditService.recordUserDelete(actorId, actorUsername, targetUserId, "FAIL");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        if (u.getRole() != User.Role.KID) {
            log.warn("event=user.delete actor={} target={} target.role={} outcome=fail reason=not-a-kid",
                    actorId, targetUserId, u.getRole());
            auditService.recordUserDelete(actorId, actorUsername, targetUserId, "FAIL");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only delete kid accounts");
        }
        try {
            users.delete(u);
            log.info("event=user.delete actor={} target={} target.username={} outcome=success",
                    actorId, targetUserId, u.getUsername());
            auditService.recordUserDelete(actorId, actorUsername, targetUserId, "SUCCESS");
        } catch (Exception e) {
            log.error("event=user.delete actor={} target={} outcome=fail reason=db-error error={}",
                    actorId, targetUserId, e.getMessage(), e);
            auditService.recordUserDelete(actorId, actorUsername, targetUserId, "FAIL");
            throw e;
        }
    }

    @Transactional
    public User updateEditWindow(Long actorId, Long targetUserId, int editWindowDays) {
        log.info("event=user.update actor={} target={} field=edit-window-days value={}",
                actorId, targetUserId, editWindowDays);
        User u = users.findById(targetUserId)
                .orElseThrow(() -> {
                    log.warn("event=user.update actor={} target={} outcome=fail reason=user-not-found",
                            actorId, targetUserId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });
        if (u.getRole() != User.Role.KID) {
            log.warn("event=user.update actor={} target={} target.role={} outcome=fail reason=not-a-kid",
                    actorId, targetUserId, u.getRole());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only update edit window for kid accounts");
        }
        u.setEditWindowDays(editWindowDays);
        log.info("event=user.update actor={} target={} field=edit-window-days outcome=success", actorId, targetUserId);
        return u;
    }

    public User getByUsername(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
