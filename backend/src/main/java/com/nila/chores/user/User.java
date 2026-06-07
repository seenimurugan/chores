package com.nila.chores.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "app_user")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "avatar_color", nullable = false)
    private String avatarColor = "#4263eb";

    @Column(name = "edit_window_days", nullable = false)
    private int editWindowDays = 14;

    /** Optional email address for at-risk reminder notifications. */
    @Email
    @Column(nullable = true)
    private String email;

    /** Optional Telegram chat ID for at-risk reminder notifications. */
    @Column(name = "telegram_chat_id", nullable = true)
    private Long telegramChatId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum Role { ADMIN, KID }
}
