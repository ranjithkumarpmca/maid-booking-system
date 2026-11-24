package com.maidbooking.application.payment.method;

import com.maidbooking.domain.model.PaymentMethodType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WalletPaymentMethod extends AbstractPaymentMethod {

    @Override
    public PaymentMethodType type() {
        return PaymentMethodType.WALLET;
    }

    @Override
    public void validate(Map<String, String> details) {
        require(details, "walletId");
    }
}
