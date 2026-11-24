package com.maidbooking.domain.model;

import com.maidbooking.domain.exception.InvalidStateException;

/**
 * One concrete slot of a booking. An instant or scheduled booking has exactly one; a recurring booking has many.
 * It is the unit of reservation, pricing, payment and cancellation, which is what makes
 * "cancel a single occurrence" and "pay per occurrence" natural.
 * State changes are package-private: only {@link Booking} (the aggregate root) may drive them.
 */
public final class BookingOccurrence {

    private final String id;
    private final TimeSlot slot;
    private final Money price;
    private volatile OccurrenceStatus status = OccurrenceStatus.SCHEDULED;
    private volatile Money paidAmount = Money.ZERO;
    private volatile String paymentId;
    private volatile Money refundedAmount = Money.ZERO;

    public BookingOccurrence(String id, TimeSlot slot, Money price) {
        this.id = id;
        this.slot = slot;
        this.price = price;
    }

    public String id() {
        return id;
    }

    public TimeSlot slot() {
        return slot;
    }

    public Money price() {
        return price;
    }

    public OccurrenceStatus status() {
        return status;
    }

    public Money paidAmount() {
        return paidAmount;
    }

    public String paymentId() {
        return paymentId;
    }

    public Money refundedAmount() {
        return refundedAmount;
    }

    public boolean isActive() {
        return status == OccurrenceStatus.SCHEDULED;
    }

    public boolean isPaid() {
        return paymentId != null;
    }

    void markPaid(String paymentId) {
        if (!isActive()) {
            throw new InvalidStateException("Occurrence " + id + " is cancelled and cannot be paid");
        }
        if (isPaid()) {
            throw new InvalidStateException("Occurrence " + id + " is already paid");
        }
        this.paidAmount = price;
        this.paymentId = paymentId;
    }

    void cancel(Money refund) {
        if (!isActive()) {
            throw new InvalidStateException("Occurrence " + id + " is already cancelled");
        }
        this.status = OccurrenceStatus.CANCELLED;
        this.refundedAmount = refund;
    }
}
