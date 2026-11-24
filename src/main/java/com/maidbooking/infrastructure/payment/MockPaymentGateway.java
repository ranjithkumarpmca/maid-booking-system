package com.maidbooking.infrastructure.payment;

import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.payment.ChargeRequest;
import com.maidbooking.domain.payment.GatewayResult;
import com.maidbooking.domain.payment.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stand-in for a real provider. Succeeds by default so flows can be exercised end to end; a failure can be
 * forced by sending {@code "simulate": "FAILURE"} in the payment details, and charges above the limit are declined.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    static final Money CHARGE_LIMIT = Money.of(100_000);

    @Override
    public GatewayResult charge(ChargeRequest request) {
        if ("FAILURE".equalsIgnoreCase(request.details().get("simulate"))) {
            return GatewayResult.failure("Declined by mock gateway (simulated failure)");
        }
        if (request.amount().isGreaterThan(CHARGE_LIMIT)) {
            return GatewayResult.failure("Amount exceeds the mock gateway limit of " + CHARGE_LIMIT);
        }
        return GatewayResult.ok("mock_charge_" + UUID.randomUUID());
    }

    @Override
    public GatewayResult refund(String gatewayReference, Money amount) {
        return GatewayResult.ok("mock_refund_" + UUID.randomUUID());
    }
}
