package com.maidbooking.domain.model;

/**
 * Booking lifecycle:
 * <pre>
 * PENDING_PAYMENT --pay--> CONFIRMED --cancel(all occurrences)--> CANCELLED
 *        \------------cancel(all occurrences)------------------------^
 * </pre>
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED;

    public boolean canTransitionTo(BookingStatus next) {
        return switch (this) {
            case PENDING_PAYMENT -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == CANCELLED;
            case CANCELLED -> false;
        };
    }
}
