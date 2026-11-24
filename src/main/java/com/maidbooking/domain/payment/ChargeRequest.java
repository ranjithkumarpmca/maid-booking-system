package com.maidbooking.domain.payment;

import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.PaymentMethodType;

import java.util.Map;

public record ChargeRequest(String paymentId, PaymentMethodType method, Money amount, Map<String, String> details) {
}
