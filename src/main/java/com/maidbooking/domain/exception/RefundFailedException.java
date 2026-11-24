package com.maidbooking.domain.exception;

/** The payment gateway rejected or failed a refund. */
public class RefundFailedException extends DomainException {

    public RefundFailedException(String message) {
        super(message);
    }
}
