package com.maidbooking.application;

import com.maidbooking.application.booking.BookingCommand;
import com.maidbooking.domain.exception.InvalidRequestException;
import com.maidbooking.domain.exception.ResourceNotFoundException;
import com.maidbooking.domain.exception.SlotUnavailableException;
import com.maidbooking.domain.model.Booking;
import com.maidbooking.domain.model.BookingType;
import com.maidbooking.domain.model.Gender;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.ServiceType;
import com.maidbooking.support.TestEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static com.maidbooking.support.TestEnvironment.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingServiceTest {

    private TestEnvironment env;
    private Maid maid;

    @BeforeEach
    void setUp() {
        env = new TestEnvironment();
        maid = env.addDefaultMaid(); // INR 200/h, works every day 08:00-18:00
    }

    @Test
    @DisplayName("scheduled booking is priced from the slot length and reserves the maid")
    void scheduledBooking() {
        Booking booking = env.bookScheduled(maid, at(1, 10), 90);

        assertEquals(1, booking.occurrences().size());
        assertEquals(Money.of(300), booking.totalAmount());
        assertTrue(!env.inventory.isFree(maid.id(), booking.occurrences().get(0).slot()));
    }

    @Test
    @DisplayName("instant booking starts now and needs no start time")
    void instantBooking() {
        Booking booking = env.bookingService.book(new BookingCommand("c1", maid.id(), ServiceType.CLEANING,
                BookingType.INSTANT, null, 60, null));

        assertEquals(TestEnvironment.NOW, booking.occurrences().get(0).slot().start());
        assertEquals(BookingType.INSTANT, booking.bookingType());
    }

    @Test
    @DisplayName("overlapping slots for the same maid are rejected, adjacent ones are fine")
    void preventsDoubleBooking() {
        env.bookScheduled(maid, at(1, 10), 60); // 10:00-11:00

        assertThrows(SlotUnavailableException.class, () -> env.bookScheduled(maid, at(1, 10), 60));
        assertThrows(SlotUnavailableException.class,
                () -> env.bookScheduled(maid, at(1, 10).plusMinutes(30), 60));

        env.bookScheduled(maid, at(1, 11), 60); // starts exactly when the first one ends
        env.bookScheduled(maid, at(1, 9), 60);  // ends exactly when the first one starts
    }

    @Test
    @DisplayName("a different maid can be booked for the same time")
    void otherMaidIsIndependent() {
        Maid other = env.addMaid("Meena", Gender.FEMALE, "Koramangala", Set.of(ServiceType.CLEANING), 250, 4.0);
        env.bookScheduled(maid, at(1, 10), 60);
        env.bookScheduled(other, at(1, 10), 60);
    }

    @Test
    @DisplayName("bookings outside the maid's working hours are rejected")
    void outsideWorkingHours() {
        assertThrows(SlotUnavailableException.class, () -> env.bookScheduled(maid, at(1, 6), 60));
        assertThrows(SlotUnavailableException.class, () -> env.bookScheduled(maid, at(1, 17), 120));
    }

    @Test
    @DisplayName("invalid requests are rejected before anything is reserved")
    void validation() {
        assertThrows(InvalidRequestException.class, () -> env.bookScheduled(maid, at(-1, 10), 60), "in the past");
        assertThrows(InvalidRequestException.class, () -> env.bookScheduled(maid, at(1, 10), 10), "too short");
        assertThrows(InvalidRequestException.class, () -> env.bookScheduled(maid, at(1, 10), 600), "too long");
        assertThrows(InvalidRequestException.class, () -> env.bookingService.book(new BookingCommand(
                "c1", maid.id(), ServiceType.CLEANING, BookingType.SCHEDULED, null, 60, null)), "no start time");
        assertThrows(InvalidRequestException.class, () -> env.bookingService.book(new BookingCommand(
                "c1", maid.id(), ServiceType.LAUNDRY, BookingType.SCHEDULED, at(1, 10), 60, null)),
                "maid does not offer laundry");
        assertThrows(ResourceNotFoundException.class, () -> env.bookingService.book(new BookingCommand(
                "c1", "nope", ServiceType.CLEANING, BookingType.SCHEDULED, at(1, 10), 60, null)));
        assertTrue(env.inventory.reservedSlots(maid.id()).isEmpty());
    }

    @Test
    @DisplayName("recurring booking expands to one occurrence per matching day")
    void recurringExpansion() {
        // Mon 30 Nov .. Sun 6 Dec (day offsets 0..6), weekdays at 09:00; "now" is Monday 08:00 so Monday counts
        Booking booking = env.bookRecurring(maid,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                0, 6);

        assertEquals(5, booking.occurrences().size());
        assertEquals(Money.of(1000), booking.totalAmount());
        assertEquals(LocalTime.of(9, 0), booking.occurrences().get(0).slot().start().toLocalTime());
    }

    @Test
    @DisplayName("recurring booking reserves all occurrences or none")
    void recurringIsAllOrNothing() {
        env.bookScheduled(maid, at(3, 9), 60); // Thursday 09:00 is taken

        assertThrows(SlotUnavailableException.class,
                () -> env.bookRecurring(maid, Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), 0, 6));

        assertEquals(1, env.inventory.reservedSlots(maid.id()).size(), "only the original booking remains reserved");
    }

    @Test
    @DisplayName("recurrence rules are validated")
    void recurrenceValidation() {
        assertThrows(InvalidRequestException.class, () -> env.bookingService.book(new BookingCommand(
                "c1", maid.id(), ServiceType.CLEANING, BookingType.RECURRING, null, 60, null)));
        assertThrows(InvalidRequestException.class,
                () -> env.bookRecurring(maid, Set.of(), 0, 6));
        assertThrows(InvalidRequestException.class,
                () -> env.bookRecurring(maid, Set.of(DayOfWeek.MONDAY), 5, 2), "end before start");
        assertThrows(InvalidRequestException.class,
                () -> env.bookRecurring(maid, Set.of(DayOfWeek.values()), 0, 200), "too many occurrences");
    }

    @Test
    @DisplayName("concurrent requests for the same slot: exactly one wins")
    void concurrentBookingsSameSlot() throws Exception {
        int threads = 24;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String customer = "customer-" + i;
            futures.add(pool.submit(() -> {
                startGun.await();
                try {
                    env.bookingService.book(env.scheduled(customer, maid, at(2, 10), 60));
                    succeeded.incrementAndGet();
                } catch (SlotUnavailableException e) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }
        startGun.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        pool.shutdown();

        assertEquals(1, succeeded.get());
        assertEquals(threads - 1, rejected.get());
        assertEquals(1, env.inventory.reservedSlots(maid.id()).size());
    }

    @Test
    @DisplayName("concurrent overlapping requests never leave overlapping reservations")
    void concurrentOverlappingSlots() throws Exception {
        int threads = 30;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int offsetMinutes = (i % 6) * 20; // many different, mostly overlapping start times
            futures.add(pool.submit(() -> {
                startGun.await();
                try {
                    env.bookingService.book(env.scheduled("c", maid, at(2, 9).plusMinutes(offsetMinutes), 60));
                } catch (SlotUnavailableException ignored) {
                    // expected for the losers
                }
                return null;
            }));
        }
        startGun.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        pool.shutdown();

        var reserved = env.inventory.reservedSlots(maid.id());
        for (int i = 0; i < reserved.size(); i++) {
            for (int j = i + 1; j < reserved.size(); j++) {
                assertTrue(!reserved.get(i).overlaps(reserved.get(j)), "double booking: " + reserved);
            }
        }
    }
}
