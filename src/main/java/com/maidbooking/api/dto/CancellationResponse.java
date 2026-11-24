package com.maidbooking.api.dto;

import com.maidbooking.domain.model.BookingStatus;

import java.math.BigDecimal;
import java.util.List;

public record CancellationResponse(String bookingId, BookingStatus bookingStatus, BigDecimal totalRefund,
                                   List<String> cancelledOccurrenceIds) {
}
