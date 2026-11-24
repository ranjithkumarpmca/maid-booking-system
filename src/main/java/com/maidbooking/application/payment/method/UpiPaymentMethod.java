package com.maidbooking.application.payment.method;

import com.maidbooking.domain.exception.InvalidRequestException;
import com.maidbooking.domain.model.PaymentMethodType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public class UpiPaymentMethod extends AbstractPaymentMethod {

    private static final Pattern VPA = Pattern.compile("^[a-zA-Z0-9._-]{2,64}@[a-zA-Z]{2,32}$");

    @Override
    public PaymentMethodType type() {
        return PaymentMethodType.UPI;
    }

    @Override
    public void validate(Map<String, String> details) {
        String vpa = require(details, "vpa");
        if (!VPA.matcher(vpa).matches()) {
            throw new InvalidRequestException("'" + vpa + "' is not a valid UPI id (expected name@bank)");
        }
    }
}
