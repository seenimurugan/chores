package com.nila.chores.user;

import com.nila.chores.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
        User u = service.createKid(actor.id(), actor.username(),
                req.username().trim(), req.password(), req.displayName().trim(), req.avatarColor());
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
        User u = service.updateEditWindow(actor.id(), id, req.editWindowDays());
        return ResponseEntity.ok(UserDto.of(u));
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
            String avatarColor
    ) {}

    public record ResetPasswordRequest(@NotBlank @Size(min = 4, max = 128) String password) {}

    public record UpdateEditWindowRequest(
            @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(365) int editWindowDays
    ) {}

    public record UserDto(Long id, String username, String displayName, String role, String avatarColor, int editWindowDays) {
        public static UserDto of(User u) {
            return new UserDto(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole().name(),
                    u.getAvatarColor(), u.getEditWindowDays());
        }
    }
}
