package com.nila.chores.scheduler;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD unit tests for the at-risk calculation logic in AtRiskCalculator.
 *
 * Algorithm:
 *   C = completions done=true for (kid,chore) in [weekStart..today] (kid's local TZ)
 *   D = days left this week including today = (7 - isoDayOfWeek(today)) + 1
 *   remaining = T - C
 *   at-risk when: remaining > 0 AND D <= remaining + L
 *
 * Spec scenarios:
 *   T=5, L=0, C=0 → at-risk from Wednesday (D=4, remaining=5 → 4 <= 5 ✓)
 *   T=2, L=0, C=0 → at-risk Sat+Sun only (D=2, remaining=2 → 2 <= 2 ✓; D=3 → 3 <= 2 ✗)
 *   T=2, L=1, C=0 → at-risk Fri+Sat+Sun (D=3, remaining=2 → 3 <= 3 ✓; D=4 → 4 <= 3 ✗)
 */
class AtRiskCalculatorTest {

    private final AtRiskCalculator calc = new AtRiskCalculator();

    // ── Scenario 1: T=5, L=0, C=0 → at-risk from Wednesday ─────────────────

    @Test
    void t5_l0_c0_monday_notAtRisk() {
        // D=7 (Mon=1, D=7-1+1=7), remaining=5, 7 <= 5 → false
        assertThat(calc.isAtRisk(5, 0, 0, monday())).isFalse();
    }

    @Test
    void t5_l0_c0_tuesday_notAtRisk() {
        // D=6 (Tue=2, D=7-2+1=6), remaining=5, 6 <= 5 → false
        assertThat(calc.isAtRisk(5, 0, 0, tuesday())).isFalse();
    }

    @Test
    void t5_l0_c0_wednesday_atRisk() {
        // D=5 (Wed=3, D=7-3+1=5), remaining=5, 5 <= 5 → true
        assertThat(calc.isAtRisk(5, 0, 0, wednesday())).isTrue();
    }

    @Test
    void t5_l0_c0_thursday_atRisk() {
        // D=4 (Thu=4, D=7-4+1=4), remaining=5, 4 <= 5 → true
        assertThat(calc.isAtRisk(5, 0, 0, thursday())).isTrue();
    }

    @Test
    void t5_l0_c0_friday_atRisk() {
        assertThat(calc.isAtRisk(5, 0, 0, friday())).isTrue();
    }

    @Test
    void t5_l0_c0_saturday_atRisk() {
        assertThat(calc.isAtRisk(5, 0, 0, saturday())).isTrue();
    }

    @Test
    void t5_l0_c0_sunday_atRisk() {
        assertThat(calc.isAtRisk(5, 0, 0, sunday())).isTrue();
    }

    // ── Scenario 2: T=2, L=0, C=0 → at-risk Sat+Sun only ───────────────────

    @Test
    void t2_l0_c0_friday_notAtRisk() {
        // D=3 (Fri=5, D=7-5+1=3), remaining=2, 3 <= 2 → false
        assertThat(calc.isAtRisk(2, 0, 0, friday())).isFalse();
    }

    @Test
    void t2_l0_c0_saturday_atRisk() {
        // D=2 (Sat=6, D=7-6+1=2), remaining=2, 2 <= 2 → true
        assertThat(calc.isAtRisk(2, 0, 0, saturday())).isTrue();
    }

    @Test
    void t2_l0_c0_sunday_atRisk() {
        // D=1 (Sun=7, D=7-7+1=1), remaining=2, 1 <= 2 → true
        assertThat(calc.isAtRisk(2, 0, 0, sunday())).isTrue();
    }

    // ── Scenario 3: T=2, L=1, C=0 → at-risk Fri+Sat+Sun ───────────────────

    @Test
    void t2_l1_c0_thursday_notAtRisk() {
        // D=4 (Thu=4, D=7-4+1=4), remaining=2, 4 <= 2+1=3 → false
        assertThat(calc.isAtRisk(2, 1, 0, thursday())).isFalse();
    }

    @Test
    void t2_l1_c0_friday_atRisk() {
        // D=3 (Fri=5, D=7-5+1=3), remaining=2, 3 <= 2+1=3 → true
        assertThat(calc.isAtRisk(2, 1, 0, friday())).isTrue();
    }

    @Test
    void t2_l1_c0_saturday_atRisk() {
        assertThat(calc.isAtRisk(2, 1, 0, saturday())).isTrue();
    }

    @Test
    void t2_l1_c0_sunday_atRisk() {
        assertThat(calc.isAtRisk(2, 1, 0, sunday())).isTrue();
    }

    // ── Target met: never at-risk ─────────────────────────────────────────────

    @Test
    void targetMet_neverAtRisk() {
        // C=T: remaining=0 → at-risk requires remaining > 0
        assertThat(calc.isAtRisk(3, 0, 3, sunday())).isFalse();
    }

    @Test
    void targetExceeded_neverAtRisk() {
        assertThat(calc.isAtRisk(3, 0, 5, wednesday())).isFalse();
    }

    // ── weekStart calculation ─────────────────────────────────────────────────

    @Test
    void weekStart_mondayInput_returnsSameMonday() {
        LocalDate monday = monday();
        assertThat(calc.mostRecentMonday(monday)).isEqualTo(monday);
    }

    @Test
    void weekStart_wednesdayInput_returnsMonday() {
        LocalDate wed = wednesday();
        assertThat(calc.mostRecentMonday(wed)).isEqualTo(monday());
    }

    @Test
    void weekStart_sundayInput_returnsMonday() {
        LocalDate sun = sunday();
        assertThat(calc.mostRecentMonday(sun)).isEqualTo(monday());
    }

    // ── Timezone correctness ─────────────────────────────────────────────────

    /**
     * Verify that "today" in Asia/Kolkata vs Europe/London can be a different day,
     * which means at-risk status can differ per-kid per-timezone.
     * We just verify the timezone-local-date logic gives distinct days for a known
     * UTC moment that straddles midnight IST (e.g. 23:00 UTC = 04:30 IST next day).
     */
    @Test
    void todayInTimezone_kolkata_isAheadOfLondon() {
        // Pick a UTC instant: 2024-01-04T23:00:00Z (Thu in London, Fri in Kolkata IST +5:30)
        java.time.Instant instant = java.time.Instant.parse("2024-01-04T23:00:00Z");
        LocalDate inLondon = instant.atZone(ZoneId.of("Europe/London")).toLocalDate();
        LocalDate inKolkata = instant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();

        // London: 2024-01-04 (Thu); Kolkata: 2024-01-05 (Fri)
        assertThat(inKolkata).isAfter(inLondon);
        assertThat(inLondon.getDayOfWeek().getValue()).isEqualTo(4); // Thursday
        assertThat(inKolkata.getDayOfWeek().getValue()).isEqualTo(5); // Friday
    }

    // ── Helpers: fixed ISO week anchored to a known Mon–Sun ─────────────────
    // Use week of 2024-01-01 (Mon) through 2024-01-07 (Sun)

    private static LocalDate monday()    { return LocalDate.of(2024, 1, 1); }
    private static LocalDate tuesday()   { return LocalDate.of(2024, 1, 2); }
    private static LocalDate wednesday() { return LocalDate.of(2024, 1, 3); }
    private static LocalDate thursday()  { return LocalDate.of(2024, 1, 4); }
    private static LocalDate friday()    { return LocalDate.of(2024, 1, 5); }
    private static LocalDate saturday()  { return LocalDate.of(2024, 1, 6); }
    private static LocalDate sunday()    { return LocalDate.of(2024, 1, 7); }
}
