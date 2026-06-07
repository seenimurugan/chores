package com.nila.chores.scheduler;

import com.nila.chores.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

/**
 * Admin-only endpoint: trigger the at-risk reminder scheduler immediately.
 * Used for smoke-testing and operational on-demand runs.
 * The scheduler always deduplicates via notification_log so calling this multiple
 * times in a day is safe.
 */
@RestController
@RequestMapping("/api/admin/scheduler")
@PreAuthorize("hasRole('ADMIN')")
public class SchedulerTriggerController {

    private static final Logger log = LoggerFactory.getLogger(SchedulerTriggerController.class);

    private final AtRiskReminderScheduler scheduler;
    private final Clock clock;

    public SchedulerTriggerController(AtRiskReminderScheduler scheduler, Clock clock) {
        this.scheduler = scheduler;
        this.clock = clock;
    }

    /**
     * POST /api/admin/scheduler/run-reminders
     * Triggers an immediate at-risk reminder run using the scheduler's injected Clock bean
     * (consistent with the @Scheduled path; avoids Clock.systemDefaultZone() divergence in tests).
     *
     * @param actor authenticated admin
     * @return 200 with a plain status message
     */
    @PostMapping("/run-reminders")
    public ResponseEntity<String> triggerRun(@AuthenticationPrincipal AuthUser actor) {
        log.info("event=admin.scheduler.trigger.request actor={}", actor.id());
        scheduler.runReminders(clock);
        log.info("event=admin.scheduler.trigger.complete actor={}", actor.id());
        return ResponseEntity.ok("Reminder run triggered");
    }
}
