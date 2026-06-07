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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Computes the per-kid, per-chore reminder overview for the admin dashboard.
 *
 * For each assigned chore with a {@code weekly_target}, returns:
 *   - choreId, choreTitle, icon, weeklyTarget
 *   - completionsInPeriod — count of done=true completions in the selected period range
 *   - remindersSentInPeriod — count of SENT notification_log rows for kid+task in the period
 *   - lastReminderInPeriod — max sentAt in the period, or null
 *   - doneThisWeek — always live current-week count (regardless of selected period)
 *   - status — always live current-week ON_TRACK / AT_RISK
 *
 * The {@code period} parameter controls the audit window:
 *   this-week | last-week | this-month | last-month | this-year | last-year
 * Default is {@code this-week}.
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
     * Returns the reminder overview for a kid. Only chores with a {@code weekly_target} are included.
     *
     * <p>Always returns live current-week status (ON_TRACK / AT_RISK) and doneThisWeek.
     * Period-scoped audit columns (completionsInPeriod, remindersSentInPeriod, lastReminderInPeriod)
     * are bounded by the requested {@code period}.</p>
     *
     * @param kidId  the kid's user ID
     * @param period one of: this-week, last-week, this-month, last-month, this-year, last-year
     * @return list of per-chore overview rows, ordered by chore title
     * @throws ResponseStatusException 404 if the kid is not found
     */
    @Transactional(readOnly = true)
    public List<ChoreOverviewRow> getOverviewForKid(Long kidId, String period) {
        log.info("event=reminder-overview.request kidId={} period={}", kidId, period);

        User kid = userRepository.findById(kidId).orElseThrow(() -> {
            log.warn("event=reminder-overview.kid-not-found kidId={}", kidId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Kid not found");
        });

        ZoneId kidZone = resolveZone(kid);
        LocalDate today = LocalDate.now(clock.withZone(kidZone));
        LocalDate currentWeekStart = calculator.mostRecentMonday(today);

        // Current-week live window (always used for status + doneThisWeek)
        OffsetDateTime currentWeekStartUtc = currentWeekStart.atStartOfDay(kidZone).toOffsetDateTime();
        OffsetDateTime currentWeekEndUtc = today.plusDays(1).atStartOfDay(kidZone).toOffsetDateTime();

        // Period-scoped audit window
        PeriodRange periodRange = resolvePeriodRange(period, kidZone, clock);
        OffsetDateTime periodStartUtc = periodRange.start().atStartOfDay(kidZone).toOffsetDateTime();
        OffsetDateTime periodEndUtc = periodRange.end().plusDays(1).atStartOfDay(kidZone).toOffsetDateTime();

        log.debug(
            "event=reminder-overview.windows kidId={} kidZone={} period={} " +
            "periodStart={} periodEnd={} currentWeekStart={} currentWeekEnd={}",
            kidId, kidZone, period, periodStartUtc, periodEndUtc,
            currentWeekStartUtc, currentWeekEndUtc);

        List<TaskAssignment> assignments = assignmentRepository.findActiveForUser(kidId);
        log.info("event=reminder-overview.assignments kidId={} count={}", kidId, assignments.size());

        // Current-week completions (for live status)
        List<CompletionRow> currentWeekCompletions =
                completionRepository.listForUser(kidId, currentWeekStart, today);
        Map<Long, Long> doneThisWeekByTask = currentWeekCompletions.stream()
                .collect(Collectors.groupingBy(CompletionRow::getTaskId, Collectors.counting()));

        // Period-scoped completions (for audit column)
        List<CompletionRow> periodCompletions =
                completionRepository.listForUser(kidId, periodRange.start(), periodRange.end());
        Map<Long, Long> doneInPeriodByTask = periodCompletions.stream()
                .collect(Collectors.groupingBy(CompletionRow::getTaskId, Collectors.counting()));

        List<ChoreOverviewRow> rows = assignments.stream()
                .map(TaskAssignment::getTask)
                .filter(task -> task.getWeeklyTarget() != null)
                .map(task -> {
                    // Live current-week status
                    int doneThisWeek = doneThisWeekByTask.getOrDefault(task.getId(), 0L).intValue();
                    boolean atRisk = calculator.isAtRisk(
                            task.getWeeklyTarget(), task.getRemindLeadDays(), doneThisWeek, today);
                    String status = atRisk ? "AT_RISK" : "ON_TRACK";

                    // Period-scoped audit
                    int completionsInPeriod = doneInPeriodByTask.getOrDefault(task.getId(), 0L).intValue();
                    long remindersSentInPeriod = logRepository.countByUserIdAndTaskIdAndSentAtBetween(
                            kidId, task.getId(), periodStartUtc, periodEndUtc);
                    Optional<OffsetDateTime> lastReminderInPeriod =
                            logRepository.findMaxSentAtByUserIdAndTaskIdAndSentAtBetween(
                                    kidId, task.getId(), periodStartUtc, periodEndUtc);

                    log.info(
                        "event=reminder-overview.chore kidId={} taskId={} taskTitle={} icon={} " +
                        "doneThisWeek={} target={} status={} " +
                        "completionsInPeriod={} remindersSentInPeriod={} period={}",
                        kidId, task.getId(), task.getTitle(), task.getIcon(),
                        doneThisWeek, task.getWeeklyTarget(), status,
                        completionsInPeriod, remindersSentInPeriod, period);

                    return new ChoreOverviewRow(
                            task.getId(),
                            task.getTitle(),
                            task.getIcon(),
                            task.getWeeklyTarget(),
                            doneThisWeek,
                            status,
                            completionsInPeriod,
                            (int) remindersSentInPeriod,
                            lastReminderInPeriod.orElse(null));
                })
                .sorted(java.util.Comparator.comparing(ChoreOverviewRow::choreTitle))
                .toList();

        log.info("event=reminder-overview.complete kidId={} period={} rows={}", kidId, period, rows.size());
        return rows;
    }

    /**
     * Convenience overload defaulting to {@code this-week}.
     * Retained for backward compatibility with existing tests.
     */
    @Transactional(readOnly = true)
    public List<ChoreOverviewRow> getOverviewForKid(Long kidId) {
        return getOverviewForKid(kidId, "this-week");
    }

    // ── Period range resolution ───────────────────────────────────────────────

    /**
     * Resolves the named period to a [start, end] LocalDate range (both inclusive),
     * anchored in the given timezone.
     *
     * <p>Week boundaries are Monday–Sunday (ISO 8601).</p>
     *
     * <p>Exposed as {@code static} so tests can call it directly without wiring
     * the full service graph.</p>
     *
     * @param period  one of: this-week, last-week, this-month, last-month, this-year, last-year
     * @param zone    the timezone to anchor date calculations
     * @param clock   the clock to compute "today"
     * @return a PeriodRange with start ≤ end
     */
    public static PeriodRange resolvePeriodRange(String period, ZoneId zone, Clock clock) {
        LocalDate today = LocalDate.now(clock.withZone(zone));

        return switch (period) {
            case "last-week" -> {
                LocalDate thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate lastWeekMonday = thisWeekMonday.minusWeeks(1);
                LocalDate lastWeekSunday = thisWeekMonday.minusDays(1);
                yield new PeriodRange(lastWeekMonday, lastWeekSunday);
            }
            case "this-month" -> {
                LocalDate firstOfMonth = today.withDayOfMonth(1);
                yield new PeriodRange(firstOfMonth, today);
            }
            case "last-month" -> {
                LocalDate firstOfThisMonth = today.withDayOfMonth(1);
                LocalDate lastDayOfLastMonth = firstOfThisMonth.minusDays(1);
                LocalDate firstOfLastMonth = lastDayOfLastMonth.withDayOfMonth(1);
                yield new PeriodRange(firstOfLastMonth, lastDayOfLastMonth);
            }
            case "this-year" -> {
                LocalDate jan1 = today.withDayOfYear(1);
                yield new PeriodRange(jan1, today);
            }
            case "last-year" -> {
                LocalDate jan1LastYear = today.minusYears(1).withDayOfYear(1);
                LocalDate dec31LastYear = jan1LastYear.withMonth(12).withDayOfMonth(31);
                yield new PeriodRange(jan1LastYear, dec31LastYear);
            }
            default -> {
                // "this-week" and any unrecognised value → current Mon–today
                if (!"this-week".equals(period)) {
                    log.warn("event=reminder-overview.unknown-period period={} defaulting=this-week", period);
                }
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new PeriodRange(monday, today);
            }
        };
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

    // ── Records ──────────────────────────────────────────────────────────────

    /**
     * Inclusive date range for a named reporting period.
     *
     * @param start first day (inclusive)
     * @param end   last day (inclusive)
     */
    public record PeriodRange(LocalDate start, LocalDate end) {}

    /**
     * Per-chore row for the reminder overview endpoint.
     *
     * @param choreId               task ID
     * @param choreTitle            task title
     * @param icon                  task icon emoji/string, or null
     * @param weeklyTarget          required completions per week
     * @param doneThisWeek          live: completions done=true in current Mon–today
     * @param status                live: "ON_TRACK" or "AT_RISK" for current week
     * @param completionsInPeriod   count of done=true completions in selected period
     * @param remindersSentInPeriod count of SENT notification_log rows in selected period
     * @param lastReminderInPeriod  max sentAt in selected period, or null
     */
    public record ChoreOverviewRow(
            Long choreId,
            String choreTitle,
            String icon,
            int weeklyTarget,
            int doneThisWeek,
            String status,
            int completionsInPeriod,
            int remindersSentInPeriod,
            OffsetDateTime lastReminderInPeriod
    ) {}
}
