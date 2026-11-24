package com.maidbooking.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record RecurrenceDto(
        @NotEmpty Set<DayOfWeek> daysOfWeek,
        @NotNull LocalTime time,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {
}
