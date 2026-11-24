package com.maidbooking.domain.exception;

/** An operation is not allowed in the current state (e.g. cancelling an already cancelled booking). */
public class InvalidStateException extends DomainException {

    public InvalidStateException(String message) {
        super(message);
    }
}
