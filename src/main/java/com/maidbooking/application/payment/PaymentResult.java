package com.maidbooking.application.payment;

import com.maidbooking.domain.model.Payment;

/** @param replayed true when this is the stored outcome of an earlier request with the same idempotency key */
public record PaymentResult(Payment payment, boolean replayed) {
}
