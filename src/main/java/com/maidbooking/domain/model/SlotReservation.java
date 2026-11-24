package com.maidbooking.domain.model;

/** A claim on a maid's time, identified by the id of whatever holds it (a booking occurrence). */
public record SlotReservation(String id, TimeSlot slot) {
}
