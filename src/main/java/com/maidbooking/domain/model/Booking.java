package com.maidbooking.domain.model;

import com.maidbooking.domain.exception.InvalidStateException;
import com.maidbooking.domain.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Aggregate root for a booking and its occurrences. All state transitions go through this class so the
 * invariants (valid status moves, no double payment, no cancelling twice) live in one place.
 * Not thread-safe by itself: services serialise mutations per booking (see KeyedLocks).
 */
public final class Booking {

    private final String id;
    private final String customerId;
    private final String maidId;
    private final ServiceType serviceType;
    private final BookingType bookingType;
    private final List<BookingOccurrence> occurrences;
    private final LocalDateTime createdAt;
    private volatile BookingStatus status = BookingStatus.PENDING_PAYMENT;

    public Booking(String id, String customerId, String maidId, ServiceType serviceType, BookingType bookingType,
                   List<BookingOccurrence> occurrences, LocalDateTime createdAt) {
        if (occurrences == null || occurrences.isEmpty()) {
            throw new IllegalArgumentException("A booking needs at least one occurrence");
        }
        this.id = id;
        this.customerId = customerId;
        this.maidId = maidId;
        this.serviceType = serviceType;
        this.bookingType = bookingType;
        this.occurrences = List.copyOf(occurrences);
        this.createdAt = createdAt;
    }

    public String id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public String maidId() {
        return maidId;
    }

    public ServiceType serviceType() {
        return serviceType;
    }

    public BookingType bookingType() {
        return bookingType;
    }

    public BookingStatus status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public List<BookingOccurrence> occurrences() {
        return Collections.unmodifiableList(occurrences);
    }

    public Optional<BookingOccurrence> findOccurrence(String occurrenceId) {
        return occurrences.stream().filter(o -> o.id().equals(occurrenceId)).findFirst();
    }

    public BookingOccurrence occurrence(String occurrenceId) {
        return findOccurrence(occurrenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Occurrence", occurrenceId));
    }

    public List<BookingOccurrence> activeOccurrences() {
        return occurrences.stream().filter(BookingOccurrence::isActive).toList();
    }

    public List<BookingOccurrence> unpaidActiveOccurrences() {
        return occurrences.stream().filter(o -> o.isActive() && !o.isPaid()).toList();
    }

    /** Total price of everything still active in this booking. */
    public Money totalAmount() {
        return Money.sum(activeOccurrences().stream().map(BookingOccurrence::price).toList());
    }

    /**
     * Fails unless every given occurrence can be paid right now. Called before money is charged, so a bad
     * request never reaches the payment gateway.
     */
    public void requirePayable(Collection<String> occurrenceIds) {
        if (status == BookingStatus.CANCELLED) {
            throw new InvalidStateException("Booking " + id + " is cancelled and cannot be paid");
        }
        if (occurrenceIds.isEmpty()) {
            throw new InvalidStateException("Nothing left to pay for in booking " + id);
        }
        for (String occurrenceId : occurrenceIds) {
            BookingOccurrence occurrence = occurrence(occurrenceId);
            if (!occurrence.isActive() || occurrence.isPaid()) {
                throw new InvalidStateException("Occurrence " + occurrenceId + " is not payable");
            }
        }
    }

    /**
     * Records a successful payment for the given occurrences. Validates all of them before changing any,
     * so a bad id never leaves the booking half-paid.
     */
    public void markPaid(Collection<String> occurrenceIds, String paymentId) {
        requirePayable(occurrenceIds);
        occurrenceIds.forEach(occurrenceId -> occurrence(occurrenceId).markPaid(paymentId));
        if (status == BookingStatus.PENDING_PAYMENT) {
            transitionTo(BookingStatus.CONFIRMED);
        }
    }

    /** Cancels one occurrence; the booking itself is cancelled once no occurrence is left active. */
    public void cancelOccurrence(String occurrenceId, Money refund) {
        if (status == BookingStatus.CANCELLED) {
            throw new InvalidStateException("Booking " + id + " is already cancelled");
        }
        occurrence(occurrenceId).cancel(refund);
        if (activeOccurrences().isEmpty()) {
            transitionTo(BookingStatus.CANCELLED);
        }
    }

    private void transitionTo(BookingStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStateException("Booking cannot move from " + status + " to " + next);
        }
        status = next;
    }
}
