package com.maidbooking.domain.repository;

import com.maidbooking.domain.model.SlotReservation;
import com.maidbooking.domain.model.TimeSlot;

import java.util.List;

/**
 * The shared, contended inventory: which parts of a maid's time are already taken.
 * <p>
 * The check-and-reserve step is a single atomic operation ({@link #tryReserveAll}); that is the only place
 * double-booking is prevented, so every implementation (in-memory lock, SQL unique/exclusion constraint,
 * Redis script...) must preserve that atomicity.
 */
public interface SlotInventory {

    boolean isFree(String maidId, TimeSlot slot);

    /**
     * Atomically reserves all given slots for the maid, or none of them.
     *
     * @return false if any slot overlaps an existing reservation (or another slot in the same request)
     */
    boolean tryReserveAll(String maidId, List<SlotReservation> reservations);

    /** Frees a reservation so the time becomes discoverable and bookable again. Unknown ids are ignored. */
    void release(String maidId, String reservationId);

    /** All reserved slots of the maid, ordered by start time. */
    List<TimeSlot> reservedSlots(String maidId);
}
