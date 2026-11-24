package com.maidbooking.application.booking;

import com.maidbooking.domain.model.BookingType;
import com.maidbooking.domain.model.ServiceType;

import java.time.LocalDateTime;

/**
 * One command for every booking type; each {@link BookingStrategy} reads only the fields it needs
 * (instant: none, scheduled: startTime, recurring: recurrence).
 */
public record BookingCommand(String customerId, String maidId, ServiceType serviceType, BookingType type,
                             LocalDateTime startTime, int durationMinutes, RecurrenceRule recurrence) {
}
