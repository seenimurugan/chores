package com.nila.chores.notification;

import com.nila.chores.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only endpoint: send a reminder for a specific task to all assigned kids
 * who have contact info configured.
 */
@RestController
@RequestMapping("/api/admin/tasks")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * POST /api/admin/tasks/{id}/send-reminder
     * Triggers an immediate reminder for the task to all assigned kids with contact info.
     *
     * @param actor  authenticated admin
     * @param taskId task to remind about
     * @return list of per-kid per-channel results (200 even if some channels fail — check individual results)
     */
    @PostMapping("/{id}/send-reminder")
    public ResponseEntity<List<NotificationService.ReminderResult>> sendReminder(
            @AuthenticationPrincipal AuthUser actor,
            @PathVariable("id") Long taskId) {

        log.info("event=admin.send-reminder.request actor={} taskId={}", actor.id(), taskId);

        List<NotificationService.ReminderResult> results = notificationService.sendReminderForTask(taskId);

        long sent = results.stream().filter(NotificationService.ReminderResult::sent).count();
        long skipped = results.stream().filter(NotificationService.ReminderResult::skipped).count();
        long failed = results.stream()
                .filter(r -> !r.sent() && !r.skipped()).count();

        log.info("event=admin.send-reminder.complete actor={} taskId={} sent={} skipped={} failed={}",
                actor.id(), taskId, sent, skipped, failed);

        return ResponseEntity.ok(results);
    }
}
