package com.maidbooking.application.cancellation;

import com.maidbooking.application.support.KeyedLocks;
import com.maidbooking.domain.exception.InvalidStateException;
import com.maidbooking.domain.exception.RefundFailedException;
import com.maidbooking.domain.exception.ResourceNotFoundException;
import com.maidbooking.domain.model.Booking;
import com.maidbooking.domain.model.BookingOccurrence;
import com.maidbooking.domain.model.BookingStatus;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.Payment;
import com.maidbooking.domain.payment.GatewayResult;
import com.maidbooking.domain.payment.PaymentGateway;
import com.maidbooking.domain.refund.RefundPolicy;
import com.maidbooking.domain.repository.BookingRepository;
import com.maidbooking.domain.repository.PaymentRepository;
import com.maidbooking.domain.repository.SlotInventory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Cancels one occurrence or a whole series. For each cancelled occurrence the {@link RefundPolicy} decides the
 * refund, the money goes back through the gateway, and the maid's slot is released so it can be found and
 * booked again. Occurrences that have already started cannot be cancelled.
 */
@Service
public class CancellationService {

    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final SlotInventory inventory;
    private final RefundPolicy refundPolicy;
    private final KeyedLocks locks;
    private final Clock clock;

    public CancellationService(BookingRepository bookings, PaymentRepository payments, PaymentGateway gateway,
                               SlotInventory inventory, RefundPolicy refundPolicy, KeyedLocks locks, Clock clock) {
        this.bookings = bookings;
        this.payments = payments;
        this.gateway = gateway;
        this.inventory = inventory;
        this.refundPolicy = refundPolicy;
        this.locks = locks;
        this.clock = clock;
    }

    public CancellationResult cancelOccurrence(String bookingId, String occurrenceId) {
        return cancel(bookingId, (booking, now) -> List.of(booking.occurrence(occurrenceId)));
    }

    /** Cancels every occurrence that is still active and has not started yet. */
    public CancellationResult cancelSeries(String bookingId) {
        return cancel(bookingId, (booking, now) -> booking.activeOccurrences().stream()
                .filter(occurrence -> occurrence.slot().start().isAfter(now))
                .toList());
    }

    private CancellationResult cancel(String bookingId,
                                      BiFunction<Booking, LocalDateTime, List<BookingOccurrence>> selector) {
        return locks.withLock(KeyedLocks.booking(bookingId), () -> {
            Booking booking = bookings.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
            if (booking.status() == BookingStatus.CANCELLED) {
                throw new InvalidStateException("Booking " + bookingId + " is already cancelled");
            }
            LocalDateTime now = LocalDateTime.now(clock);
            List<BookingOccurrence> targets = selector.apply(booking, now);
            requireCancellable(targets, now);

            Map<BookingOccurrence, Money> refunds = new LinkedHashMap<>();
            for (BookingOccurrence target : targets) {
                refunds.put(target, refundPolicy.refundFor(target.paidAmount(), target.slot(), now));
            }
            Map<Payment, Money> refundsByPayment = groupByPayment(refunds);

            // Money first: if the gateway refuses, nothing else has changed.
            refundsByPayment.forEach(this::refundViaGateway);

            refundsByPayment.forEach((payment, amount) -> {
                payment.recordRefund(amount);
                payments.save(payment);
            });
            refunds.forEach((occurrence, refund) -> {
                booking.cancelOccurrence(occurrence.id(), refund);
                inventory.release(booking.maidId(), occurrence.id());
            });
            bookings.save(booking);

            return new CancellationResult(booking, Money.sum(refunds.values()),
                    targets.stream().map(BookingOccurrence::id).toList());
        });
    }

    private static void requireCancellable(List<BookingOccurrence> targets, LocalDateTime now) {
        if (targets.isEmpty()) {
            throw new InvalidStateException("Nothing to cancel: no upcoming active occurrences");
        }
        for (BookingOccurrence target : targets) {
            if (!target.isActive()) {
                throw new InvalidStateException("Occurrence " + target.id() + " is already cancelled");
            }
            if (!target.slot().start().isAfter(now)) {
                throw new InvalidStateException("Occurrence " + target.id() + " has already started");
            }
        }
    }

    private Map<Payment, Money> groupByPayment(Map<BookingOccurrence, Money> refunds) {
        Map<String, Money> totals = new LinkedHashMap<>();
        refunds.forEach((occurrence, refund) -> {
            if (occurrence.paymentId() != null && !refund.isZero()) {
                totals.merge(occurrence.paymentId(), refund, Money::plus);
            }
        });
        Map<Payment, Money> byPayment = new LinkedHashMap<>();
        totals.forEach((paymentId, amount) -> byPayment.put(
                payments.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId)),
                amount));
        return byPayment;
    }

    private void refundViaGateway(Payment payment, Money amount) {
        GatewayResult result = gateway.refund(payment.gatewayReference(), amount);
        if (!result.success()) {
            throw new RefundFailedException("Refund of " + amount + " for payment " + payment.id()
                    + " failed: " + result.failureReason());
        }
    }
}
