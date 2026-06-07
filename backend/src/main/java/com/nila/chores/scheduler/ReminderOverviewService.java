package com.nila.chores.scheduler;

import com.nila.chores.notification.NotificationLogRepository;
import com.nila.chores.task.TaskAssignment;
import com.nila.chores.task.TaskAssignmentRepository;
import com.nila.chores.task.TaskCompletionRepository;
import com.nila.chores.task.TaskCompletionRepository.CompletionRow;
import com.nila.chores.user.User;
import com.nila.chores.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes the per-kid, per-chore reminder overview for the admin dashboard.
 *
 * For each assigned chore with a {@code weekly_target}, returns:
 *   - choreId, choreTitle, weeklyTarget
 *   - doneThisWeek (count of done=true completions in current ISO week)
 *   - status (ON_TRACK | AT_RISK) — reuses AtRiskCalculator
 *   - remindersSentThisWeek (count of SENT notification_log rows for this kid+chore this week)
 *   - lastReminderAt (max sentAt, or null if never reminded)
 *
 * Clock is injected for testability.
 */
@Service
public class ReminderOverviewService {

    private static final Logger log = LoggerFactory.getLogger(ReminderOverviewService.class);

    private final UserRepository userRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final TaskCompletionRepository completionRepository;
    private final NotificationLogRepository logRepository;
    private final AtRiskCalculator calculator;
    private final Clock clock;

    public ReminderOverviewService(UserRepository userRepository,
                                   TaskAssignmentRepository assignmentRepository,
                                   TaskCompletionRepository completionRepository,
                                   NotificationLogRepository logRepository,
                                   AtRiskCalculator calculator,
                                   Clock clock) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.completionRepository = completionRepository;
        this.logRepository = logRepository;
        this.calculator = calculator;
        this.clock = clock;
    }

    /**
     * Returns the reminder overview for a kid. Only chores with a {@code weekly_target}
     * are included.
     *
     * @param kidId the kid's user ID
     * @return list of per-chore overview rows, ordered by chore title
     * @throws ResponseStatusException 404 if the kid is not found
     */
    @Transactional(readOnly = true)
    public List<ChoreOverviewRow> getOverviewForKid(Long kidId) {
        log.info("event=reminder-overview.request kidId={}", kidId);

        User kid = userRepository.findById(kidId).orElseThrow(() -> {
            log.warn("event=reminder-overview.kid-not-found kidId={}", kidId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Kid not found");
        });

        ZoneId kidZone = resolveZone(kid);
        LocalDate today = LocalDate.now(clock.withZone(kidZone));
        LocalDate weekStart = calculator.mostRecentMonday(today);

        // Week window anchored in the KID's local timezone (not UTC).
        // Using the kid's zone ensures the boundary aligns with the kid's local midnight,
        // so reminders sent at e.g. 01:30 IST are counted in the correct IST calendar week.
        OffsetDateTime weekStartUtc = weekStart.atStartOfDay(kidZone).toOffsetDateTime();
        OffsetDateTime weekEndUtc = today.plusDays(1).atStartOfDay(kidZone).toOffsetDateTime();

        log.debug("event=reminder-overview.week-window kidId={} kidZone={} weekStartUtc={} weekEndUtc={}",
                kidId, kidZone, weekStartUtc, weekEndUtc);

        List<TaskAssignment> assignments = assignmentRepository.findActiveForUser(kidId);
        log.info("event=reminder-overview.assignments kidId={} count={}", kidId, assignments.size());

        // Load all completions for this kid this week (one query, then group by taskId)
        List<CompletionRow> allCompletions = completionRepository.listForUser(kidId, weekStart, today);
        Map<Long, Long> doneByTask = allCompletions.stream()
                .collect(Collectors.groupingBy(CompletionRow::getTaskId, Collectors.counting()));

        List<ChoreOverviewRow> rows = assignments.stream()
                .map(TaskAssignment::getTask)
                .filter(task -> task.getWeeklyTarget() != null)
                .map(task -> {
                    long done = doneByTask.getOrDefault(task.getId(), 0L);
                    boolean atRisk = calculator.isAtRisk(task.getWeeklyTarget(), task.getRemindLeadDays(), (int) done, today);
                    String status = atRisk ? "AT_RISK" : "ON_TRACK";

                    long reminderCount = logRepository.countByUserIdAndTaskIdAndSentAtBetween(
                            kidId, task.getId(), weekStartUtc, weekEndUtc);
                    Optional<OffsetDateTime> lastReminder = logRepository.findMaxSentAtByUserIdAndTaskIdAndSentAtBetween(
                            kidId, task.getId(), weekStartUtc, weekEndUtc);

                    log.info("event=reminder-overview.chore kidId={} taskId={} taskTitle={} done={} target={} status={} remindersSent={}",
                            kidId, task.getId(), task.getTitle(), done, task.getWeeklyTarget(), status, reminderCount);

                    return new ChoreOverviewRow(
                            task.getId(), task.getTitle(), task.getWeeklyTarget(),
                            (int) done, status, (int) reminderCount,
                            lastReminder.orElse(null));
                })
                .sorted(java.util.Comparator.comparing(ChoreOverviewRow::choreTitle))
                .toList();

        log.info("event=reminder-overview.complete kidId={} rows={}", kidId, rows.size());
        return rows;
    }

    private ZoneId resolveZone(User kid) {
        String tz = kid.getTimezone();
        if (tz == null || tz.isBlank()) {
            log.warn("event=reminder-overview.timezone.missing kidId={} fallback=Europe/London", kid.getId());
            return ZoneId.of("Europe/London");
        }
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            log.warn("event=reminder-overview.timezone.invalid kidId={} tz={} fallback=Europe/London reason={}",
                    kid.getId(), tz, e.getMessage());
            return ZoneId.of("Europe/London");
        }
    }

    /**
     * Per-chore row for the reminder overview endpoint.
     *
     * @param choreId              task ID
     * @param choreTitle           task title
     * @param weeklyTarget         required completions per week
     * @param doneThisWeek         completions done=true in [weekStart..today]
     * @param status               "ON_TRACK" or "AT_RISK"
     * @param remindersSentThisWeek count of SENT notification_log rows this week for this kid+chore
     * @param lastReminderAt       max sentAt this week, or null
     */
    public record ChoreOverviewRow(
            Long choreId,
            String choreTitle,
            int weeklyTarget,
            int doneThisWeek,
            String status,
            int remindersSentThisWeek,
            OffsetDateTime lastReminderAt
    ) {}
}
