package com.nila.chores.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public List<User> listKids() {
        return users.findByRoleOrderByDisplayNameAsc(User.Role.KID);
    }

    @Transactional
    public User createKid(String username, String password, String displayName, String avatarColor) {
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        u.setDisplayName(displayName);
        u.setRole(User.Role.KID);
        if (avatarColor != null && !avatarColor.isBlank()) u.setAvatarColor(avatarColor);
        return users.save(u);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User u = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        u.setPasswordHash(encoder.encode(newPassword));
    }

    @Transactional
    public void deleteKid(Long userId) {
        User u = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getRole() != User.Role.KID) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only delete kid accounts");
        }
        users.delete(u);
    }

    @Transactional
    public User updateEditWindow(Long userId, int editWindowDays) {
        User u = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getRole() != User.Role.KID) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only update edit window for kid accounts");
        }
        u.setEditWindowDays(editWindowDays);
        return u;
    }

    public User getByUsername(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
