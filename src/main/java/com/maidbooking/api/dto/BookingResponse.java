package com.maidbooking.api.dto;

import com.maidbooking.domain.model.BookingStatus;
import com.maidbooking.domain.model.BookingType;
import com.maidbooking.domain.model.ServiceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(String id, String customerId, String maidId, ServiceType serviceType,
                              BookingType type, BookingStatus status, LocalDateTime createdAt,
                              BigDecimal totalAmount, List<OccurrenceResponse> occurrences) {
}
