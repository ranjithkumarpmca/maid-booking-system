package com.maidbooking.application.payment.method;

import com.maidbooking.domain.exception.InvalidRequestException;
import com.maidbooking.domain.payment.PaymentMethod;

import java.util.Map;

/** Shared "required field" check for payment methods. */
abstract class AbstractPaymentMethod implements PaymentMethod {

    protected String require(Map<String, String> details, String key) {
        String value = details.get(key);
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(type() + " payment requires '" + key + "'");
        }
        return value.trim();
    }
}
