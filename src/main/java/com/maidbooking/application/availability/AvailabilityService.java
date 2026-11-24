package com.maidbooking.application.availability;

import com.maidbooking.domain.exception.SlotUnavailableException;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.TimeSlot;
import com.maidbooking.domain.repository.SlotInventory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Free-versus-busy reasoning. "Free" means: inside the maid's weekly working windows AND not reserved.
 */
@Service
public class AvailabilityService {

    private final SlotInventory inventory;

    public AvailabilityService(SlotInventory inventory) {
        this.inventory = inventory;
    }

    public boolean isAvailable(Maid maid, TimeSlot slot) {
        return maid.isWorkingDuring(slot) && inventory.isFree(maid.id(), slot);
    }

    public void requireWorkingHours(Maid maid, TimeSlot slot) {
        if (!maid.isWorkingDuring(slot)) {
            throw new SlotUnavailableException(
                    "Maid " + maid.name() + " does not work during " + slot.start() + " - " + slot.end());
        }
    }

    /** The maid's free intervals on a date: her working windows minus everything already reserved. */
    public List<TimeSlot> freeSlots(Maid maid, LocalDate date) {
        List<TimeSlot> reserved = inventory.reservedSlots(maid.id());
        List<TimeSlot> free = new ArrayList<>();
        for (TimeSlot window : maid.workingSlotsOn(date)) {
            LocalDateTime cursor = window.start();
            for (TimeSlot busy : reserved) {
                if (!busy.overlaps(window)) {
                    continue;
                }
                if (busy.start().isAfter(cursor)) {
                    free.add(new TimeSlot(cursor, busy.start()));
                }
                if (busy.end().isAfter(cursor)) {
                    cursor = busy.end();
                }
            }
            if (cursor.isBefore(window.end())) {
                free.add(new TimeSlot(cursor, window.end()));
            }
        }
        return free;
    }
}
