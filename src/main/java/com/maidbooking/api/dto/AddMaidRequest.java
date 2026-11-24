package com.maidbooking.api.dto;

import com.maidbooking.domain.model.Gender;
import com.maidbooking.domain.model.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record AddMaidRequest(
        @NotBlank String name,
        @NotNull Gender gender,
        @NotBlank String locality,
        @NotEmpty Set<ServiceType> services,
        @NotNull @Positive BigDecimal hourlyRate,
        @DecimalMin("0.0") @DecimalMax("5.0") Double rating,
        @NotEmpty List<@Valid @NotNull AvailabilityWindowDto> availability) {
}
