package com.nila.chores.task;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "task")
@Getter @Setter @NoArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer points = 1;

    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Recurrence recurrence = Recurrence.DAILY;

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * How many times per week this task should be completed.
     * Null means no weekly target is tracked for this task.
     * Must be > 0 when set.
     */
    @Column(name = "weekly_target", nullable = true)
    private Integer weeklyTarget;

    /**
     * How many days before the end of the tracking period to send an at-risk reminder.
     * 0 = only remind on the exact day; default is 0 (strict).
     */
    @Column(name = "remind_lead_days", nullable = false)
    private int remindLeadDays = 0;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum Recurrence { DAILY, WEEKLY, ONCE }
}
