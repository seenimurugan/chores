package com.nila.chores.task;

import com.nila.chores.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "task_completion", uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "user_id", "completion_date"}))
@Getter @Setter @NoArgsConstructor
public class TaskCompletion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "completion_date", nullable = false)
    private LocalDate completionDate;

    @Column(nullable = false)
    private Boolean done = true;

    @Column(name = "completed_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime completedAt;
}
