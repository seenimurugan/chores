package com.nila.chores.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
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

    /**
     * IANA timezone string (e.g. "Europe/London", "Asia/Kolkata").
     * Used to localise reminder scheduling for each kid.
     * Defaults to Europe/London so existing records get a sane value.
     */
    @Column(name = "timezone", nullable = false)
    private String timezone = "Europe/London";

    /**
     * Per-kid time-of-day at which at-risk reminders are sent (in the kid's own timezone).
     * Defaults to 06:00 — matching the previous global chores.reminder.send-time default.
     * Admins can override per kid via the contacts-update endpoint.
     */
    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime = LocalTime.of(6, 0);

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum Role { ADMIN, KID }
}
