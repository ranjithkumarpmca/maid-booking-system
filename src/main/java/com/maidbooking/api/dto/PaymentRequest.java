package com.maidbooking.api.dto;

import com.maidbooking.domain.model.PaymentMethodType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * @param details       instrument details: CARD {cardToken}, UPI {vpa}, WALLET {walletId}.
 *                      Add {"simulate": "FAILURE"} to make the mock gateway decline the charge.
 * @param occurrenceIds optional; omit to pay for every unpaid occurrence of the booking
 */
public record PaymentRequest(
        @NotNull PaymentMethodType method,
        @NotEmpty Map<String, String> details,
        Set<String> occurrenceIds) {
}
