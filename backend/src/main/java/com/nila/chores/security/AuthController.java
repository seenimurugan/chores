package com.nila.chores.security;

import com.nila.chores.audit.UserAuditService;
import com.nila.chores.user.User;
import com.nila.chores.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final UserAuditService auditService;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                          UserAuditService auditService) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        String sourceIp = resolveClientIp(httpReq);
        String username = req.username().trim();

        log.info("event=login.attempt actor={} sourceIp={}", username, sourceIp);

        User u;
        try {
            u = users.findByUsername(username).orElse(null);
        } catch (Exception e) {
            log.error("event=login.attempt actor={} sourceIp={} outcome=fail reason=db-error error={}",
                    username, sourceIp, e.getMessage(), e);
            auditService.recordLoginAttempt(username, sourceIp, "FAIL", "db-error");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Login failed");
        }

        if (u == null) {
            log.warn("event=login.attempt actor={} sourceIp={} outcome=fail reason=user-not-found",
                    username, sourceIp);
            auditService.recordLoginAttempt(username, sourceIp, "FAIL", "user-not-found");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            log.warn("event=login.attempt actor={} role={} sourceIp={} outcome=fail reason=bad-credentials",
                    username, u.getRole(), sourceIp);
            auditService.recordLoginAttempt(username, sourceIp, "FAIL", "bad-credentials");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwt.issue(u);
        log.info("event=login.attempt actor={} role={} sourceIp={} outcome=success reason=jwt-issued",
                username, u.getRole(), sourceIp);
        auditService.recordLoginAttempt(username, sourceIp, "SUCCESS", "jwt-issued");

        return new LoginResponse(token, jwt.getTtlMillis() / 1000,
                new MeDto(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole().name(),
                        u.getAvatarColor(), u.getEditWindowDays()));
    }

    @GetMapping("/me")
    public MeDto me(@AuthenticationPrincipal AuthUser principal) {
        if (principal == null) {
            log.warn("event=me.request outcome=fail reason=unauthenticated");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        log.info("event=me.request actor={} role={} outcome=success", principal.username(), principal.role());
        User u = users.findById(principal.id())
                .orElseThrow(() -> {
                    log.warn("event=me.request actor={} outcome=fail reason=user-not-found-in-db", principal.id());
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED);
                });
        return new MeDto(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole().name(),
                u.getAvatarColor(), u.getEditWindowDays());
    }

    private String resolveClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, long expiresInSeconds, MeDto user) {}
    public record MeDto(Long id, String username, String displayName, String role, String avatarColor, int editWindowDays) {}
}
