package com.nila.chores.scheduler;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD unit tests for ReminderOverviewService.resolvePeriodRange(String, ZoneId).
 *
 * Verifies all 6 period keys produce correct [start, end] boundaries
 * anchored in the kid's timezone with Mon–Sun week semantics.
 *
 * Fixed clock: 2024-03-13 (Wednesday) 10:00 Europe/London.
 *   This week:  Mon 2024-03-11 → Wed 2024-03-13
 *   Last week:  Mon 2024-03-04 → Sun 2024-03-10
 *   This month: 2024-03-01 → 2024-03-13
 *   Last month: 2024-02-01 → 2024-02-29  (2024 is leap year)
 *   This year:  2024-01-01 → 2024-03-13
 *   Last year:  2023-01-01 → 2023-12-31
 */
class ReminderOverviewPeriodTest {

    private static final ZoneId LONDON = ZoneId.of("Europe/London");
    // Wednesday 2024-03-13 10:00 London (no DST yet – UTC same day)
    private static final Instant FIXED_INSTANT =
            LocalDate.of(2024, 3, 13).atTime(10, 0).atZone(LONDON).toInstant();
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, LONDON);

    /** Creates the service with all collaborators null — we only test the period math helper. */
    private ReminderOverviewService.PeriodRange range(String period) {
        return ReminderOverviewService.resolvePeriodRange(period, LONDON, FIXED_CLOCK);
    }

    // ── this-week ────────────────────────────────────────────────────────────

    @Test
    void thisWeek_startsOnMonday() {
        var r = range("this-week");
        assertThat(r.start()).isEqualTo(LocalDate.of(2024, 3, 11)); // Monday
        assertThat(r.end()).isEqualTo(LocalDate.of(2024, 3, 13));   // today (Wednesday)
    }

    // ── last-week ────────────────────────────────────────────────────────────

    @Test
    void lastWeek_priorMonToSun() {
        var r = range("last-week");
        assertThat(r.start()).isEqualTo(LocalDate.of(2024, 3, 4));  // prior Monday
        assertThat(r.end()).isEqualTo(LocalDate.of(2024, 3, 10));   // prior Sunday
    }

    // ── this-month ───────────────────────────────────────────────────────────

    @Test
    void thisMonth_firstOfMonthToToday() {
        var r = range("this-month");
        assertThat(r.start()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(r.end()).isEqualTo(LocalDate.of(2024, 3, 13));
    }

    // ── last-month ───────────────────────────────────────────────────────────

    @Test
    void lastMonth_priorFullMonthLeapYear() {
        // Feb 2024 is a leap year → last day is 29
        var r = range("last-month");
        assertThat(r.start()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(r.end()).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    // ── this-year ────────────────────────────────────────────────────────────

    @Test
    void thisYear_jan1ToToday() {
        var r = range("this-year");
        assertThat(r.start()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(r.end()).isEqualTo(LocalDate.of(2024, 3, 13));
    }

    // ── last-year ────────────────────────────────────────────────────────────

    @Test
    void lastYear_priorFullYear() {
        var r = range("last-year");
        assertThat(r.start()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(r.end()).isEqualTo(LocalDate.of(2023, 12, 31));
    }

    // ── default fallback ─────────────────────────────────────────────────────

    @Test
    void unknownPeriod_defaultsToThisWeek() {
        var r = range("garbage-value");
        // Should behave like this-week
        assertThat(r.start()).isEqualTo(LocalDate.of(2024, 3, 11));
        assertThat(r.end()).isEqualTo(LocalDate.of(2024, 3, 13));
    }

    // ── edge: period starts exactly on a Monday ───────────────────────────────

    @Test
    void thisWeek_whenTodayIsMonday_startEqualsToday() {
        // Fixed to Monday 2024-03-11
        Instant mondayInstant = LocalDate.of(2024, 3, 11).atTime(8, 0).atZone(LONDON).toInstant();
        Clock mondayClock = Clock.fixed(mondayInstant, LONDON);
        var r = ReminderOverviewService.resolvePeriodRange("this-week", LONDON, mondayClock);
        assertThat(r.start()).isEqualTo(LocalDate.of(2024, 3, 11));
        assertThat(r.end()).isEqualTo(LocalDate.of(2024, 3, 11));
    }

    // ── edge: last-week when today is Monday (last week = prior Mon–Sun) ─────

    @Test
    void lastWeek_whenTodayIsMonday_isFullPriorWeek() {
        Instant mondayInstant = LocalDate.of(2024, 3, 11).atTime(8, 0).atZone(LONDON).toInstant();
        Clock mondayClock = Clock.fixed(mondayInstant, LONDON);
        var r = ReminderOverviewService.resolvePeriodRange("last-week", LONDON, mondayClock);
        assertThat(r.start()).isEqualTo(LocalDate.of(2024, 3, 4));
        assertThat(r.end()).isEqualTo(LocalDate.of(2024, 3, 10));
    }

    // ── edge: last-month in January → December of prior year ─────────────────

    @Test
    void lastMonth_whenJanuary_isDecemberOfPriorYear() {
        Instant janInstant = LocalDate.of(2024, 1, 15).atTime(12, 0).atZone(LONDON).toInstant();
        Clock janClock = Clock.fixed(janInstant, LONDON);
        var r = ReminderOverviewService.resolvePeriodRange("last-month", LONDON, janClock);
        assertThat(r.start()).isEqualTo(LocalDate.of(2023, 12, 1));
        assertThat(r.end()).isEqualTo(LocalDate.of(2023, 12, 31));
    }
}
