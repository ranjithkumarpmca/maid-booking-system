package com.maidbooking.domain.model;

/**
 * Supported booking kinds. Each constant is backed by one BookingStrategy implementation;
 * supporting a new kind means adding a constant and a strategy, nothing else.
 */
public enum BookingType {
    INSTANT,
    SCHEDULED,
    RECURRING
}
