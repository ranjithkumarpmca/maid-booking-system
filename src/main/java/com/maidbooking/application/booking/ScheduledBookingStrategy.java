package com.maidbooking.application.booking;

import com.maidbooking.domain.exception.InvalidRequestException;
import com.maidbooking.domain.model.BookingType;
import com.maidbooking.domain.model.TimeSlot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** Book a specific future date and time. */
@Component
public class ScheduledBookingStrategy implements BookingStrategy {

    @Override
    public BookingType type() {
        return BookingType.SCHEDULED;
    }

    @Override
    public List<TimeSlot> resolveSlots(BookingCommand command, LocalDateTime now) {
        if (command.startTime() == null) {
            throw new InvalidRequestException("startTime is required for a scheduled booking");
        }
        if (!command.startTime().isAfter(now)) {
            throw new InvalidRequestException("A scheduled booking must start in the future");
        }
        return List.of(TimeSlot.of(command.startTime(), command.durationMinutes()));
    }
}
