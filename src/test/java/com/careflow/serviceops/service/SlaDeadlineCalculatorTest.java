package com.careflow.serviceops.service;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlaDeadlineCalculatorTest {

    private static final Set<DayOfWeek> WEEKDAYS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    // 2026-10-09 is a Friday; 2026-10-12 is the following Monday.
    private static final SlaCalendar IST = calendar("Asia/Kolkata", "09:00", "18:00", WEEKDAYS, Set.of());

    private static SlaCalendar calendar(String zone, String open, String close, Set<DayOfWeek> days, Set<LocalDate> holidays) {
        return new SlaCalendar(ZoneId.of(zone), LocalTime.parse(open), LocalTime.parse(close), days, holidays);
    }

    private static void assertDeadline(String start, int minutes, boolean businessOnly, SlaCalendar calendar, String expected) {
        OffsetDateTime actual = SlaDeadlineCalculator.addMinutes(OffsetDateTime.parse(start), minutes, businessOnly, calendar);
        assertThat(actual.toInstant()).isEqualTo(OffsetDateTime.parse(expected).toInstant());
    }

    // ---- business hours ------------------------------------------------------------------

    @Test
    void staysOnTheSameDayWhenTimeFits() {
        assertDeadline("2026-10-09T10:00+05:30", 120, true, IST, "2026-10-09T12:00+05:30");
    }

    @Test
    void deadlineExactlyAtClosingTimeStaysAtClosingTime() {
        assertDeadline("2026-10-09T17:00+05:30", 60, true, IST, "2026-10-09T18:00+05:30");
    }

    @Test
    void oneMinutePastClosingRollsToNextWorkingDay() {
        assertDeadline("2026-10-09T17:00+05:30", 61, true, IST, "2026-10-12T09:01+05:30");
    }

    @Test
    void workStartingExactlyAtClosingTimeBeginsNextOpening() {
        assertDeadline("2026-10-09T18:00+05:30", 60, true, IST, "2026-10-12T10:00+05:30");
    }

    @Test
    void workStartingExactlyAtOpeningTimeCountsFromOpening() {
        assertDeadline("2026-10-12T09:00+05:30", 60, true, IST, "2026-10-12T10:00+05:30");
    }

    @Test
    void workRaisedBeforeOpeningStartsAtOpening() {
        assertDeadline("2026-10-12T06:00+05:30", 60, true, IST, "2026-10-12T10:00+05:30");
    }

    @Test
    void workRaisedOnSaturdayStartsMondayMorning() {
        assertDeadline("2026-10-10T11:00+05:30", 60, true, IST, "2026-10-12T10:00+05:30");
    }

    @Test
    void multiDayWindowSpansSeveralBusinessDays() {
        // 1,440 business minutes = 24 business hours = two 9h days plus 6h
        assertDeadline("2026-10-12T09:00+05:30", 1440, true, IST, "2026-10-14T15:00+05:30");
    }

    @Test
    void holidaysAreSkipped() {
        SlaCalendar withHoliday = calendar("Asia/Kolkata", "09:00", "18:00", WEEKDAYS, Set.of(LocalDate.of(2026, 10, 12)));
        assertDeadline("2026-10-09T17:00+05:30", 120, true, withHoliday, "2026-10-13T10:00+05:30");
    }

    @Test
    void deadlinesAreComputedInTheSiteTimeZoneNotUtc() {
        // 12:00 UTC on Friday is 17:30 IST: only 30 minutes of business time remain that day.
        assertDeadline("2026-10-09T12:00Z", 60, true, IST, "2026-10-12T09:30+05:30");
    }

    // ---- 24x7 ----------------------------------------------------------------------------

    @Test
    void twentyFourSevenIgnoresHoursAndWeekends() {
        assertDeadline("2026-10-09T17:00+05:30", 120, false, IST, "2026-10-09T19:00+05:30");
        assertDeadline("2026-10-10T11:00+05:30", 1440, false, IST, "2026-10-11T11:00+05:30");
    }

    // ---- daylight saving (America/New_York: 2026-03-08 spring forward, 2026-11-01 fall back) ----

    @Test
    void twentyFourSevenWindowAcrossSpringForwardIsTwentyFourRealHours() {
        SlaCalendar ny = calendar("America/New_York", "09:00", "17:00", WEEKDAYS, Set.of());
        assertDeadline("2026-03-07T12:00-05:00", 1440, false, ny, "2026-03-08T13:00-04:00");
    }

    @Test
    void twentyFourSevenWindowAcrossFallBackIsTwentyFourRealHours() {
        SlaCalendar ny = calendar("America/New_York", "09:00", "17:00", WEEKDAYS, Set.of());
        assertDeadline("2026-10-31T12:00-04:00", 1440, false, ny, "2026-11-01T11:00-05:00");
    }

    @Test
    void businessHoursRollingAcrossTheDstWeekendLandOnLocalWallClockTime() {
        SlaCalendar ny = calendar("America/New_York", "09:00", "17:00", WEEKDAYS, Set.of());
        // Fri 16:00 EST + 120 business minutes: 60 on Friday, 60 on Monday (now EDT) -> Mon 10:00 EDT
        assertDeadline("2026-03-06T16:00-05:00", 120, true, ny, "2026-03-09T10:00-04:00");
    }

    @Test
    void aBusinessWindowContainingTheSkippedHourIsOnlyThreeRealHoursLong() {
        // Sundays 01:00-05:00. On 8 March 02:00->03:00 is skipped, so that window has 3 real hours.
        SlaCalendar night = calendar("America/New_York", "01:00", "05:00", EnumSet.of(DayOfWeek.SUNDAY), Set.of());
        assertDeadline("2026-03-08T01:00-05:00", 240, true, night, "2026-03-15T02:00-04:00");
    }

    @Test
    void aBusinessWindowContainingTheRepeatedHourIsFiveRealHoursLong() {
        SlaCalendar night = calendar("America/New_York", "01:00", "05:00", EnumSet.of(DayOfWeek.SUNDAY), Set.of());
        assertDeadline("2026-11-01T01:00-04:00", 300, true, night, "2026-11-01T05:00-05:00");
    }
    private static final SlaCalendar NEW_YORK = calendar("America/New_York", "09:00", "18:00", WEEKDAYS, Set.of());

    @Test
    void newYorkBusinessHoursSkipTheWeekendAcrossTheNovemberDstChange() {
        // Fri 30 Oct 17:00 EDT: 60 min left on Friday, Sat/Sun closed, remaining 60 min Mon 2 Nov (now EST, -05:00)
        assertDeadline("2026-10-30T17:00-04:00", 120, true, NEW_YORK, "2026-11-02T10:00-05:00");
    }

    // ---- guard rails ---------------------------------------------------------------------

    @Test
    void anImpossibleCalendarFailsInsteadOfLoopingForever() {
        Set<LocalDate> everyDay = new java.util.HashSet<>();
        LocalDate day = LocalDate.of(2026, 10, 9);
        for (int i = 0; i < 500; i++) everyDay.add(day.plusDays(i));
        SlaCalendar closedForever = calendar("Asia/Kolkata", "09:00", "18:00", WEEKDAYS, everyDay);

        assertThatThrownBy(() -> SlaDeadlineCalculator.addMinutes(
                OffsetDateTime.parse("2026-10-09T10:00+05:30"), 60, true, closedForever))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void invalidCalendarsAreRejected() {
        assertThatThrownBy(() -> calendar("Asia/Kolkata", "18:00", "09:00", WEEKDAYS, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calendar("Asia/Kolkata", "09:00", "18:00", EnumSet.noneOf(DayOfWeek.class), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}