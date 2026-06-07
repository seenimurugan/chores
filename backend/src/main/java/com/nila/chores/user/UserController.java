package com.nila.chores.user;

import com.nila.chores.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserDto> list(@AuthenticationPrincipal AuthUser actor) {
        log.info("event=admin.users.list actor={}", actor.id());
        return service.listKids().stream().map(UserDto::of).toList();
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@AuthenticationPrincipal AuthUser actor,
                                          @Valid @RequestBody CreateKidRequest req) {
        log.info("event=admin.users.create actor={} target.username={} timezone={}", actor.id(), req.username(), req.timezone());
        String tz = resolveAndValidateTimezone(req.timezone());
        User u = service.createKid(actor.id(), actor.username(),
                req.username().trim(), req.password(), req.displayName().trim(), req.avatarColor(),
                req.email(), req.telegramChatId(), tz);
        return ResponseEntity.ok(UserDto.of(u));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@AuthenticationPrincipal AuthUser actor,
                                              @PathVariable Long id,
                                              @Valid @RequestBody ResetPasswordRequest req) {
        service.resetPassword(actor.id(), actor.username(), id, req.password());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/edit-window")
    public ResponseEntity<UserDto> updateEditWindow(@AuthenticationPrincipal AuthUser actor,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody UpdateEditWindowRequest req) {
        log.info("event=admin.users.edit-window actor={} target={} value={}", actor.id(), id, req.editWindowDays());
        User u = service.updateEditWindow(actor.id(), id, req.editWindowDays());
        return ResponseEntity.ok(UserDto.of(u));
    }

    @PatchMapping("/{id}/contacts")
    public ResponseEntity<UserDto> updateContacts(@AuthenticationPrincipal AuthUser actor,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody UpdateContactsRequest req) {
        log.info("event=admin.users.contacts.update actor={} target={} hasEmail={} hasTelegram={} timezone={}",
                actor.id(), id, req.email() != null, req.telegramChatId() != null, req.timezone());
        String tz = resolveAndValidateTimezone(req.timezone());
        User u = service.updateContacts(actor.id(), id, req.email(), req.telegramChatId(), tz);
        return ResponseEntity.ok(UserDto.of(u));
    }

    /**
     * Validates that {@code raw} is a real IANA zone identifier.
     * Returns the canonical zone string, or "Europe/London" if {@code raw} is null/blank.
     * Throws 400 if the zone is non-blank but unrecognised.
     */
    private String resolveAndValidateTimezone(String raw) {
        if (raw == null || raw.isBlank()) {
            log.debug("event=timezone.resolve input=blank outcome=defaulted-to-Europe/London");
            return "Europe/London";
        }
        try {
            ZoneId zone = ZoneId.of(raw.trim());
            log.debug("event=timezone.resolve input={} outcome=valid zone={}", raw, zone.getId());
            return zone.getId();
        } catch (ZoneRulesException | IllegalArgumentException e) {
            log.warn("event=timezone.resolve input={} outcome=invalid reason={}", raw, e.getMessage());
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Invalid IANA timezone: " + raw);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser actor,
                                       @PathVariable Long id) {
        service.deleteKid(actor.id(), actor.username(), id);
        return ResponseEntity.noContent().build();
    }

    public record CreateKidRequest(
            @NotBlank @Size(min = 2, max = 64) String username,
            @NotBlank @Size(min = 4, max = 128) String password,
            @NotBlank @Size(min = 1, max = 128) String displayName,
            String avatarColor,
            @Email @Size(max = 255) String email,
            Long telegramChatId,
            @Size(max = 64) String timezone
    ) {}

    public record ResetPasswordRequest(@NotBlank @Size(min = 4, max = 128) String password) {}

    public record UpdateEditWindowRequest(
            @Min(0) @jakarta.validation.constraints.Max(365) int editWindowDays
    ) {}

    public record UpdateContactsRequest(
            @Email @Size(max = 255) String email,
            Long telegramChatId,
            @Size(max = 64) String timezone
    ) {}

    public record UserDto(Long id, String username, String displayName, String role,
                          String avatarColor, int editWindowDays,
                          String email, Long telegramChatId, String timezone) {
        public static UserDto of(User u) {
            return new UserDto(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole().name(),
                    u.getAvatarColor(), u.getEditWindowDays(), u.getEmail(), u.getTelegramChatId(),
                    u.getTimezone());
        }
    }
}
