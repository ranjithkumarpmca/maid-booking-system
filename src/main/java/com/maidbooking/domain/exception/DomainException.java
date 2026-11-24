package com.maidbooking.domain.exception;

/** Base type for all business-rule violations. The API layer maps subtypes to HTTP statuses. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
