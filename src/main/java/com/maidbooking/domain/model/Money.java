package com.maidbooking.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

/**
 * Immutable, non-negative amount in INR. A single currency is an explicit assumption of the platform.
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount is required");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Money cannot be negative: " + amount);
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(long rupees) {
        return new Money(BigDecimal.valueOf(rupees));
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money sum(Collection<Money> values) {
        return values.stream().reduce(ZERO, Money::plus);
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    /** Fails if the result would be negative. */
    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor));
    }

    public Money percent(int percent) {
        return times(BigDecimal.valueOf(percent).movePointLeft(2));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isGreaterThan(Money other) {
        return amount.compareTo(other.amount) > 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return "INR " + amount;
    }
}
