package com.maidbooking.application.payment;

import com.maidbooking.application.support.KeyedLocks;
import com.maidbooking.application.support.TypeRegistry;
import com.maidbooking.domain.exception.IdempotencyConflictException;
import com.maidbooking.domain.exception.InvalidRequestException;
import com.maidbooking.domain.exception.ResourceNotFoundException;
import com.maidbooking.domain.model.Booking;
import com.maidbooking.domain.model.BookingOccurrence;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.Payment;
import com.maidbooking.domain.model.PaymentMethodType;
import com.maidbooking.domain.payment.ChargeRequest;
import com.maidbooking.domain.payment.GatewayResult;
import com.maidbooking.domain.payment.PaymentGateway;
import com.maidbooking.domain.payment.PaymentMethod;
import com.maidbooking.domain.repository.BookingRepository;
import com.maidbooking.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Takes payment for a booking.
 * <ul>
 *   <li><b>Idempotency:</b> every request carries a client-chosen key. A repeat of the same request returns the
 *       stored outcome (success <em>or</em> failure) without charging again; the key reused for a different
 *       request is rejected. Retrying after a failure needs a new key.</li>
 *   <li><b>Recurring bookings:</b> each occurrence has its own price and records what was paid for it, so a series
 *       can be paid in one charge or occurrence by occurrence, and a single occurrence can later be refunded.</li>
 *   <li><b>Concurrency:</b> requests are serialised per idempotency key, then per booking.</li>
 * </ul>
 */
@Service
public class PaymentService {

    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final TypeRegistry<PaymentMethodType, PaymentMethod> methods;
    private final KeyedLocks locks;
    private final Clock clock;

    public PaymentService(BookingRepository bookings, PaymentRepository payments, PaymentGateway gateway,
                          List<PaymentMethod> methods, KeyedLocks locks, Clock clock) {
        this.bookings = bookings;
        this.payments = payments;
        this.gateway = gateway;
        this.methods = new TypeRegistry<>("payment method", methods, PaymentMethod::type);
        this.locks = locks;
        this.clock = clock;
    }

    public PaymentResult pay(PaymentCommand command) {
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new InvalidRequestException("An idempotency key is required");
        }
        if (command.method() == null) {
            throw new InvalidRequestException("Payment method is required");
        }
        return locks.withLock(KeyedLocks.idempotency(command.idempotencyKey()),
                () -> locks.withLock(KeyedLocks.booking(command.bookingId()), () -> payLocked(command)));
    }

    private PaymentResult payLocked(PaymentCommand command) {
        Optional<Payment> previous = payments.findByIdempotencyKey(command.idempotencyKey());
        if (previous.isPresent()) {
            return replay(previous.get(), command);
        }

        Booking booking = bookings.findById(command.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", command.bookingId()));
        List<String> occurrenceIds = occurrencesToPay(booking, command);
        booking.requirePayable(occurrenceIds);
        methods.get(command.method()).validate(command.details());

        Money amount = Money.sum(occurrenceIds.stream().map(id -> booking.occurrence(id).price()).toList());
        String paymentId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(clock);

        GatewayResult result = gateway.charge(new ChargeRequest(paymentId, command.method(), amount, command.details()));

        Payment payment;
        if (result.success()) {
            payment = Payment.succeeded(paymentId, booking.id(), amount, command.method(),
                    command.idempotencyKey(), occurrenceIds, result.reference(), now);
            booking.markPaid(occurrenceIds, paymentId);
            bookings.save(booking);
        } else {
            payment = Payment.failed(paymentId, booking.id(), amount, command.method(),
                    command.idempotencyKey(), occurrenceIds, result.failureReason(), now);
        }
        return new PaymentResult(payments.save(payment), false);
    }

    private PaymentResult replay(Payment previous, PaymentCommand command) {
        boolean sameRequest = previous.bookingId().equals(command.bookingId())
                && previous.method() == command.method()
                && (command.occurrenceIds().isEmpty()
                || command.occurrenceIds().equals(Set.copyOf(previous.occurrenceIds())));
        if (!sameRequest) {
            throw new IdempotencyConflictException(
                    "Idempotency key '" + command.idempotencyKey() + "' was already used for a different request");
        }
        return new PaymentResult(previous, true);
    }

    private static List<String> occurrencesToPay(Booking booking, PaymentCommand command) {
        if (command.occurrenceIds().isEmpty()) {
            return booking.unpaidActiveOccurrences().stream().map(BookingOccurrence::id).toList();
        }
        return List.copyOf(command.occurrenceIds());
    }
}
