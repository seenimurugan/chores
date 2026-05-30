package com.nila.chores.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserDto> list() {
        return service.listKids().stream().map(UserDto::of).toList();
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateKidRequest req) {
        User u = service.createKid(req.username().trim(), req.password(), req.displayName().trim(), req.avatarColor());
        return ResponseEntity.ok(UserDto.of(u));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) {
        service.resetPassword(id, req.password());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteKid(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateKidRequest(
            @NotBlank @Size(min = 2, max = 64) String username,
            @NotBlank @Size(min = 4, max = 128) String password,
            @NotBlank @Size(min = 1, max = 128) String displayName,
            String avatarColor
    ) {}

    public record ResetPasswordRequest(@NotBlank @Size(min = 4, max = 128) String password) {}

    public record UserDto(Long id, String username, String displayName, String role, String avatarColor) {
        public static UserDto of(User u) {
            return new UserDto(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole().name(), u.getAvatarColor());
        }
    }
}
