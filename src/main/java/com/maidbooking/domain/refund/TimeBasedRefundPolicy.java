package com.maidbooking.domain.refund;

import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.TimeSlot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Refund percentage depends on how much notice the customer gives. The first tier (longest notice first)
 * whose required notice is met applies; if none is met, nothing is refunded.
 */
public class TimeBasedRefundPolicy implements RefundPolicy {

    public record Tier(Duration minimumNotice, int refundPercent) {
        public Tier {
            if (refundPercent < 0 || refundPercent > 100) {
                throw new IllegalArgumentException("refundPercent must be between 0 and 100");
            }
        }
    }

    private final List<Tier> tiers;

    public TimeBasedRefundPolicy(List<Tier> tiers) {
        this.tiers = tiers.stream()
                .sorted(Comparator.comparing(Tier::minimumNotice).reversed())
                .toList();
    }

    /** 24h or more notice: full refund. 2h or more: half. Less than 2h: nothing. */
    public static TimeBasedRefundPolicy standard() {
        return new TimeBasedRefundPolicy(List.of(
                new Tier(Duration.ofHours(24), 100),
                new Tier(Duration.ofHours(2), 50)));
    }

    @Override
    public Money refundFor(Money paidAmount, TimeSlot slot, LocalDateTime cancelledAt) {
        if (paidAmount.isZero()) {
            return Money.ZERO;
        }
        Duration notice = Duration.between(cancelledAt, slot.start());
        return tiers.stream()
                .filter(tier -> notice.compareTo(tier.minimumNotice()) >= 0)
                .findFirst()
                .map(tier -> paidAmount.percent(tier.refundPercent()))
                .orElse(Money.ZERO);
    }
}
