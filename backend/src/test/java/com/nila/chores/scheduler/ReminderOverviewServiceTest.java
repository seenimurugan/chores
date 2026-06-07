package com.nila.chores.scheduler;

import com.nila.chores.notification.NotificationLogRepository;
import com.nila.chores.task.Task;
import com.nila.chores.task.TaskAssignment;
import com.nila.chores.task.TaskAssignmentRepository;
import com.nila.chores.task.TaskCompletionRepository;
import com.nila.chores.user.User;
import com.nila.chores.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * TDD unit tests for ReminderOverviewService.getOverviewForKid(Long).
 *
 * Scenarios:
 *   - mix of on-track + at-risk + never-reminded chores → correct status/count/lastReminder
 *   - chore without weeklyTarget → excluded from overview
 *   - kid not found → 404
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReminderOverviewServiceTest {

    @Mock UserRepository userRepository;
    @Mock TaskAssignmentRepository assignmentRepository;
    @Mock TaskCompletionRepository completionRepository;
    @Mock NotificationLogRepository logRepository;

    ReminderOverviewService service;

    // Clock fixed to 2024-01-07 (Sunday) 09:00 Europe/London
    private static final ZoneId LONDON = ZoneId.of("Europe/London");
    private static final LocalDate SUNDAY = LocalDate.of(2024, 1, 7);
    private static final LocalDate MONDAY = LocalDate.of(2024, 1, 1); // week start

    private Clock fixedClock() {
        Instant instant = SUNDAY.atTime(9, 0).atZone(LONDON).toInstant();
        return Clock.fixed(instant, LONDON);
    }

    @BeforeEach
    void setUp() {
        service = new ReminderOverviewService(
                userRepository, assignmentRepository, completionRepository,
                logRepository, new AtRiskCalculator(), fixedClock());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User kid(Long id, String tz) {
        User u = new User();
        u.setId(id);
        u.setUsername("kid" + id);
        u.setDisplayName("Kid" + id);
        u.setRole(User.Role.KID);
        u.setTimezone(tz);
        u.setAvatarColor("#4263eb");
        u.setPasswordHash("hash");
        return u;
    }

    private Task task(Long id, String title, Integer weeklyTarget, int leadDays) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        t.setWeeklyTarget(weeklyTarget);
        t.setRemindLeadDays(leadDays);
        t.setRecurrence(Task.Recurrence.DAILY);
        t.setPoints(1);
        t.setActive(true);
        return t;
    }

    private TaskAssignment assignment(Task task, User user) {
        TaskAssignment a = new TaskAssignment();
        a.setTask(task);
        a.setUser(user);
        return a;
    }

    private TaskCompletionRepository.DailyCount doneCount(LocalDate day, Long count) {
        return new TaskCompletionRepository.DailyCount() {
            public LocalDate getDay() { return day; }
            public Long getDone() { return count; }
        };
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Mix of on-track, at-risk, never-reminded chores.
     * Sunday: D=1.
     *   chore1: T=2, C=2 → remaining=0 → ON_TRACK
     *   chore2: T=1, C=0 → remaining=1, D=1 ≤ 1+0 → AT_RISK; 3 reminders sent, lastReminder not null
     *   chore3: T=2, C=0 → remaining=2, D=1 ≤ 2+0 → AT_RISK; 0 reminders, lastReminder null
     *   chore4: weekly_target=null → excluded
     */
    @Test
    void mixedChores_correctStatusCountsAndLastReminder() {
        User kid = kid(1L, "Europe/London");
        Task chore1 = task(10L, "Reading", 2, 0);
        Task chore2 = task(11L, "Science", 1, 0);
        Task chore3 = task(12L, "Exercise", 2, 0);
        Task chore4 = task(13L, "Free play", null, 0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(kid));
        when(assignmentRepository.findActiveForUser(1L)).thenReturn(List.of(
                assignment(chore1, kid), assignment(chore2, kid),
                assignment(chore3, kid), assignment(chore4, kid)));

        // The service queries per-task completions using listForUser (not countDoneByDay).
        when(completionRepository.listForUser(eq(1L), eq(MONDAY), eq(SUNDAY)))
                .thenReturn(List.of(
                        completionRow(MONDAY, 1L, 10L),
                        completionRow(LocalDate.of(2024,1,2), 1L, 10L)  // chore1: 2 completions
                        // chore2 and chore3: 0 completions
                ));

        // reminder log counts
        OffsetDateTime lastReminderAt = OffsetDateTime.parse("2024-01-07T09:30:00+00:00");
        when(logRepository.countByUserIdAndTaskIdAndSentAtBetween(eq(1L), eq(11L), any(), any()))
                .thenReturn(3L);
        when(logRepository.findMaxSentAtByUserIdAndTaskIdAndSentAtBetween(eq(1L), eq(11L), any(), any()))
                .thenReturn(Optional.of(lastReminderAt));
        when(logRepository.countByUserIdAndTaskIdAndSentAtBetween(eq(1L), eq(10L), any(), any()))
                .thenReturn(0L);
        when(logRepository.countByUserIdAndTaskIdAndSentAtBetween(eq(1L), eq(12L), any(), any()))
                .thenReturn(0L);
        when(logRepository.findMaxSentAtByUserIdAndTaskIdAndSentAtBetween(eq(1L), eq(10L), any(), any()))
                .thenReturn(Optional.empty());
        when(logRepository.findMaxSentAtByUserIdAndTaskIdAndSentAtBetween(eq(1L), eq(12L), any(), any()))
                .thenReturn(Optional.empty());

        List<ReminderOverviewService.ChoreOverviewRow> rows = service.getOverviewForKid(1L);

        // Only 3 chores with weekly_target (chore4 excluded)
        assertThat(rows).hasSize(3);

        ReminderOverviewService.ChoreOverviewRow r1 = rows.stream().filter(r -> r.choreId() == 10L).findFirst().orElseThrow();
        assertThat(r1.choreTitle()).isEqualTo("Reading");
        assertThat(r1.weeklyTarget()).isEqualTo(2);
        assertThat(r1.doneThisWeek()).isEqualTo(2);
        assertThat(r1.status()).isEqualTo("ON_TRACK");
        assertThat(r1.remindersSentThisWeek()).isEqualTo(0);
        assertThat(r1.lastReminderAt()).isNull();

        ReminderOverviewService.ChoreOverviewRow r2 = rows.stream().filter(r -> r.choreId() == 11L).findFirst().orElseThrow();
        assertThat(r2.choreTitle()).isEqualTo("Science");
        assertThat(r2.doneThisWeek()).isEqualTo(0);
        assertThat(r2.status()).isEqualTo("AT_RISK");
        assertThat(r2.remindersSentThisWeek()).isEqualTo(3);
        assertThat(r2.lastReminderAt()).isEqualTo(lastReminderAt);

        ReminderOverviewService.ChoreOverviewRow r3 = rows.stream().filter(r -> r.choreId() == 12L).findFirst().orElseThrow();
        assertThat(r3.status()).isEqualTo("AT_RISK");
        assertThat(r3.remindersSentThisWeek()).isEqualTo(0);
        assertThat(r3.lastReminderAt()).isNull();
    }

    /**
     * Kid not found → 404.
     */
    @Test
    void kidNotFound_throws404() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOverviewForKid(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    /**
     * Kid with no targeted chores → empty list.
     */
    @Test
    void noTargetedChores_returnsEmpty() {
        User kid = kid(2L, "Europe/London");
        Task chore = task(20L, "Free play", null, 0); // no target

        when(userRepository.findById(2L)).thenReturn(Optional.of(kid));
        when(assignmentRepository.findActiveForUser(2L)).thenReturn(List.of(assignment(chore, kid)));

        List<ReminderOverviewService.ChoreOverviewRow> rows = service.getOverviewForKid(2L);

        assertThat(rows).isEmpty();
    }

    // ── Private helper ────────────────────────────────────────────────────────

    private TaskCompletionRepository.CompletionRow completionRow(LocalDate day, Long userId, Long taskId) {
        return new TaskCompletionRepository.CompletionRow() {
            public LocalDate getDay() { return day; }
            public Long getUserId() { return userId; }
            public Long getTaskId() { return taskId; }
        };
    }
}
