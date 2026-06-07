package com.nila.chores.notification;

import com.nila.chores.task.Task;
import com.nila.chores.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Persistent audit record for every notification send attempt (one row per kid per channel).
 * Migration: V6__add_notification_log.sql
 */
@Entity
@Table(name = "notification_log")
@Getter @Setter @NoArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The kid who was notified. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** The task the reminder was about. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    /** Channel name: "EMAIL" or "TELEGRAM". */
    @Column(nullable = false, length = 16)
    private String channel;

    /** Send outcome: "SENT" or "FAILED". */
    @Column(nullable = false, length = 16)
    private String status;

    /** Error detail when status = "FAILED". Null on success. */
    @Column(columnDefinition = "TEXT")
    private String error;

    /** The message body that was sent. */
    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "sent_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime sentAt;
}
