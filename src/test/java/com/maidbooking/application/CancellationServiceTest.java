package com.maidbooking.application;

import com.maidbooking.application.cancellation.CancellationResult;
import com.maidbooking.domain.exception.InvalidStateException;
import com.maidbooking.domain.exception.RefundFailedException;
import com.maidbooking.domain.model.Booking;
import com.maidbooking.domain.model.BookingStatus;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.OccurrenceStatus;
import com.maidbooking.domain.model.PaymentStatus;
import com.maidbooking.support.TestEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.Set;

import static com.maidbooking.support.TestEnvironment.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancellationServiceTest {

    private TestEnvironment env;
    private Maid maid;

    @BeforeEach
    void setUp() {
        env = new TestEnvironment();
        maid = env.addDefaultMaid(); // INR 200/h
    }

    @Test
    @DisplayName("cancelling with plenty of notice refunds in full and frees the slot")
    void fullRefundAndSlotFreed() {
        Booking booking = env.bookScheduled(maid, at(3, 10), 60);
        var slot = booking.occurrences().get(0).slot();
        env.payByUpi(booking, "k1");

        CancellationResult result = env.cancellationService.cancelSeries(booking.id());

        assertEquals(Money.of(200), result.totalRefund());
        assertEquals(BookingStatus.CANCELLED, result.booking().status());
        assertEquals(Money.of(200), env.gateway.refundedAmounts.get(0));
        assertTrue(env.inventory.isFree(maid.id(), slot), "the maid's slot must be bookable again");
        env.bookScheduled(maid, at(3, 10), 60); // and really is bookable
    }

    @Test
    @DisplayName("the freed slot shows up again in discovery")
    void slotRediscoverable() {
        Booking booking = env.bookScheduled(maid, at(3, 10), 60);
        var query = new com.maidbooking.application.search.MaidSearchQuery(booking.occurrences().get(0).slot(), null);
        assertTrue(env.searchService.search(query).isEmpty());

        env.cancellationService.cancelSeries(booking.id());

        assertEquals(1, env.searchService.search(query).size());
    }

    @Test
    @DisplayName("refund follows the notice period: half with 2-24h notice, none with less")
    void refundDependsOnNotice() {
        Booking halfRefund = env.bookScheduled(maid, at(0, 12), 60); // now is 08:00, 4h notice
        env.payByUpi(halfRefund, "k1");
        assertEquals(Money.of(100), env.cancellationService.cancelSeries(halfRefund.id()).totalRefund());

        Booking noRefund = env.bookScheduled(maid, at(0, 10), 60); // 2h notice -> exactly the 50% tier
        env.payByUpi(noRefund, "k2");
        env.clock.advance(Duration.ofMinutes(30)); // now 08:30, 90 minutes notice
        assertEquals(Money.ZERO, env.cancellationService.cancelSeries(noRefund.id()).totalRefund());
        assertEquals(1, env.gateway.refunds.get(), "no gateway call when nothing is refunded");
    }

    @Test
    @DisplayName("cancelling an unpaid booking refunds nothing but still frees the slot")
    void unpaidCancellation() {
        Booking booking = env.bookScheduled(maid, at(3, 10), 60);

        CancellationResult result = env.cancellationService.cancelSeries(booking.id());

        assertEquals(Money.ZERO, result.totalRefund());
        assertEquals(0, env.gateway.refunds.get());
        assertTrue(env.inventory.reservedSlots(maid.id()).isEmpty());
    }

    @Test
    @DisplayName("a single occurrence of a recurring booking can be cancelled while the rest stays")
    void cancelSingleOccurrence() {
        Booking series = env.bookRecurring(maid, Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), 0, 6);
        env.payByUpi(series, "k1"); // 3 x 200 charged
        String target = series.occurrences().get(1).id();

        CancellationResult result = env.cancellationService.cancelOccurrence(series.id(), target);

        assertEquals(Money.of(200), result.totalRefund());
        Booking reloaded = env.bookingService.get(series.id());
        assertEquals(BookingStatus.CONFIRMED, reloaded.status());
        assertEquals(OccurrenceStatus.CANCELLED, reloaded.occurrence(target).status());
        assertEquals(2, reloaded.activeOccurrences().size());
        assertEquals(2, env.inventory.reservedSlots(maid.id()).size());
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED,
                env.payments.findById(reloaded.occurrence(target).paymentId()).orElseThrow().status());
    }

    @Test
    @DisplayName("cancelling the whole series refunds every occurrence and cancels the booking")
    void cancelWholeSeries() {
        Booking series = env.bookRecurring(maid, Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), 0, 6);
        env.payByUpi(series, "k1");

        CancellationResult result = env.cancellationService.cancelSeries(series.id());

        assertEquals(Money.of(600), result.totalRefund());
        assertEquals(BookingStatus.CANCELLED, env.bookingService.get(series.id()).status());
        assertTrue(env.inventory.reservedSlots(maid.id()).isEmpty());
        assertEquals(PaymentStatus.REFUNDED,
                env.payments.findById(series.occurrences().get(0).paymentId()).orElseThrow().status());
    }

    @Test
    @DisplayName("cancelling the series skips occurrences that were already cancelled or already started")
    void seriesSkipsPastAndCancelled() {
        // Monday 09:00 occurrence is already in the past once the clock is at 10:00 Monday
        Booking series = env.bookRecurring(maid, Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY), 1, 7);
        String tuesday = series.occurrences().get(0).id();
        env.cancellationService.cancelOccurrence(series.id(), tuesday);

        CancellationResult result = env.cancellationService.cancelSeries(series.id());

        assertEquals(2, result.cancelledOccurrenceIds().size());
        assertEquals(BookingStatus.CANCELLED, result.booking().status());
    }

    @Test
    @DisplayName("invalid cancellations are rejected")
    void invalidCancellations() {
        Booking booking = env.bookScheduled(maid, at(3, 10), 60);
        env.cancellationService.cancelSeries(booking.id());
        String occurrenceId = booking.occurrences().get(0).id();

        assertThrows(InvalidStateException.class, () -> env.cancellationService.cancelSeries(booking.id()));
        assertThrows(InvalidStateException.class,
                () -> env.cancellationService.cancelOccurrence(booking.id(), occurrenceId));

        Booking started = env.bookScheduled(maid, at(0, 9), 60);
        env.clock.advance(Duration.ofMinutes(90)); // 09:30, the slot is under way
        assertThrows(InvalidStateException.class, () -> env.cancellationService.cancelSeries(started.id()));
    }

    @Test
    @DisplayName("if the gateway refund fails nothing is cancelled and the slot stays reserved")
    void refundFailureIsAtomic() {
        Booking booking = env.bookScheduled(maid, at(3, 10), 60);
        env.payByUpi(booking, "k1");
        env.gateway.failRefunds = true;

        assertThrows(RefundFailedException.class, () -> env.cancellationService.cancelSeries(booking.id()));

        Booking reloaded = env.bookingService.get(booking.id());
        assertEquals(BookingStatus.CONFIRMED, reloaded.status());
        assertEquals(1, reloaded.activeOccurrences().size());
        assertEquals(1, env.inventory.reservedSlots(maid.id()).size());

        env.gateway.failRefunds = false;
        assertEquals(Money.of(200), env.cancellationService.cancelSeries(booking.id()).totalRefund());
    }
}
