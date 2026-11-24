package com.maidbooking.application.payment.method;

import com.maidbooking.domain.model.PaymentMethodType;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Card payments use a gateway-issued token; raw card numbers never reach this service. */
@Component
public class CardPaymentMethod extends AbstractPaymentMethod {

    @Override
    public PaymentMethodType type() {
        return PaymentMethodType.CARD;
    }

    @Override
    public void validate(Map<String, String> details) {
        require(details, "cardToken");
    }
}
