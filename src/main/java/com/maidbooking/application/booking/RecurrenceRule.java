package com.maidbooking.application.booking;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * "Every weekday at 9am" = {MON..FRI}, 09:00, from startDate to endDate (inclusive).
 * A series is always bounded so that it can be priced, paid for and reserved up front.
 */
public record RecurrenceRule(Set<DayOfWeek> daysOfWeek, LocalTime time, LocalDate startDate, LocalDate endDate) {
}
