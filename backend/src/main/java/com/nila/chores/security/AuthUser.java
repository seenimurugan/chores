package com.nila.chores.security;

import com.nila.chores.user.User;

public record AuthUser(Long id, String username, String displayName, User.Role role) {
    public static AuthUser from(User u) {
        return new AuthUser(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole());
    }
}
