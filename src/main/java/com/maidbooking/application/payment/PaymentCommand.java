package com.maidbooking.application.payment;

import com.maidbooking.domain.model.PaymentMethodType;

import java.util.Map;
import java.util.Set;

/**
 * @param occurrenceIds which occurrences to pay for; empty means every active, unpaid occurrence of the booking.
 *                      Recurring bookings can therefore be paid in one go or occurrence by occurrence.
 */
public record PaymentCommand(String bookingId, PaymentMethodType method, Map<String, String> details,
                             String idempotencyKey, Set<String> occurrenceIds) {

    public PaymentCommand {
        details = details == null ? Map.of() : Map.copyOf(details);
        occurrenceIds = occurrenceIds == null ? Set.of() : Set.copyOf(occurrenceIds);
    }
}
