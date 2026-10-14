package com.careflow.serviceops.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * Everything the deadline calculator needs to know about one site's working week.
 * All times are wall-clock times in the site's own time zone.
 */
public record SlaCalendar(ZoneId zone, LocalTime businessStart, LocalTime businessEnd,
                          Set<DayOfWeek> businessDays, Set<LocalDate> holidays) {

    public SlaCalendar {
        if (!businessStart.isBefore(businessEnd)) {
            throw new IllegalArgumentException("Business hours must start before they end.");
        }
        if (businessDays.isEmpty()) {
            throw new IllegalArgumentException("A site calendar needs at least one business day.");
        }
        businessDays = Set.copyOf(businessDays);
        holidays = Set.copyOf(holidays);
    }

    public boolean isWorkingDay(LocalDate date) {
        return businessDays.contains(date.getDayOfWeek()) && !holidays.contains(date);
    }
}