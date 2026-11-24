package com.maidbooking.api.dto;

import com.maidbooking.domain.model.OccurrenceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OccurrenceResponse(String id, LocalDateTime start, LocalDateTime end, OccurrenceStatus status,
                                 BigDecimal price, BigDecimal paidAmount, BigDecimal refundedAmount) {
}
