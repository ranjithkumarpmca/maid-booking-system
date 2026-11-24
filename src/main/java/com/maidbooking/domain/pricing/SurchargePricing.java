package com.maidbooking.domain.pricing;

import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.ServiceType;
import com.maidbooking.domain.model.TimeSlot;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.function.Predicate;

/**
 * Dynamic-pricing decorator: multiplies the delegate's price when a condition on the slot holds.
 * Decorators compose, e.g. {@code weekend(peakHours(base, ...), ...)}.
 */
public class SurchargePricing implements PricingStrategy {

    private final PricingStrategy delegate;
    private final Predicate<TimeSlot> applies;
    private final BigDecimal multiplier;

    public SurchargePricing(PricingStrategy delegate, Predicate<TimeSlot> applies, BigDecimal multiplier) {
        if (multiplier.signum() <= 0) {
            throw new IllegalArgumentException("multiplier must be positive");
        }
        this.delegate = delegate;
        this.applies = applies;
        this.multiplier = multiplier;
    }

    public static SurchargePricing weekend(PricingStrategy delegate, BigDecimal multiplier) {
        return new SurchargePricing(delegate, slot -> {
            DayOfWeek day = slot.start().getDayOfWeek();
            return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        }, multiplier);
    }

    /** Applies when the slot starts within [from, to). */
    public static SurchargePricing peakHours(PricingStrategy delegate, LocalTime from, LocalTime to,
                                             BigDecimal multiplier) {
        return new SurchargePricing(delegate, slot -> {
            LocalTime start = slot.start().toLocalTime();
            return !start.isBefore(from) && start.isBefore(to);
        }, multiplier);
    }

    @Override
    public Money priceFor(Maid maid, ServiceType serviceType, TimeSlot slot) {
        Money base = delegate.priceFor(maid, serviceType, slot);
        return applies.test(slot) ? base.times(multiplier) : base;
    }
}
