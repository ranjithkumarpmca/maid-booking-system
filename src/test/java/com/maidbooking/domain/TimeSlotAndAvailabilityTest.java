package com.maidbooking.domain;

import com.maidbooking.domain.model.AvailabilityWindow;
import com.maidbooking.domain.model.Gender;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.ServiceType;
import com.maidbooking.domain.model.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeSlotAndAvailabilityTest {

    private static final LocalDateTime MONDAY_9 = LocalDateTime.of(2026, 11, 30, 9, 0);

    private static TimeSlot slot(int fromHour, int toHour) {
        return new TimeSlot(MONDAY_9.withHour(fromHour), MONDAY_9.withHour(toHour));
    }

    @Test
    @DisplayName("slots that overlap, even partially or by containment, are detected")
    void overlappingSlots() {
        assertTrue(slot(9, 11).overlaps(slot(10, 12)));
        assertTrue(slot(9, 12).overlaps(slot(10, 11)));
        assertTrue(slot(10, 11).overlaps(slot(9, 12)));
        assertTrue(slot(9, 11).overlaps(slot(9, 11)));
    }

    @Test
    @DisplayName("back-to-back slots do not overlap (intervals are half-open)")
    void touchingSlotsDoNotOverlap() {
        assertFalse(slot(9, 10).overlaps(slot(10, 11)));
        assertFalse(slot(10, 11).overlaps(slot(9, 10)));
    }

    @Test
    @DisplayName("a slot must start before it ends")
    void invalidSlot() {
        assertThrows(IllegalArgumentException.class, () -> slot(10, 10));
        assertThrows(IllegalArgumentException.class, () -> slot(11, 10));
        assertThrows(IllegalArgumentException.class, () -> TimeSlot.of(MONDAY_9, 0));
    }

    @Test
    @DisplayName("a window covers slots inside it on its weekday only")
    void windowCoverage() {
        AvailabilityWindow monday = new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        assertTrue(monday.covers(slot(8, 12)));
        assertTrue(monday.covers(slot(9, 10)));
        assertFalse(monday.covers(slot(7, 9)), "starts before the window");
        assertFalse(monday.covers(slot(11, 13)), "ends after the window");
        assertFalse(monday.covers(new TimeSlot(MONDAY_9.plusDays(1), MONDAY_9.plusDays(1).plusHours(1))),
                "Tuesday is not covered by a Monday window");
    }

    @Test
    @DisplayName("a slot spanning midnight is never covered")
    void slotAcrossMidnight() {
        AvailabilityWindow allDay = new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(0, 0), LocalTime.of(23, 59));
        assertFalse(allDay.covers(new TimeSlot(MONDAY_9.withHour(23), MONDAY_9.plusDays(1).withHour(1))));
    }

    @Test
    @DisplayName("onboarding rejects overlapping windows, empty services and bad ratings")
    void maidValidation() {
        AvailabilityWindow morning = new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        AvailabilityWindow clashing = new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(14, 0));

        assertThrows(IllegalArgumentException.class, () -> maid(Set.of(ServiceType.CLEANING), 4.0, List.of(morning, clashing)));
        assertThrows(IllegalArgumentException.class, () -> maid(Set.of(), 4.0, List.of(morning)));
        assertThrows(IllegalArgumentException.class, () -> maid(Set.of(ServiceType.CLEANING), 5.5, List.of(morning)));
        assertThrows(IllegalArgumentException.class, () -> maid(Set.of(ServiceType.CLEANING), 4.0, List.of()));
    }

    @Test
    @DisplayName("money cannot be negative and keeps two decimals")
    void money() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(-1));
        assertEquals(Money.of("100.00"), Money.of(100));
        assertEquals(Money.of("50.00"), Money.of(100).percent(50));
        assertThrows(IllegalArgumentException.class, () -> Money.of(10).minus(Money.of(20)));
    }

    private static Maid maid(Set<ServiceType> services, double rating, List<AvailabilityWindow> windows) {
        return new Maid("m1", "Asha", Gender.FEMALE, "Koramangala", services, Money.of(200), rating, windows);
    }
}
