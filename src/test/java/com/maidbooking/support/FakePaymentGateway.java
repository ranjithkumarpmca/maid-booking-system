package com.maidbooking.support;

import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.payment.ChargeRequest;
import com.maidbooking.domain.payment.GatewayResult;
import com.maidbooking.domain.payment.PaymentGateway;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Counts calls so tests can assert "charged exactly once" and "gateway never called". */
public final class FakePaymentGateway implements PaymentGateway {

    public final AtomicInteger charges = new AtomicInteger();
    public final AtomicInteger refunds = new AtomicInteger();
    public final List<Money> chargedAmounts = new CopyOnWriteArrayList<>();
    public final List<Money> refundedAmounts = new CopyOnWriteArrayList<>();
    public volatile boolean failCharges;
    public volatile boolean failRefunds;

    @Override
    public GatewayResult charge(ChargeRequest request) {
        charges.incrementAndGet();
        if (failCharges) {
            return GatewayResult.failure("card declined");
        }
        chargedAmounts.add(request.amount());
        return GatewayResult.ok("charge-" + request.paymentId());
    }

    @Override
    public GatewayResult refund(String gatewayReference, Money amount) {
        if (failRefunds) {
            return GatewayResult.failure("refund service down");
        }
        refunds.incrementAndGet();
        refundedAmounts.add(amount);
        return GatewayResult.ok("refund-" + gatewayReference);
    }
}
