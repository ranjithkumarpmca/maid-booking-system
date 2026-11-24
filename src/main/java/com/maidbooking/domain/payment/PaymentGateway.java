package com.maidbooking.domain.payment;

import com.maidbooking.domain.model.Money;

/** Port to the (third-party) payment provider. The application only ever talks to this interface. */
public interface PaymentGateway {

    GatewayResult charge(ChargeRequest request);

    GatewayResult refund(String gatewayReference, Money amount);
}
