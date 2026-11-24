package com.maidbooking.api.dto;

import com.maidbooking.domain.model.PaymentMethodType;
import com.maidbooking.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;

public record PaymentResponse(String id, String bookingId, BigDecimal amount, PaymentMethodType method,
                              PaymentStatus status, String gatewayReference, String failureReason,
                              BigDecimal refundedAmount, List<String> occurrenceIds, boolean replayed) {
}
