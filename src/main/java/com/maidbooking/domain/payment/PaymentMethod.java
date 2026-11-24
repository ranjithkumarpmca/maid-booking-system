package com.maidbooking.domain.payment;

import com.maidbooking.domain.model.PaymentMethodType;

import java.util.Map;

/**
 * One way of paying (card, UPI, wallet...). Owns the validation of its own instrument details,
 * so the payment flow never needs to know what a card or a VPA looks like.
 */
public interface PaymentMethod {

    PaymentMethodType type();

    /** @throws com.maidbooking.domain.exception.InvalidRequestException if the details are unusable */
    void validate(Map<String, String> details);
}
