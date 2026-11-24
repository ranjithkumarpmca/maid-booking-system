package com.maidbooking.domain.exception;

/** The request is well-formed but violates a business validation rule. */
public class InvalidRequestException extends DomainException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
