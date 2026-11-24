package com.maidbooking.application.booking;

import com.maidbooking.domain.exception.InvalidRequestException;
import com.maidbooking.domain.model.BookingType;
import com.maidbooking.domain.model.TimeSlot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Expands a weekly recurrence rule into one slot per matching day. Occurrences already in the past are skipped. */
@Component
public class RecurringBookingStrategy implements BookingStrategy {

    static final int MAX_OCCURRENCES = 60;
    static final int MAX_RANGE_DAYS = 366;

    @Override
    public BookingType type() {
        return BookingType.RECURRING;
    }

    @Override
    public List<TimeSlot> resolveSlots(BookingCommand command, LocalDateTime now) {
        RecurrenceRule rule = requireValidRule(command.recurrence());

        List<TimeSlot> slots = new ArrayList<>();
        for (LocalDate date = rule.startDate(); !date.isAfter(rule.endDate()); date = date.plusDays(1)) {
            LocalDateTime start = date.atTime(rule.time());
            if (rule.daysOfWeek().contains(date.getDayOfWeek()) && start.isAfter(now)) {
                slots.add(TimeSlot.of(start, command.durationMinutes()));
            }
        }
        if (slots.isEmpty()) {
            throw new InvalidRequestException("The recurrence has no upcoming occurrences");
        }
        if (slots.size() > MAX_OCCURRENCES) {
            throw new InvalidRequestException(
                    "A recurring booking may have at most " + MAX_OCCURRENCES + " occurrences, got " + slots.size());
        }
        return slots;
    }

    private static RecurrenceRule requireValidRule(RecurrenceRule rule) {
        if (rule == null) {
            throw new InvalidRequestException("recurrence is required for a recurring booking");
        }
        if (rule.daysOfWeek() == null || rule.daysOfWeek().isEmpty()) {
            throw new InvalidRequestException("recurrence.daysOfWeek must not be empty");
        }
        if (rule.time() == null || rule.startDate() == null || rule.endDate() == null) {
            throw new InvalidRequestException("recurrence.time, startDate and endDate are required");
        }
        if (rule.endDate().isBefore(rule.startDate())) {
            throw new InvalidRequestException("recurrence.endDate must not be before startDate");
        }
        if (rule.endDate().isAfter(rule.startDate().plusDays(MAX_RANGE_DAYS))) {
            throw new InvalidRequestException("A recurrence may span at most " + MAX_RANGE_DAYS + " days");
        }
        return rule;
    }
}
