package com.maidbooking.domain.exception;

/** The maid is outside working hours or already booked for an overlapping slot. */
public class SlotUnavailableException extends DomainException {

    public SlotUnavailableException(String message) {
        super(message);
    }
}
