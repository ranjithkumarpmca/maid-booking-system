package com.maidbooking.application.booking;

import com.maidbooking.domain.model.BookingType;
import com.maidbooking.domain.model.TimeSlot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Turns a booking request of one type into the concrete slots it needs. Everything else (availability,
 * pricing, reservation, persistence) is identical for all types and lives in BookingService, so a new
 * booking type is one new implementation of this interface.
 */
public interface BookingStrategy {

    BookingType type();

    /**
     * @throws com.maidbooking.domain.exception.InvalidRequestException if the command is invalid for this type
     */
    List<TimeSlot> resolveSlots(BookingCommand command, LocalDateTime now);
}
