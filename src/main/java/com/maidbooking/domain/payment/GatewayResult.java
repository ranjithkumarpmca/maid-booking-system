package com.maidbooking.domain.payment;

public record GatewayResult(boolean success, String reference, String failureReason) {

    public static GatewayResult ok(String reference) {
        return new GatewayResult(true, reference, null);
    }

    public static GatewayResult failure(String reason) {
        return new GatewayResult(false, null, reason);
    }
}
