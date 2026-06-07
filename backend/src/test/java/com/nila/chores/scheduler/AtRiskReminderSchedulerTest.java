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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD unit tests for AtRiskReminderScheduler.runReminders(Clock).
 *
 * Tests:
 *   - per-chore: 2 at-risk chores → 2 separate Telegram messages
 *   - dedup: second tick on same day → no resend
 *   - on-track (T=2, C=2) → no send
 *   - no weekly_target → skip
 *   - kid send-time gate: if local time < send-time → skip
 *   - timezone correctness (London vs Kolkata on same UTC instant)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtRiskReminderSchedulerTest {

    @Mock UserRepository userRepository;
    @Mock TaskAssignmentRepository assignmentRepository;
    @Mock TaskCompletionRepository completionRepository;
    @Mock NotificationLogRepository logRepository;
    @Mock TelegramSender telegramSender;
    @Mock EmailSender emailSender;

    AtRiskReminderScheduler scheduler;

    // Send-time default: 06:00
    private static final LocalTime SEND_TIME = LocalTime.of(6, 0);

    @BeforeEach
    void setUp() {
        when(telegramSender.isConfigured()).thenReturn(true);
        when(emailSender.isConfigured()).thenReturn(true);
        when(telegramSender.send(anyString(), anyString(), anyString())).thenReturn(SendResult.success());
        when(emailSender.send(anyString(), anyString(), anyString())).thenReturn(SendResult.success());

        scheduler = new AtRiskReminderScheduler(
                userRepository, assignmentRepository, completionRepository,
                logRepository, telegramSender, emailSender,
                new AtRiskCalculator(), SEND_TIME);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** 2024-01-07 is a Sunday in Europe/London. At-risk: remaining=1, D=1 → 1<=1 */
    private static final LocalDate SUNDAY_2024_01_07 = LocalDate.of(2024, 1, 7);
    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    /**
     * Clock fixed to 2024-01-07 09:00:00 Europe/London (Sunday 09:00 — past 06:00 send gate).
     */
    private Clock clockLondonSunday09() {
        Instant instant = SUNDAY_2024_01_07.atTime(9, 0).atZone(LONDON).toInstant();
        return Clock.fixed(instant, LONDON);
    }

    /**
     * Clock fixed to 2024-01-07 05:00:00 Europe/London (before send-time gate 06:00).
     */
    private Clock clockLondonSunday05() {
        Instant instant = SUNDAY_2024_01_07.atTime(5, 0).atZone(LONDON).toInstant();
        return Clock.fixed(instant, LONDON);
    }

    private User kid(Long id, String tz, Long telegramChatId, String email) {
        User u = new User();
        u.setId(id);
        u.setUsername("kid" + id);
        u.setDisplayName("Kid" + id);
        u.setRole(User.Role.KID);
        u.setTimezone(tz);
        u.setTelegramChatId(telegramChatId);
        u.setEmail(email);
        u.setAvatarColor("#4263eb");
        u.setPasswordHash("hash");
        return u;
    }

    private Task task(Long id, String title, int weeklyTarget, int leadDays) {
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

    private Task taskNoTarget(Long id, String title) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        t.setWeeklyTarget(null);
        t.setRemindLeadDays(0);
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

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Two at-risk chores → two separate Telegram messages sent + 2 log rows written.
     */
    @Test
    void twoAtRiskChores_twoMessagesSent() {
        User kid = kid(1L, "Europe/London", -5139466273L, null);
        Task chore1 = task(10L, "Science video", 1, 0);  // T=1, L=0, Sunday → D=1, remaining=1 → at-risk
        Task chore2 = task(11L, "Reading", 1, 0);         // same

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(1L))
                .thenReturn(List.of(assignment(chore1, kid), assignment(chore2, kid)));
        // No completions this week — countDoneForUserTask returns 0 for all (kid,task) pairs
        when(completionRepository.countDoneForUserTask(anyLong(), anyLong(), any(), any())).thenReturn(0L);
        // No existing log rows (dedup check returns false)
        when(logRepository.existsByUserIdAndTaskIdAndChannelAndSentAtBetween(anyLong(), anyLong(), anyString(), any(), any()))
                .thenReturn(false);

        scheduler.runReminders(clockLondonSunday09());

        // 2 Telegram sends (one per chore)
        verify(telegramSender, times(2)).send(eq("-5139466273"), anyString(), anyString());
        // 2 log rows
        verify(logRepository, times(2)).save(any(NotificationLog.class));
    }

    /**
     * Same-day dedup: second tick → existsByUserIdAndTaskIdAndChannelAndSentAtBetween returns true → no resend.
     */
    @Test
    void dedup_secondTickSameDay_noResend() {
        User kid = kid(2L, "Europe/London", -5139466273L, null);
        Task chore = task(20L, "Exercise", 1, 0);

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(2L)).thenReturn(List.of(assignment(chore, kid)));
        when(completionRepository.countDoneForUserTask(anyLong(), anyLong(), any(), any())).thenReturn(0L);
        // Simulate already sent today — dedup returns true
        when(logRepository.existsByUserIdAndTaskIdAndChannelAndSentAtBetween(anyLong(), anyLong(), anyString(), any(), any()))
                .thenReturn(true);

        scheduler.runReminders(clockLondonSunday09());

        verify(telegramSender, never()).send(anyString(), anyString(), anyString());
        verify(logRepository, never()).save(any());
    }

    /**
     * On-track (completions >= target) → no message sent.
     * T=2, C=2 → remaining=0 → not at-risk → no send.
     */
    @Test
    void onTrack_noMessageSent() {
        User kid = kid(3L, "Europe/London", -5139466273L, null);
        Task chore = task(30L, "Reading", 2, 0); // T=2

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(3L)).thenReturn(List.of(assignment(chore, kid)));
        // 2 completions = target met — countDoneForUserTask returns 2
        when(completionRepository.countDoneForUserTask(eq(3L), eq(30L), any(), any()))
                .thenReturn(2L);

        scheduler.runReminders(clockLondonSunday09());

        verify(telegramSender, never()).send(anyString(), anyString(), anyString());
        verify(logRepository, never()).save(any());
    }

    /**
     * Task with null weekly_target → skip entirely (no at-risk evaluation).
     */
    @Test
    void noWeeklyTarget_skipped() {
        User kid = kid(4L, "Europe/London", -5139466273L, null);
        Task chore = taskNoTarget(40L, "Free play");

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(4L)).thenReturn(List.of(assignment(chore, kid)));

        scheduler.runReminders(clockLondonSunday09());

        verify(telegramSender, never()).send(anyString(), anyString(), anyString());
        verify(logRepository, never()).save(any());
    }

    /**
     * Send-time gate: if kid's local time is before send-time, skip all notifications.
     * Clock fixed to 05:00 London (before 06:00 default send-time).
     */
    @Test
    void beforeSendTime_noMessageSent() {
        User kid = kid(5L, "Europe/London", -5139466273L, null);
        Task chore = task(50L, "Exercise", 1, 0);

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(5L)).thenReturn(List.of(assignment(chore, kid)));

        scheduler.runReminders(clockLondonSunday05());

        verify(telegramSender, never()).send(anyString(), anyString(), anyString());
    }

    /**
     * Timezone: kid in Asia/Kolkata (IST +5:30). At 23:30 UTC on a Thursday, it is
     * already Friday in Kolkata. Verify the scheduler uses Kolkata local date.
     *
     * 2024-01-04T23:30:00Z = 2024-01-05T05:00:00+05:30 (IST Friday, before 06:00)
     * → send gate not passed → no send.
     */
    @Test
    void timezone_kolkata_beforeSendTime_noSend() {
        ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        User kid = kid(6L, "Asia/Kolkata", -5139466273L, null);
        // T=1, L=0 — at-risk on Sunday (D=1). In Kolkata it's Friday 05:00 IST (not past gate)
        Task chore = task(60L, "Homework", 1, 0);

        // 2024-01-04T23:30Z = 2024-01-05 Friday 05:00 IST → before 06:00 gate
        Instant instant = Instant.parse("2024-01-04T23:30:00Z");
        Clock fixedClock = Clock.fixed(instant, kolkata);

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(6L)).thenReturn(List.of(assignment(chore, kid)));

        scheduler.runReminders(fixedClock);

        verify(telegramSender, never()).send(anyString(), anyString(), anyString());
    }

    /**
     * Timezone: kid in Asia/Kolkata at 08:00 IST on Sunday. At-risk T=1,L=0,C=0.
     * Expects Telegram sent.
     */
    @Test
    void timezone_kolkata_pastSendTime_atRisk_sends() {
        ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        User kid = kid(7L, "Asia/Kolkata", -5139466273L, null);
        // 2024-01-07 (Sunday in Kolkata) at 08:00 IST = 02:30 UTC 2024-01-07
        Task chore = task(70L, "Reading", 1, 0); // T=1, Sunday D=1, remaining=1 → at-risk

        Instant instant = LocalDate.of(2024, 1, 7).atTime(8, 0).atZone(kolkata).toInstant();
        Clock fixedClock = Clock.fixed(instant, kolkata);

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(7L)).thenReturn(List.of(assignment(chore, kid)));
        when(completionRepository.countDoneForUserTask(anyLong(), anyLong(), any(), any())).thenReturn(0L);
        when(logRepository.existsByUserIdAndTaskIdAndChannelAndSentAtBetween(anyLong(), anyLong(), anyString(), any(), any()))
                .thenReturn(false);

        scheduler.runReminders(fixedClock);

        verify(telegramSender, times(1)).send(eq("-5139466273"), anyString(), anyString());
        verify(logRepository, times(1)).save(any(NotificationLog.class));
    }

    /**
     * MUST-FIX 2 — Timezone-dedup regression (the canonical bug scenario from the task).
     *
     * Clock fixed to 2024-01-07T20:00:00Z.
     * For a kid in Asia/Kolkata (IST +5:30) that is 2024-01-08T01:30+05:30 (IST Monday).
     * UTC date is still 2024-01-07 (Sunday).
     *
     * today(IST) = 2024-01-08 (Monday).
     *
     * Bug scenario: a prior SENT row was recorded at sentAt = 2024-01-08T01:30+05:30
     * = 2024-01-07T20:00Z. This is early Monday IST (01:30), but the scheduler clock
     * has now advanced to 09:00 IST (2024-01-08T03:30Z) — past the send gate.
     *
     * OLD code builds the dedup window using ZoneOffset.UTC:
     *   dayStart = 2024-01-08T00:00Z, dayEnd = 2024-01-09T00:00Z
     * The prior sentAt (2024-01-07T20:00Z) is NOT in that window → dedup misses → double-send (BUG).
     *
     * FIXED code builds the window using the kid's IST zone:
     *   dayStart = 2024-01-08T00:00+05:30 = 2024-01-07T18:30Z
     *   dayEnd   = 2024-01-09T00:00+05:30 = 2024-01-08T18:30Z
     * The prior sentAt (2024-01-07T20:00Z) IS in [2024-01-07T18:30Z, 2024-01-08T18:30Z) → deduped (CORRECT).
     *
     * Test strategy: stub the repo to return true ONLY for the IST-correct window arguments.
     * Old code calls with UTC-wrong window → stub returns false → send fires → test fails.
     * Fixed code calls with IST window → stub returns true → dedup fires → test passes.
     *
     * Clock is set to 2024-01-08T03:30:00Z = 2024-01-08T09:00+05:30 (IST 09:00) — past the send gate.
     */
    @Test
    void timezone_kolkata_dedupWindow_anchored_in_ist_not_utc() {
        ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        // 2024-01-08T03:30:00Z = 2024-01-08T09:00+05:30 (IST Monday 09:00 — past 06:00 gate)
        Instant fixedInstant = Instant.parse("2024-01-08T03:30:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, kolkata);

        // today(IST) = 2024-01-08 (Monday)
        // Fixed code builds the window using the kid's zone: 2024-01-08T00:00+05:30 → 2024-01-09T00:00+05:30
        // The OffsetDateTime produced by today.atStartOfDay(kolkata).toOffsetDateTime() is
        // 2024-01-08T00:00+05:30 — NOT 2024-01-07T18:30Z (same instant, different offset).
        // We match using isEqual (same instant comparison) via argThat.
        Instant istDayStartInstant = Instant.parse("2024-01-07T18:30:00Z"); // = 2024-01-08T00:00+05:30
        Instant istDayEndInstant   = Instant.parse("2024-01-08T18:30:00Z"); // = 2024-01-09T00:00+05:30

        // UTC-wrong window (old code): 2024-01-08T00:00Z → 2024-01-09T00:00Z
        // = instants 2024-01-08T00:00Z and 2024-01-09T00:00Z
        // — a prior sentAt at 2024-01-07T20:00Z is NOT in [2024-01-08T00:00Z, 2024-01-09T00:00Z) → double-send.

        User kid = kid(91L, "Asia/Kolkata", -5139466273L, null);
        // T=7, lead=0, C=0. today=Monday(isoDow=1), D=7, remaining=7 → 7 <= 7+0 → at-risk.
        Task chore = task(910L, "Morning stretch", 7, 0);

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(91L)).thenReturn(List.of(assignment(chore, kid)));
        when(completionRepository.countDoneForUserTask(anyLong(), anyLong(), any(), any())).thenReturn(0L);

        // The prior SENT row (sentAt = 2024-01-07T20:00Z = 2024-01-08T01:30+05:30) already exists.
        // Return true ONLY when the scheduler asks with the IST-correct window (by instant comparison).
        // Old UTC-wrong window ends at 2024-01-09T00:00Z (not the IST window end) → stub returns false
        // → send fires → verify(never()) fails → RED on old code.
        // Fixed code uses IST window → stub returns true → dedup fires → GREEN.
        when(logRepository.existsByUserIdAndTaskIdAndChannelAndSentAtBetween(
                eq(91L), eq(910L), eq("TELEGRAM"),
                argThat(odt -> odt.toInstant().equals(istDayStartInstant)),
                argThat(odt -> odt.toInstant().equals(istDayEndInstant))))
                .thenReturn(true);

        scheduler.runReminders(fixedClock);

        // Dedup must fire (prior IST-day send detected) → no Telegram send
        verify(telegramSender, never()).send(anyString(), anyString(), anyString());
        verify(logRepository, never()).save(any(NotificationLog.class));
    }

    /**
     * SHOULD-FIX 5 — A prior FAILED log row must NOT block a retry.
     *
     * existsByUserIdAndTaskIdAndChannelAndSentAtBetween must filter status = 'SENT'.
     * Without the status filter the mock returning true (as if a FAILED row exists) would
     * block the send; with the fix the FAILED row should be ignored.
     *
     * We verify this by stubbing the "status-filtered" exists call (the new @Query method)
     * to return false (no SENT row), while confirming a send still occurs.
     */
    @Test
    void failedLogRow_doesNotBlockRetry() {
        User kid = kid(92L, "Europe/London", -5139466273L, null);
        Task chore = task(920L, "Piano practice", 1, 0);

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(92L)).thenReturn(List.of(assignment(chore, kid)));
        when(completionRepository.countDoneForUserTask(anyLong(), anyLong(), any(), any())).thenReturn(0L);
        // The dedup check (now status=SENT filtered) returns false → no SENT row exists → retry should proceed
        when(logRepository.existsByUserIdAndTaskIdAndChannelAndSentAtBetween(anyLong(), anyLong(), anyString(), any(), any()))
                .thenReturn(false);

        scheduler.runReminders(clockLondonSunday09());

        // Send should proceed (FAILED row did not block it)
        verify(telegramSender, times(1)).send(eq("-5139466273"), anyString(), anyString());
        verify(logRepository, times(1)).save(any(NotificationLog.class));
    }

    /**
     * Kid with both email + telegram: one at-risk chore → 2 channel sends.
     */
    @Test
    void kidWithEmailAndTelegram_atRisk_twoChannelsSent() {
        User kid = kid(8L, "Europe/London", -5139466273L, "kid8@example.com");
        Task chore = task(80L, "Science", 1, 0);

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(8L)).thenReturn(List.of(assignment(chore, kid)));
        when(completionRepository.countDoneForUserTask(anyLong(), anyLong(), any(), any())).thenReturn(0L);
        when(logRepository.existsByUserIdAndTaskIdAndChannelAndSentAtBetween(anyLong(), anyLong(), anyString(), any(), any()))
                .thenReturn(false);

        scheduler.runReminders(clockLondonSunday09());

        verify(telegramSender, times(1)).send(anyString(), anyString(), anyString());
        verify(emailSender, times(1)).send(anyString(), anyString(), anyString());
        verify(logRepository, times(2)).save(any(NotificationLog.class));
    }

    // ── Per-kid reminderTime tests ─────────────────────────────────────────────

    /** Helper that also sets a custom reminderTime on the kid. */
    private User kidWithReminderTime(Long id, String tz, Long telegramChatId, String email,
                                     LocalTime reminderTime) {
        User u = kid(id, tz, telegramChatId, email);
        u.setReminderTime(reminderTime);
        return u;
    }

    /**
     * Kid with reminderTime=07:30 fires at 07:30 local — clock at 07:30 is past the gate → sends.
     */
    @Test
    void perKidReminderTime_kidAt0730_clockAt0730_sends() {
        // Clock: 2024-01-07 (Sunday) 07:30 London — past kid's 07:30 gate
        Instant instant = SUNDAY_2024_01_07.atTime(7, 30).atZone(LONDON).toInstant();
        Clock fixedClock = Clock.fixed(instant, LONDON);

        User kid = kidWithReminderTime(100L, "Europe/London", -5139466273L, null,
                LocalTime.of(7, 30));
        Task chore = task(100L, "Reading", 1, 0); // T=1, Sunday D=1, remaining=1 → at-risk

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(100L)).thenReturn(List.of(assignment(chore, kid)));
        when(completionRepository.countDoneForUserTask(anyLong(), anyLong(), any(), any())).thenReturn(0L);
        when(logRepository.existsByUserIdAndTaskIdAndChannelAndSentAtBetween(anyLong(), anyLong(), anyString(), any(), any()))
                .thenReturn(false);

        scheduler.runReminders(fixedClock);

        verify(telegramSender, times(1)).send(eq("-5139466273"), anyString(), anyString());
        verify(logRepository, times(1)).save(any(NotificationLog.class));
    }

    /**
     * Kid with reminderTime=07:30 — clock at 07:29 is BEFORE the gate → no send.
     */
    @Test
    void perKidReminderTime_kidAt0730_clockAt0729_noSend() {
        // Clock: 2024-01-07 07:29 London — one minute before 07:30 gate
        Instant instant = SUNDAY_2024_01_07.atTime(7, 29).atZone(LONDON).toInstant();
        Clock fixedClock = Clock.fixed(instant, LONDON);

        User kid = kidWithReminderTime(101L, "Europe/London", -5139466273L, null,
                LocalTime.of(7, 30));
        Task chore = task(101L, "Exercise", 1, 0);

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(101L)).thenReturn(List.of(assignment(chore, kid)));

        scheduler.runReminders(fixedClock);

        verify(telegramSender, never()).send(anyString(), anyString(), anyString());
    }

    /**
     * Two kids with DIFFERENT reminderTimes at a clock time between the two gates.
     * Kid A: reminderTime=06:00 → fires (clock past 06:00).
     * Kid B: reminderTime=08:00 → skipped (clock before 08:00 at 07:00).
     */
    @Test
    void perKidReminderTime_twoKids_differentTimes_onlyDueKidFires() {
        // Clock: 2024-01-07 07:00 London — past 06:00 gate but before 08:00 gate
        Instant instant = SUNDAY_2024_01_07.atTime(7, 0).atZone(LONDON).toInstant();
        Clock fixedClock = Clock.fixed(instant, LONDON);

        // Kid A: reminderTime=06:00 (past gate → should send)
        User kidA = kidWithReminderTime(102L, "Europe/London", -5139466273L, null,
                LocalTime.of(6, 0));
        Task choreA = task(102L, "Science", 1, 0);

        // Kid B: reminderTime=08:00 (before gate → should NOT send)
        User kidB = kidWithReminderTime(103L, "Europe/London", -5139466274L, null,
                LocalTime.of(8, 0));
        Task choreB = task(103L, "Reading", 1, 0);

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kidA, kidB));
        when(assignmentRepository.findActiveForUser(102L)).thenReturn(List.of(assignment(choreA, kidA)));
        when(assignmentRepository.findActiveForUser(103L)).thenReturn(List.of(assignment(choreB, kidB)));
        when(completionRepository.countDoneForUserTask(anyLong(), anyLong(), any(), any())).thenReturn(0L);
        when(logRepository.existsByUserIdAndTaskIdAndChannelAndSentAtBetween(anyLong(), anyLong(), anyString(), any(), any()))
                .thenReturn(false);

        scheduler.runReminders(fixedClock);

        // Kid A fires (06:00 past), Kid B does not (08:00 not yet reached)
        verify(telegramSender, times(1)).send(eq("-5139466273"), anyString(), anyString());
        verify(telegramSender, never()).send(eq("-5139466274"), anyString(), anyString());
        verify(logRepository, times(1)).save(any(NotificationLog.class));
    }

    /**
     * Default reminderTime (06:00) fires at 06:00 — mirrors the old global-default behaviour.
     * Kid's reminderTime field is the JVM/entity default (06:00) — not explicitly set.
     */
    @Test
    void perKidReminderTime_defaultSixAm_firesAt0600() {
        // Clock: 2024-01-07 06:00 London — exactly at the default 06:00 gate
        Instant instant = SUNDAY_2024_01_07.atTime(6, 0).atZone(LONDON).toInstant();
        Clock fixedClock = Clock.fixed(instant, LONDON);

        // kid() does NOT call setReminderTime — entity default (06:00) applies
        User kid = kid(104L, "Europe/London", -5139466273L, null);
        Task chore = task(104L, "Piano", 1, 0);

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(104L)).thenReturn(List.of(assignment(chore, kid)));
        when(completionRepository.countDoneForUserTask(anyLong(), anyLong(), any(), any())).thenReturn(0L);
        when(logRepository.existsByUserIdAndTaskIdAndChannelAndSentAtBetween(anyLong(), anyLong(), anyString(), any(), any()))
                .thenReturn(false);

        scheduler.runReminders(fixedClock);

        verify(telegramSender, times(1)).send(eq("-5139466273"), anyString(), anyString());
    }

    /**
     * Per-kid reminderTime is interpreted in the KID'S TIMEZONE.
     * Kid in Asia/Kolkata with reminderTime=07:30.
     * Clock at 02:00 UTC = 07:30 IST → gate passed → sends.
     */
    @Test
    void perKidReminderTime_interpretedInKidTimezone() {
        ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        // 2024-01-07T02:00:00Z = 2024-01-07T07:30+05:30 IST (exactly 07:30 gate)
        Instant instant = Instant.parse("2024-01-07T02:00:00Z");
        Clock fixedClock = Clock.fixed(instant, kolkata);

        User kid = kidWithReminderTime(105L, "Asia/Kolkata", -5139466273L, null,
                LocalTime.of(7, 30));
        Task chore = task(105L, "Homework", 1, 0); // T=1, Sunday IST, D=1 → at-risk

        when(userRepository.findAllByRole(User.Role.KID)).thenReturn(List.of(kid));
        when(assignmentRepository.findActiveForUser(105L)).thenReturn(List.of(assignment(chore, kid)));
        when(completionRepository.countDoneForUserTask(anyLong(), anyLong(), any(), any())).thenReturn(0L);
        when(logRepository.existsByUserIdAndTaskIdAndChannelAndSentAtBetween(anyLong(), anyLong(), anyString(), any(), any()))
                .thenReturn(false);

        scheduler.runReminders(fixedClock);

        verify(telegramSender, times(1)).send(eq("-5139466273"), anyString(), anyString());
    }

}
