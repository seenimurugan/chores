package com.nila.chores.scheduler;

import com.nila.chores.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only endpoint: per-kid reminder overview.
 * Returns a row per targeted chore with at-risk status and notification counts.
 */
@RestController
@RequestMapping("/api/admin/kids")
@PreAuthorize("hasRole('ADMIN')")
public class ReminderOverviewController {

    private static final Logger log = LoggerFactory.getLogger(ReminderOverviewController.class);

    private final ReminderOverviewService overviewService;

    public ReminderOverviewController(ReminderOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    /**
     * GET /api/admin/kids/{id}/reminder-overview?period=this-week
     * Returns reminder overview for the specified kid.
     *
     * <p>The {@code period} param controls the audit window for
     * completionsInPeriod / remindersSentInPeriod / lastReminderInPeriod.
     * Live current-week status (ON_TRACK / AT_RISK) and doneThisWeek are
     * always returned regardless of the selected period.</p>
     *
     * @param actor  authenticated admin
     * @param kidId  the kid's user ID
     * @param period one of: this-week (default), last-week, this-month, last-month, this-year, last-year
     * @return list of per-chore rows (200), empty array if no targeted chores, 404 if kid not found
     */
    @GetMapping("/{id}/reminder-overview")
    public ResponseEntity<List<ReminderOverviewService.ChoreOverviewRow>> getOverview(
            @AuthenticationPrincipal AuthUser actor,
            @PathVariable("id") Long kidId,
            @RequestParam(name = "period", defaultValue = "this-week") String period) {

        log.info("event=admin.reminder-overview.request actor={} kidId={} period={}", actor.id(), kidId, period);

        List<ReminderOverviewService.ChoreOverviewRow> rows = overviewService.getOverviewForKid(kidId, period);

        log.info("event=admin.reminder-overview.response actor={} kidId={} period={} rows={}",
                actor.id(), kidId, period, rows.size());
        return ResponseEntity.ok(rows);
    }
}
