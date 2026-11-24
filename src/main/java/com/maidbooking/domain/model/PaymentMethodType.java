package com.maidbooking.domain.model;

/** Supported payment methods; each is backed by one PaymentMethod implementation. */
public enum PaymentMethodType {
    CARD,
    UPI,
    WALLET
}
