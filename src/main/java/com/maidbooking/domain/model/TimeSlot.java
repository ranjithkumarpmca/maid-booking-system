package com.maidbooking.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A half-open interval [start, end). Two slots that merely touch (one ends when the other starts) do not overlap.
 */
public record TimeSlot(LocalDateTime start, LocalDateTime end) {

    public TimeSlot {
        Objects.requireNonNull(start, "start is required");
        Objects.requireNonNull(end, "end is required");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Slot start must be before end");
        }
    }

    public static TimeSlot of(LocalDateTime start, int durationMinutes) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        return new TimeSlot(start, start.plusMinutes(durationMinutes));
    }

    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public boolean isOnSingleDay() {
        return start.toLocalDate().equals(end.toLocalDate());
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public BigDecimal hours() {
        return BigDecimal.valueOf(duration().toMinutes()).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
    }
}
