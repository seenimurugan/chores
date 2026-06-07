package com.nila.chores.scheduler;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Pure, stateless at-risk calculation logic.
 *
 * Algorithm (per kid's timezone, week Mon–Sun):
 *   C = completions done=true for (kid, chore) in [weekStart..today]
 *   D = days left this week including today = (7 - isoDayOfWeek(today)) + 1
 *   remaining = T - C
 *   at-risk ⟺ remaining > 0 AND D <= remaining + L
 *
 * This class is injectable (Spring @Component) so callers can mock it in tests.
 */
@Component
public class AtRiskCalculator {

    /**
     * Returns {@code true} if the chore is at-risk given the current state.
     *
     * @param weeklyTarget   T — required completions per week
     * @param remindLeadDays L — how many extra days of lead to trigger the reminder
     * @param doneThisWeek   C — completions done=true in [weekStart..today] for this kid+chore
     * @param today          the kid's local date (in their timezone)
     * @return true if the kid is at risk of not meeting the weekly target
     */
    public boolean isAtRisk(int weeklyTarget, int remindLeadDays, int doneThisWeek, LocalDate today) {
        int remaining = weeklyTarget - doneThisWeek;
        if (remaining <= 0) {
            return false;
        }
        // ISO day of week: Mon=1, Tue=2, ..., Sun=7
        int isoDow = today.getDayOfWeek().getValue();
        int daysLeft = (7 - isoDow) + 1;
        return daysLeft <= remaining + remindLeadDays;
    }

    /**
     * Returns the most recent Monday on or before {@code date}.
     * Used to compute the start of the ISO week.
     *
     * @param date any date
     * @return the Monday of that ISO week
     */
    public LocalDate mostRecentMonday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
