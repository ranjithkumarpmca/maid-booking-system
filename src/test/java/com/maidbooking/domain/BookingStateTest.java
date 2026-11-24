package com.maidbooking.domain;

import com.maidbooking.domain.exception.InvalidStateException;
import com.maidbooking.domain.model.Booking;
import com.maidbooking.domain.model.BookingOccurrence;
import com.maidbooking.domain.model.BookingStatus;
import com.maidbooking.domain.model.BookingType;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.OccurrenceStatus;
import com.maidbooking.domain.model.ServiceType;
import com.maidbooking.domain.model.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingStateTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 12, 1, 9, 0);

    private static Booking series(int occurrences) {
        List<BookingOccurrence> list = new java.util.ArrayList<>();
        for (int i = 0; i < occurrences; i++) {
            list.add(new BookingOccurrence("o" + i, TimeSlot.of(BASE.plusDays(i), 60), Money.of(200)));
        }
        return new Booking("b1", "c1", "m1", ServiceType.CLEANING, BookingType.RECURRING, list, BASE.minusDays(1));
    }

    @Test
    @DisplayName("a new booking is pending payment and becomes confirmed once paid")
    void payConfirms() {
        Booking booking = series(1);
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.status());

        booking.markPaid(List.of("o0"), "pay-1");

        assertEquals(BookingStatus.CONFIRMED, booking.status());
        assertTrue(booking.occurrence("o0").isPaid());
        assertEquals(Money.of(200), booking.occurrence("o0").paidAmount());
    }

    @Test
    @DisplayName("an occurrence cannot be paid twice")
    void noDoublePayment() {
        Booking booking = series(1);
        booking.markPaid(List.of("o0"), "pay-1");
        assertThrows(InvalidStateException.class, () -> booking.markPaid(List.of("o0"), "pay-2"));
    }

    @Test
    @DisplayName("a payment naming an unpayable occurrence changes nothing")
    void paymentIsAllOrNothing() {
        Booking booking = series(3);
        booking.markPaid(List.of("o0"), "pay-1");

        assertThrows(InvalidStateException.class, () -> booking.markPaid(List.of("o1", "o0"), "pay-2"));

        assertFalse(booking.occurrence("o1").isPaid(), "o1 must stay unpaid when the request fails as a whole");
    }

    @Test
    @DisplayName("cancelling one occurrence leaves the booking alive; cancelling the last one cancels it")
    void cancellationCascades() {
        Booking booking = series(2);
        booking.markPaid(List.of("o0", "o1"), "pay-1");

        booking.cancelOccurrence("o0", Money.of(200));
        assertEquals(BookingStatus.CONFIRMED, booking.status());
        assertEquals(OccurrenceStatus.CANCELLED, booking.occurrence("o0").status());
        assertEquals(Money.of(200), booking.occurrence("o0").refundedAmount());
        assertEquals(Money.of(200), booking.totalAmount(), "only the active occurrence counts");

        booking.cancelOccurrence("o1", Money.ZERO);
        assertEquals(BookingStatus.CANCELLED, booking.status());
    }

    @Test
    @DisplayName("a cancelled booking or occurrence cannot be cancelled or paid again")
    void invalidTransitions() {
        Booking booking = series(1);
        booking.cancelOccurrence("o0", Money.ZERO);

        assertThrows(InvalidStateException.class, () -> booking.cancelOccurrence("o0", Money.ZERO));
        assertThrows(InvalidStateException.class, () -> booking.markPaid(List.of("o0"), "pay-1"));
    }

    @Test
    @DisplayName("status transitions are restricted")
    void statusTransitions() {
        assertTrue(BookingStatus.PENDING_PAYMENT.canTransitionTo(BookingStatus.CONFIRMED));
        assertTrue(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.CANCELLED));
        assertFalse(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.PENDING_PAYMENT));
        assertFalse(BookingStatus.CANCELLED.canTransitionTo(BookingStatus.CONFIRMED));
    }
}
