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
     * GET /api/admin/kids/{id}/reminder-overview
     * Returns reminder overview for the specified kid.
     *
     * @param actor  authenticated admin
     * @param kidId  the kid's user ID
     * @return list of per-chore rows (200), empty array if no targeted chores, 404 if kid not found
     */
    @GetMapping("/{id}/reminder-overview")
    public ResponseEntity<List<ReminderOverviewService.ChoreOverviewRow>> getOverview(
            @AuthenticationPrincipal AuthUser actor,
            @PathVariable("id") Long kidId) {

        log.info("event=admin.reminder-overview.request actor={} kidId={}", actor.id(), kidId);

        List<ReminderOverviewService.ChoreOverviewRow> rows = overviewService.getOverviewForKid(kidId);

        log.info("event=admin.reminder-overview.response actor={} kidId={} rows={}", actor.id(), kidId, rows.size());
        return ResponseEntity.ok(rows);
    }
}
