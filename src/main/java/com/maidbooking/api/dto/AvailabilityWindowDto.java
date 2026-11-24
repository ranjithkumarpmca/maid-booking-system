package com.maidbooking.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityWindowDto(@NotNull DayOfWeek day, @NotNull LocalTime start, @NotNull LocalTime end) {
}
