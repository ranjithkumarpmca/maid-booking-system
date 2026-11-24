package com.maidbooking.application.booking;

import com.maidbooking.application.availability.AvailabilityService;
import com.maidbooking.application.support.TypeRegistry;
import com.maidbooking.domain.exception.InvalidRequestException;
import com.maidbooking.domain.exception.ResourceNotFoundException;
import com.maidbooking.domain.exception.SlotUnavailableException;
import com.maidbooking.domain.model.Booking;
import com.maidbooking.domain.model.BookingOccurrence;
import com.maidbooking.domain.model.BookingType;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.SlotReservation;
import com.maidbooking.domain.model.TimeSlot;
import com.maidbooking.domain.pricing.PricingStrategy;
import com.maidbooking.domain.repository.BookingRepository;
import com.maidbooking.domain.repository.MaidRepository;
import com.maidbooking.domain.repository.SlotInventory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Booking orchestration, identical for every booking type:
 * resolve slots (type-specific) -> check working hours -> price -> atomically reserve -> persist.
 * Double-booking is prevented by the atomic reservation, not by the earlier checks, which only give friendlier errors.
 */
@Service
public class BookingService {

    static final int MIN_DURATION_MINUTES = 30;
    static final int MAX_DURATION_MINUTES = 480;

    private final MaidRepository maids;
    private final BookingRepository bookings;
    private final SlotInventory inventory;
    private final AvailabilityService availability;
    private final TypeRegistry<BookingType, BookingStrategy> strategies;
    private final PricingStrategy pricing;
    private final Clock clock;

    public BookingService(MaidRepository maids, BookingRepository bookings, SlotInventory inventory,
                          AvailabilityService availability, List<BookingStrategy> strategies,
                          PricingStrategy pricing, Clock clock) {
        this.maids = maids;
        this.bookings = bookings;
        this.inventory = inventory;
        this.availability = availability;
        this.strategies = new TypeRegistry<>("booking type", strategies, BookingStrategy::type);
        this.pricing = pricing;
        this.clock = clock;
    }

    public Booking book(BookingCommand command) {
        validate(command);
        Maid maid = maids.findById(command.maidId())
                .orElseThrow(() -> new ResourceNotFoundException("Maid", command.maidId()));
        if (!maid.offers(command.serviceType())) {
            throw new InvalidRequestException(
                    "Maid " + maid.name() + " does not offer " + command.serviceType());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<TimeSlot> slots = strategies.get(command.type()).resolveSlots(command, now);
        slots.forEach(slot -> availability.requireWorkingHours(maid, slot));

        List<BookingOccurrence> occurrences = slots.stream()
                .map(slot -> new BookingOccurrence(newId(), slot, pricing.priceFor(maid, command.serviceType(), slot)))
                .toList();
        List<SlotReservation> reservations = occurrences.stream()
                .map(occurrence -> new SlotReservation(occurrence.id(), occurrence.slot()))
                .toList();

        if (!inventory.tryReserveAll(maid.id(), reservations)) {
            throw new SlotUnavailableException(
                    "Maid " + maid.name() + " is already booked for an overlapping slot");
        }

        Booking booking = new Booking(newId(), command.customerId(), maid.id(), command.serviceType(),
                command.type(), occurrences, now);
        try {
            return bookings.save(booking);
        } catch (RuntimeException e) {
            reservations.forEach(reservation -> inventory.release(maid.id(), reservation.id()));
            throw e;
        }
    }

    public Booking get(String bookingId) {
        return bookings.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }

    private static void validate(BookingCommand command) {
        if (command.customerId() == null || command.customerId().isBlank()) {
            throw new InvalidRequestException("customerId is required");
        }
        if (command.maidId() == null || command.maidId().isBlank()) {
            throw new InvalidRequestException("maidId is required");
        }
        if (command.serviceType() == null) {
            throw new InvalidRequestException("serviceType is required");
        }
        if (command.type() == null) {
            throw new InvalidRequestException("type is required");
        }
        if (command.durationMinutes() < MIN_DURATION_MINUTES || command.durationMinutes() > MAX_DURATION_MINUTES) {
            throw new InvalidRequestException("durationMinutes must be between " + MIN_DURATION_MINUTES
                    + " and " + MAX_DURATION_MINUTES);
        }
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
