package com.careflow.serviceops.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * Pure date maths for SLA deadlines: no Spring, no database, so every edge case
 * (weekends, holidays, daylight-saving changes, boundaries) is unit-testable.
 *
 * Rules:
 *  - 24x7 policies: deadline = start + N real minutes (the instant timeline, so a DST
 *    change inside the window correctly adds/removes a wall-clock hour).
 *  - Business-hours policies: the clock only runs inside [businessStart, businessEnd) on
 *    working days (a business day that is not a holiday), in the SITE's time zone. A start
 *    outside those hours begins counting at the next opening time.
 *  - A deadline that lands exactly on closing time stays at closing time; work starting
 *    exactly at closing time has no time left today and begins at the next opening.
 */
public final class SlaDeadlineCalculator {

    // Upper bound on how far we scan for a working day. Protects against a calendar that
    // is mis-configured so that no day ever qualifies.
    static final int MAX_SCAN_DAYS = 400;

    private SlaDeadlineCalculator() {
    }

    public static OffsetDateTime addMinutes(OffsetDateTime start, int minutes,
                                            boolean businessHoursOnly, SlaCalendar calendar) {
        if (minutes < 0) {
            throw new IllegalArgumentException("SLA minutes must not be negative.");
        }
        ZonedDateTime cursor = start.atZoneSameInstant(calendar.zone());
        if (!businessHoursOnly) {
            return cursor.plusMinutes(minutes).toOffsetDateTime();
        }

        long remaining = minutes * 60L;                 // work in seconds so no time is lost
        cursor = alignToBusinessTime(cursor, calendar);
        while (true) {
            ZonedDateTime closing = cursor.toLocalDate().atTime(calendar.businessEnd()).atZone(calendar.zone());
            long available = Duration.between(cursor, closing).getSeconds();   // real elapsed seconds
            if (remaining <= available) {
                return cursor.plusSeconds(remaining).toOffsetDateTime();
            }
            remaining -= available;
            cursor = alignToBusinessTime(cursor.toLocalDate().plusDays(1).atStartOfDay(calendar.zone()), calendar);
        }
    }

    /** Moves {@code from} forward to the nearest moment at which the SLA clock is running. */
    static ZonedDateTime alignToBusinessTime(ZonedDateTime from, SlaCalendar calendar) {
        ZonedDateTime candidate = from;
        for (int i = 0; i < MAX_SCAN_DAYS; i++) {
            LocalDate date = candidate.toLocalDate();
            if (calendar.isWorkingDay(date)) {
                ZonedDateTime opening = date.atTime(calendar.businessStart()).atZone(calendar.zone());
                ZonedDateTime closing = date.atTime(calendar.businessEnd()).atZone(calendar.zone());
                if (candidate.isBefore(opening)) return opening;
                if (candidate.isBefore(closing)) return candidate;
            }
            candidate = date.plusDays(1).atStartOfDay(calendar.zone());
        }
        throw new IllegalStateException(
                "No working day found within " + MAX_SCAN_DAYS + " days. Check the site's business days and holidays.");
    }
}