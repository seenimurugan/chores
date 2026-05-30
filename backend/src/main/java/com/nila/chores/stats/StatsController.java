package com.nila.chores.stats;

import com.nila.chores.security.AuthUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class StatsController {
    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping("/me/stats")
    @PreAuthorize("isAuthenticated()")
    public StatsService.KidStats myStats(@AuthenticationPrincipal AuthUser user,
                                         @RequestParam(value = "days", defaultValue = "14") int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(0, days - 1));
        return stats.forUser(user.id(), from, to);
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StatsService.KidStats> allKidStats(@RequestParam(value = "days", defaultValue = "14") int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(0, days - 1));
        return stats.forAllKids(from, to);
    }

    @GetMapping("/me/stats/matrix")
    @PreAuthorize("isAuthenticated()")
    public StatsService.KidMatrix myMatrix(@AuthenticationPrincipal AuthUser user,
                                           @RequestParam(value = "days", defaultValue = "14") int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(0, days - 1));
        return stats.matrixForUser(user.id(), from, to);
    }

    @GetMapping("/admin/stats/matrix")
    @PreAuthorize("hasRole('ADMIN')")
    public StatsService.AdminMatrix adminMatrix(@RequestParam(value = "days", defaultValue = "14") int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(0, days - 1));
        return stats.matrixForAllKids(from, to);
    }

    @GetMapping("/admin/stats/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public StatsService.KidStats kidStats(@PathVariable Long userId,
                                          @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to == null) to = LocalDate.now();
        if (from == null) from = to.minusDays(13);
        if (from.isAfter(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from > to");
        return stats.forUser(userId, from, to);
    }
}
