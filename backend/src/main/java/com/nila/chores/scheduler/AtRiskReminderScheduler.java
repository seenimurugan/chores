package com.nila.chores.scheduler;

import com.nila.chores.notification.EmailSender;
import com.nila.chores.notification.NotificationLog;
import com.nila.chores.notification.NotificationLogRepository;
import com.nila.chores.notification.SendResult;
import com.nila.chores.notification.TelegramSender;
import com.nila.chores.task.Task;
import com.nila.chores.task.TaskAssignment;
import com.nila.chores.task.TaskAssignmentRepository;
import com.nila.chores.task.TaskCompletionRepository;
import com.nila.chores.user.User;
import com.nila.chores.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Scheduled at-risk reminder: runs every 15 minutes, evaluates every KID's
 * chores with a {@code weekly_target} and sends a per-chore message via all
 * configured channels (email + Telegram) when the at-risk condition is met.
 *
 * Dedup: one message per (kid, task, channel) per day — checked against
 * {@code notification_log} before sending.
 *
 * Clock is injected for testability (mirrors the reminders app pattern).
 */
@Component
public class AtRiskReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(AtRiskReminderScheduler.class);

    private final UserRepository userRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final TaskCompletionRepository completionRepository;
    private final NotificationLogRepository logRepository;
    private final TelegramSender telegramSender;
    private final EmailSender emailSender;
    private final AtRiskCalculator calculator;
    private final LocalTime sendTime;
    private final Clock clock;

    /**
     * Spring production constructor — Clock injected as a bean from {@link com.nila.chores.config.ClockConfig}.
     * {@code @Autowired} required because there are multiple constructors.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public AtRiskReminderScheduler(
            UserRepository userRepository,
            TaskAssignmentRepository assignmentRepository,
            TaskCompletionRepository completionRepository,
            NotificationLogRepository logRepository,
            TelegramSender telegramSender,
            EmailSender emailSender,
            AtRiskCalculator calculator,
            @Value("${chores.reminder.send-time:06:00}") String sendTimeStr,
            Clock clock) {
        this(userRepository, assignmentRepository, completionRepository, logRepository,
                telegramSender, emailSender, calculator,
                LocalTime.parse(sendTimeStr), clock);
    }

    /**
     * Package-visible constructor for tests — allows injecting a fixed Clock and send-time directly.
     */
    AtRiskReminderScheduler(
            UserRepository userRepository,
            TaskAssignmentRepository assignmentRepository,
            TaskCompletionRepository completionRepository,
            NotificationLogRepository logRepository,
            TelegramSender telegramSender,
            EmailSender emailSender,
            AtRiskCalculator calculator,
            LocalTime sendTime) {
        this(userRepository, assignmentRepository, completionRepository, logRepository,
                telegramSender, emailSender, calculator, sendTime, Clock.systemDefaultZone());
    }

    AtRiskReminderScheduler(
            UserRepository userRepository,
            TaskAssignmentRepository assignmentRepository,
            TaskCompletionRepository completionRepository,
            NotificationLogRepository logRepository,
            TelegramSender telegramSender,
            EmailSender emailSender,
            AtRiskCalculator calculator,
            LocalTime sendTime,
            Clock clock) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.completionRepository = completionRepository;
        this.logRepository = logRepository;
        this.telegramSender = telegramSender;
        this.emailSender = emailSender;
        this.calculator = calculator;
        this.sendTime = sendTime;
        this.clock = clock;
    }

    /**
     * Trigger point for the scheduler (every 15 minutes in production).
     * Delegates to {@link #runReminders(Clock)} with the live clock.
     */
    @Scheduled(fixedDelayString = "${chores.reminder.check-interval-ms:900000}")
    @Transactional
    public void scheduledRun() {
        log.info("event=atrisk-scheduler.tick");
        runReminders(clock);
    }

    /**
     * Core logic — package-visible so unit tests can inject a fixed Clock.
     *
     * @param clock the clock to derive "now" and "today" from
     */
    @Transactional
    void runReminders(Clock clock) {
        log.info("event=atrisk-scheduler.run.start");
        var kids = userRepository.findAllByRole(User.Role.KID);
        log.info("event=atrisk-scheduler.run.kids-found count={}", kids.size());

        int totalSent = 0;
        int totalSkipped = 0;

        for (User kid : kids) {
            int sent = processKid(kid, clock);
            totalSent += sent;
            if (sent == 0) totalSkipped++;
        }

        log.info("event=atrisk-scheduler.run.complete totalKids={} totalMessagesSent={} kidsSkipped={}",
                kids.size(), totalSent, totalSkipped);
    }

    private int processKid(User kid, Clock clock) {
        ZoneId kidZone = resolveZone(kid);
        LocalDate today = LocalDate.now(clock.withZone(kidZone));
        LocalTime nowLocal = LocalTime.now(clock.withZone(kidZone));

        log.debug("event=atrisk-scheduler.kid.check kidId={} kidName={} timezone={} localDate={} localTime={}",
                kid.getId(), kid.getDisplayName(), kidZone, today, nowLocal);

        // Send-time gate: don't send before the configured time in the kid's TZ
        if (nowLocal.isBefore(sendTime)) {
            log.info("event=atrisk-scheduler.kid.skip-before-send-time kidId={} kidName={} localTime={} sendTime={}",
                    kid.getId(), kid.getDisplayName(), nowLocal, sendTime);
            return 0;
        }

        LocalDate weekStart = calculator.mostRecentMonday(today);
        var assignments = assignmentRepository.findActiveForUser(kid.getId());
        log.info("event=atrisk-scheduler.kid.assignments kidId={} count={}", kid.getId(), assignments.size());

        int sent = 0;
        for (TaskAssignment assignment : assignments) {
            Task task = assignment.getTask();
            if (task.getWeeklyTarget() == null) {
                log.debug("event=atrisk-scheduler.task.skip-no-target kidId={} taskId={} taskTitle={}",
                        kid.getId(), task.getId(), task.getTitle());
                continue;
            }

            long done = completionRepository.countDoneForUserTask(kid.getId(), task.getId(), weekStart, today);
            boolean atRisk = calculator.isAtRisk(task.getWeeklyTarget(), task.getRemindLeadDays(), (int) done, today);

            log.info("event=atrisk-scheduler.task.evaluated kidId={} taskId={} taskTitle={} target={} done={} atRisk={}",
                    kid.getId(), task.getId(), task.getTitle(), task.getWeeklyTarget(), done, atRisk);

            if (!atRisk) continue;

            int remaining = task.getWeeklyTarget() - (int) done;
            String message = String.format(
                    "Reminder: do '%s' — done %d/%d this week, %d day(s) left.",
                    task.getTitle(), done, task.getWeeklyTarget(), daysLeft(today));

            sent += sendIfNotDuped(kid, task, message, today, clock);
        }

        return sent;
    }

    private int sendIfNotDuped(User kid, Task task, String message, LocalDate today, Clock clock) {
        // Window = [start-of-today UTC, end-of-today UTC]
        OffsetDateTime dayStart = today.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime dayEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        int sent = 0;

        if (kid.getTelegramChatId() != null && telegramSender.isConfigured()) {
            sent += sendChannel(kid, task, message, "TELEGRAM", dayStart, dayEnd, clock);
        }

        if (kid.getEmail() != null && !kid.getEmail().isBlank() && emailSender.isConfigured()) {
            sent += sendChannel(kid, task, message, "EMAIL", dayStart, dayEnd, clock);
        }

        if (kid.getTelegramChatId() == null && (kid.getEmail() == null || kid.getEmail().isBlank())) {
            log.warn("event=atrisk-scheduler.kid.no-contact kidId={} kidName={} taskId={} reason=no-contact-info",
                    kid.getId(), kid.getDisplayName(), task.getId());
        }

        return sent;
    }

    private int sendChannel(User kid, Task task, String message, String channel,
                            OffsetDateTime dayStart, OffsetDateTime dayEnd, Clock clock) {
        boolean alreadySent = logRepository.existsByUserIdAndTaskIdAndChannelAndSentAtBetween(
                kid.getId(), task.getId(), channel, dayStart, dayEnd);

        if (alreadySent) {
            log.info("event=atrisk-scheduler.dedup.skip kidId={} taskId={} channel={} reason=already-sent-today",
                    kid.getId(), task.getId(), channel);
            return 0;
        }

        log.info("event=atrisk-scheduler.send.attempt kidId={} taskId={} taskTitle={} channel={}",
                kid.getId(), task.getId(), task.getTitle(), channel);

        String subject = "Chore reminder: " + task.getTitle();
        SendResult result = switch (channel) {
            case "TELEGRAM" -> telegramSender.send(String.valueOf(kid.getTelegramChatId()), subject, message);
            case "EMAIL" -> emailSender.send(kid.getEmail(), subject, message);
            default -> SendResult.fail("Unknown channel: " + channel);
        };

        NotificationLog entry = buildLogEntry(kid, task, channel, result, message);
        logRepository.save(entry);

        if (result.ok()) {
            log.info("event=atrisk-scheduler.send.success kidId={} kidName={} taskId={} taskTitle={} channel={}",
                    kid.getId(), kid.getDisplayName(), task.getId(), task.getTitle(), channel);
            return 1;
        } else {
            log.warn("event=atrisk-scheduler.send.failed kidId={} taskId={} channel={} error={}",
                    kid.getId(), task.getId(), channel, result.error());
            return 0;
        }
    }

    private NotificationLog buildLogEntry(User kid, Task task, String channel, SendResult result, String message) {
        NotificationLog entry = new NotificationLog();
        entry.setUser(kid);
        entry.setTask(task);
        entry.setChannel(channel);
        entry.setStatus(result.ok() ? "SENT" : "FAILED");
        entry.setError(result.ok() ? null : result.error());
        entry.setMessage(message);
        return entry;
    }

    private ZoneId resolveZone(User kid) {
        String tz = kid.getTimezone();
        if (tz == null || tz.isBlank()) {
            log.warn("event=atrisk-scheduler.timezone.missing kidId={} fallback=Europe/London", kid.getId());
            return ZoneId.of("Europe/London");
        }
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            log.warn("event=atrisk-scheduler.timezone.invalid kidId={} tz={} fallback=Europe/London reason={}",
                    kid.getId(), tz, e.getMessage());
            return ZoneId.of("Europe/London");
        }
    }

    private static int daysLeft(LocalDate today) {
        return (7 - today.getDayOfWeek().getValue()) + 1;
    }
}
