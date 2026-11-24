package com.maidbooking.domain.exception;

/** An idempotency key was reused for a different request. */
public class IdempotencyConflictException extends DomainException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
