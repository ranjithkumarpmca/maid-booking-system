package com.maidbooking.api.dto;

import com.maidbooking.domain.model.BookingType;
import com.maidbooking.domain.model.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * One request shape for all booking types: INSTANT needs no time, SCHEDULED needs {@code startTime},
 * RECURRING needs {@code recurrence}. {@code durationMinutes} defaults to 60.
 */
public record BookingRequest(
        @NotBlank String customerId,
        @NotBlank String maidId,
        @NotNull ServiceType serviceType,
        @NotNull BookingType type,
        LocalDateTime startTime,
        @Min(30) @Max(480) Integer durationMinutes,
        @Valid RecurrenceDto recurrence) {
}
