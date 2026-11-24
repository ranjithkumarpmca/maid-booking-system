package com.maidbooking.domain.model;

import com.maidbooking.domain.exception.InvalidStateException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of one charge attempt against a booking (covering one or more occurrences).
 * Failed attempts are stored too, so a retry with the same idempotency key replays the same outcome.
 */
public final class Payment {

    private final String id;
    private final String bookingId;
    private final Money amount;
    private final PaymentMethodType method;
    private final String idempotencyKey;
    private final List<String> occurrenceIds;
    private final String gatewayReference;
    private final String failureReason;
    private final LocalDateTime createdAt;
    private volatile PaymentStatus status;
    private volatile Money refundedAmount = Money.ZERO;

    private Payment(String id, String bookingId, Money amount, PaymentMethodType method, String idempotencyKey,
                    List<String> occurrenceIds, PaymentStatus status, String gatewayReference,
                    String failureReason, LocalDateTime createdAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.amount = amount;
        this.method = method;
        this.idempotencyKey = idempotencyKey;
        this.occurrenceIds = List.copyOf(occurrenceIds);
        this.status = status;
        this.gatewayReference = gatewayReference;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    public static Payment succeeded(String id, String bookingId, Money amount, PaymentMethodType method,
                                    String idempotencyKey, List<String> occurrenceIds, String gatewayReference,
                                    LocalDateTime createdAt) {
        return new Payment(id, bookingId, amount, method, idempotencyKey, occurrenceIds,
                PaymentStatus.SUCCEEDED, gatewayReference, null, createdAt);
    }

    public static Payment failed(String id, String bookingId, Money amount, PaymentMethodType method,
                                 String idempotencyKey, List<String> occurrenceIds, String failureReason,
                                 LocalDateTime createdAt) {
        return new Payment(id, bookingId, amount, method, idempotencyKey, occurrenceIds,
                PaymentStatus.FAILED, null, failureReason, createdAt);
    }

    public String id() {
        return id;
    }

    public String bookingId() {
        return bookingId;
    }

    public Money amount() {
        return amount;
    }

    public PaymentMethodType method() {
        return method;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public List<String> occurrenceIds() {
        return occurrenceIds;
    }

    public String gatewayReference() {
        return gatewayReference;
    }

    public String failureReason() {
        return failureReason;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public PaymentStatus status() {
        return status;
    }

    public Money refundedAmount() {
        return refundedAmount;
    }

    public boolean isSuccessful() {
        return status != PaymentStatus.FAILED;
    }

    /** Records money returned to the customer; the total refunded can never exceed what was charged. */
    public void recordRefund(Money refund) {
        if (status == PaymentStatus.FAILED) {
            throw new InvalidStateException("A failed payment cannot be refunded");
        }
        Money totalRefunded = refundedAmount.plus(refund);
        if (totalRefunded.isGreaterThan(amount)) {
            throw new InvalidStateException("Refund would exceed the amount paid for payment " + id);
        }
        refundedAmount = totalRefunded;
        status = totalRefunded.compareTo(amount) == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
    }
}
