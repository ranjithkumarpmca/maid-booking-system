package com.maidbooking.support;

import com.maidbooking.application.availability.AvailabilityService;
import com.maidbooking.application.booking.BookingCommand;
import com.maidbooking.application.booking.BookingService;
import com.maidbooking.application.booking.InstantBookingStrategy;
import com.maidbooking.application.booking.RecurrenceRule;
import com.maidbooking.application.booking.RecurringBookingStrategy;
import com.maidbooking.application.booking.ScheduledBookingStrategy;
import com.maidbooking.application.cancellation.CancellationService;
import com.maidbooking.application.maid.MaidService;
import com.maidbooking.application.maid.OnboardMaidCommand;
import com.maidbooking.application.payment.PaymentCommand;
import com.maidbooking.application.payment.PaymentResult;
import com.maidbooking.application.payment.PaymentService;
import com.maidbooking.application.payment.method.CardPaymentMethod;
import com.maidbooking.application.payment.method.UpiPaymentMethod;
import com.maidbooking.application.payment.method.WalletPaymentMethod;
import com.maidbooking.application.search.MaidSearchService;
import com.maidbooking.application.search.filter.GenderFilter;
import com.maidbooking.application.search.filter.LocalityFilter;
import com.maidbooking.application.search.filter.MaxHourlyRateFilter;
import com.maidbooking.application.search.filter.MinRatingFilter;
import com.maidbooking.application.search.filter.ServiceTypeFilter;
import com.maidbooking.application.search.filter.SkillsFilter;
import com.maidbooking.application.support.KeyedLocks;
import com.maidbooking.domain.model.AvailabilityWindow;
import com.maidbooking.domain.model.Booking;
import com.maidbooking.domain.model.BookingType;
import com.maidbooking.domain.model.Gender;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.PaymentMethodType;
import com.maidbooking.domain.model.ServiceType;
import com.maidbooking.domain.pricing.HourlyRatePricing;
import com.maidbooking.domain.refund.TimeBasedRefundPolicy;
import com.maidbooking.domain.repository.BookingRepository;
import com.maidbooking.domain.repository.MaidRepository;
import com.maidbooking.domain.repository.PaymentRepository;
import com.maidbooking.domain.repository.SlotInventory;
import com.maidbooking.infrastructure.persistence.InMemoryBookingRepository;
import com.maidbooking.infrastructure.persistence.InMemoryMaidRepository;
import com.maidbooking.infrastructure.persistence.InMemoryPaymentRepository;
import com.maidbooking.infrastructure.persistence.InMemorySlotInventory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wires the real services over in-memory repositories with a controllable clock and a fake gateway,
 * so each test reads as a business scenario. "Now" is Monday 2026-11-30 08:00; {@link #at} counts days from it.
 */
public final class TestEnvironment {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 30, 8, 0);

    public final MutableClock clock = new MutableClock(NOW.atZone(ZONE).toInstant(), ZONE);
    public final MaidRepository maids = new InMemoryMaidRepository();
    public final BookingRepository bookings = new InMemoryBookingRepository();
    public final PaymentRepository payments = new InMemoryPaymentRepository();
    public final SlotInventory inventory = new InMemorySlotInventory();
    public final FakePaymentGateway gateway = new FakePaymentGateway();
    public final KeyedLocks locks = new KeyedLocks();

    public final MaidService maidService = new MaidService(maids);
    public final AvailabilityService availability = new AvailabilityService(inventory);
    public final BookingService bookingService = new BookingService(maids, bookings, inventory, availability,
            List.of(new InstantBookingStrategy(), new ScheduledBookingStrategy(), new RecurringBookingStrategy()),
            new HourlyRatePricing(), clock);
    public final PaymentService paymentService = new PaymentService(bookings, payments, gateway,
            List.of(new CardPaymentMethod(), new UpiPaymentMethod(), new WalletPaymentMethod()), locks, clock);
    public final CancellationService cancellationService = new CancellationService(bookings, payments, gateway,
            inventory, TimeBasedRefundPolicy.standard(), locks, clock);
    public final MaidSearchService searchService = new MaidSearchService(maids, availability,
            List.of(new LocalityFilter(), new ServiceTypeFilter(), new SkillsFilter(), new MinRatingFilter(),
                    new MaxHourlyRateFilter(), new GenderFilter()));

    /** Date and time {@code dayOffset} days after today (offset 0 = Monday 2026-11-30). */
    public static LocalDateTime at(int dayOffset, int hour) {
        return NOW.toLocalDate().plusDays(dayOffset).atTime(hour, 0);
    }

    public static LocalDate dateOf(int dayOffset) {
        return NOW.toLocalDate().plusDays(dayOffset);
    }

    /** A maid working every day 08:00-18:00. */
    public Maid addMaid(String name, Gender gender, String locality, Set<ServiceType> services, long hourlyRate,
                        double rating) {
        List<AvailabilityWindow> windows = Arrays.stream(DayOfWeek.values())
                .map(day -> new AvailabilityWindow(day, LocalTime.of(8, 0), LocalTime.of(18, 0)))
                .toList();
        return maidService.onboard(new OnboardMaidCommand(
                name, gender, locality, services, Money.of(hourlyRate), rating, windows));
    }

    /** Cleaning and cooking at INR 200/hour, rated 4.5. */
    public Maid addDefaultMaid() {
        return addMaid("Lakshmi", Gender.FEMALE, "Koramangala", Set.of(ServiceType.CLEANING, ServiceType.COOKING),
                200, 4.5);
    }

    public BookingCommand scheduled(String customerId, Maid maid, LocalDateTime start, int minutes) {
        return new BookingCommand(customerId, maid.id(), ServiceType.CLEANING, BookingType.SCHEDULED,
                start, minutes, null);
    }

    public Booking bookScheduled(Maid maid, LocalDateTime start, int minutes) {
        return bookingService.book(scheduled("customer-1", maid, start, minutes));
    }

    /** Every listed weekday at 09:00 between the two day offsets (inclusive), 60 minutes each. */
    public Booking bookRecurring(Maid maid, Set<DayOfWeek> days, int fromDayOffset, int toDayOffset) {
        RecurrenceRule rule = new RecurrenceRule(days, LocalTime.of(9, 0), dateOf(fromDayOffset), dateOf(toDayOffset));
        return bookingService.book(new BookingCommand("customer-1", maid.id(), ServiceType.CLEANING,
                BookingType.RECURRING, null, 60, rule));
    }

    public PaymentResult payByUpi(Booking booking, String idempotencyKey) {
        return paymentService.pay(new PaymentCommand(booking.id(), PaymentMethodType.UPI,
                Map.of("vpa", "ranjith@okbank"), idempotencyKey, null));
    }
}
