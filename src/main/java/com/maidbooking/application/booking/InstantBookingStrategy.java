package com.maidbooking.application.booking;

import com.maidbooking.domain.model.BookingType;
import com.maidbooking.domain.model.TimeSlot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Book a maid who is free right now: the slot starts at the current minute. */
@Component
public class InstantBookingStrategy implements BookingStrategy {

    @Override
    public BookingType type() {
        return BookingType.INSTANT;
    }

    @Override
    public List<TimeSlot> resolveSlots(BookingCommand command, LocalDateTime now) {
        return List.of(TimeSlot.of(now.truncatedTo(ChronoUnit.MINUTES), command.durationMinutes()));
    }
}
