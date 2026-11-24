package com.maidbooking.domain;

import com.maidbooking.domain.model.Gender;
import com.maidbooking.domain.model.AvailabilityWindow;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.ServiceType;
import com.maidbooking.domain.model.TimeSlot;
import com.maidbooking.domain.pricing.HourlyRatePricing;
import com.maidbooking.domain.pricing.PricingStrategy;
import com.maidbooking.domain.pricing.SurchargePricing;
import com.maidbooking.domain.refund.TimeBasedRefundPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolicyTest {

    // Monday 2026-11-30 and Saturday 2026-12-05
    private static final LocalDateTime MONDAY_10 = LocalDateTime.of(2026, 11, 30, 10, 0);
    private static final LocalDateTime SATURDAY_10 = LocalDateTime.of(2026, 12, 5, 10, 0);

    private final Maid maid = new Maid("m1", "Asha", Gender.FEMALE, "Koramangala", Set.of(ServiceType.CLEANING),
            Money.of(200), 4.5, List.of(new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0))));

    @Test
    @DisplayName("base price is hourly rate x hours, including fractions of an hour")
    void hourlyPricing() {
        PricingStrategy pricing = new HourlyRatePricing();
        assertEquals(Money.of(400), pricing.priceFor(maid, ServiceType.CLEANING, TimeSlot.of(MONDAY_10, 120)));
        assertEquals(Money.of(300), pricing.priceFor(maid, ServiceType.CLEANING, TimeSlot.of(MONDAY_10, 90)));
    }

    @Test
    @DisplayName("weekend surcharge applies on weekends only, and decorators stack")
    void surchargePricing() {
        PricingStrategy weekend = SurchargePricing.weekend(new HourlyRatePricing(), new BigDecimal("1.5"));
        assertEquals(Money.of(200), weekend.priceFor(maid, ServiceType.CLEANING, TimeSlot.of(MONDAY_10, 60)));
        assertEquals(Money.of(300), weekend.priceFor(maid, ServiceType.CLEANING, TimeSlot.of(SATURDAY_10, 60)));

        PricingStrategy stacked = SurchargePricing.peakHours(weekend, LocalTime.of(9, 0), LocalTime.of(11, 0),
                new BigDecimal("2"));
        assertEquals(Money.of(400), stacked.priceFor(maid, ServiceType.CLEANING, TimeSlot.of(MONDAY_10, 60)));
        assertEquals(Money.of(600), stacked.priceFor(maid, ServiceType.CLEANING, TimeSlot.of(SATURDAY_10, 60)));
    }

    @Test
    @DisplayName("standard refund tiers: 24h+ full, 2h+ half, less than 2h nothing")
    void standardRefundTiers() {
        TimeBasedRefundPolicy policy = TimeBasedRefundPolicy.standard();
        Money paid = Money.of(400);
        TimeSlot slot = TimeSlot.of(MONDAY_10, 60);

        assertEquals(Money.of(400), policy.refundFor(paid, slot, MONDAY_10.minusDays(3)));
        assertEquals(Money.of(400), policy.refundFor(paid, slot, MONDAY_10.minusHours(24)), "exactly 24h counts as full");
        assertEquals(Money.of(200), policy.refundFor(paid, slot, MONDAY_10.minusHours(23)));
        assertEquals(Money.of(200), policy.refundFor(paid, slot, MONDAY_10.minusHours(2)));
        assertEquals(Money.ZERO, policy.refundFor(paid, slot, MONDAY_10.minusMinutes(119)));
        assertEquals(Money.ZERO, policy.refundFor(paid, slot, MONDAY_10.minusMinutes(5)));
    }

    @Test
    @DisplayName("nothing paid means nothing to refund")
    void nothingPaid() {
        assertEquals(Money.ZERO,
                TimeBasedRefundPolicy.standard().refundFor(Money.ZERO, TimeSlot.of(MONDAY_10, 60), MONDAY_10.minusDays(5)));
    }

    @Test
    @DisplayName("tiers are order-independent and custom policies are possible")
    void customTiers() {
        TimeBasedRefundPolicy policy = new TimeBasedRefundPolicy(List.of(
                new TimeBasedRefundPolicy.Tier(Duration.ofHours(1), 25),
                new TimeBasedRefundPolicy.Tier(Duration.ofHours(48), 90)));
        Money paid = Money.of(1000);
        TimeSlot slot = TimeSlot.of(MONDAY_10, 60);

        assertEquals(Money.of(900), policy.refundFor(paid, slot, MONDAY_10.minusDays(2)));
        assertEquals(Money.of(250), policy.refundFor(paid, slot, MONDAY_10.minusHours(5)));
    }
}
