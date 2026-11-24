package com.maidbooking.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A recurring weekly working window of a maid, e.g. every Monday 08:00-14:00.
 * Whether a maid is free inside a window is decided by what is already reserved (see SlotInventory).
 */
public record AvailabilityWindow(DayOfWeek day, LocalTime start, LocalTime end) {

    public AvailabilityWindow {
        Objects.requireNonNull(day, "day is required");
        Objects.requireNonNull(start, "start is required");
        Objects.requireNonNull(end, "end is required");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Availability window start must be before end");
        }
    }

    /** True when the slot lies entirely inside this window (slots cannot span midnight). */
    public boolean covers(TimeSlot slot) {
        return slot.isOnSingleDay()
                && slot.start().getDayOfWeek() == day
                && !slot.start().toLocalTime().isBefore(start)
                && !slot.end().toLocalTime().isAfter(end);
    }

    public boolean overlaps(AvailabilityWindow other) {
        return day == other.day && start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** The concrete slot this window produces on the given date, if the date falls on this window's weekday. */
    public Optional<TimeSlot> on(LocalDate date) {
        if (date.getDayOfWeek() != day) {
            return Optional.empty();
        }
        return Optional.of(new TimeSlot(date.atTime(start), date.atTime(end)));
    }
}
