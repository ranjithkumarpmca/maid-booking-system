package com.maidbooking.infrastructure.persistence;

import com.maidbooking.domain.model.SlotReservation;
import com.maidbooking.domain.model.TimeSlot;
import com.maidbooking.domain.repository.SlotInventory;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory inventory with one lock per maid: bookings for different maids never contend, while two requests
 * for the same maid are strictly serialised, so check-then-reserve cannot interleave.
 * A SQL implementation would get the same guarantee from an exclusion constraint or SELECT ... FOR UPDATE.
 */
@Repository
public class InMemorySlotInventory implements SlotInventory {

    private final Map<String, MaidCalendar> calendars = new ConcurrentHashMap<>();

    @Override
    public boolean isFree(String maidId, TimeSlot slot) {
        return calendar(maidId).isFree(slot);
    }

    @Override
    public boolean tryReserveAll(String maidId, List<SlotReservation> reservations) {
        return calendar(maidId).tryReserveAll(reservations);
    }

    @Override
    public void release(String maidId, String reservationId) {
        calendar(maidId).release(reservationId);
    }

    @Override
    public List<TimeSlot> reservedSlots(String maidId) {
        return calendar(maidId).slots();
    }

    private MaidCalendar calendar(String maidId) {
        return calendars.computeIfAbsent(maidId, id -> new MaidCalendar());
    }

    /** All state of one maid, guarded by the calendar's own monitor. */
    private static final class MaidCalendar {

        private final Map<String, TimeSlot> reserved = new HashMap<>();

        synchronized boolean isFree(TimeSlot slot) {
            return reserved.values().stream().noneMatch(slot::overlaps);
        }

        synchronized boolean tryReserveAll(List<SlotReservation> reservations) {
            Map<String, TimeSlot> staged = new HashMap<>(reserved);
            for (SlotReservation reservation : reservations) {
                boolean clashes = staged.values().stream().anyMatch(reservation.slot()::overlaps);
                if (clashes || staged.putIfAbsent(reservation.id(), reservation.slot()) != null) {
                    return false;
                }
            }
            reserved.putAll(staged);
            return true;
        }

        synchronized void release(String reservationId) {
            reserved.remove(reservationId);
        }

        synchronized List<TimeSlot> slots() {
            return reserved.values().stream().sorted(Comparator.comparing(TimeSlot::start)).toList();
        }
    }
}
