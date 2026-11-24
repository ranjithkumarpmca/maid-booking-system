package com.maidbooking.domain.refund;

import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.TimeSlot;

import java.time.LocalDateTime;

/** Decides how much of the amount paid for one occurrence is returned when it is cancelled. */
public interface RefundPolicy {

    Money refundFor(Money paidAmount, TimeSlot slot, LocalDateTime cancelledAt);
}
