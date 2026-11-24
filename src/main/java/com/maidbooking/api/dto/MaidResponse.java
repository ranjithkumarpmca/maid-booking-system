package com.maidbooking.api.dto;

import com.maidbooking.domain.model.Gender;
import com.maidbooking.domain.model.ServiceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record MaidResponse(String id, String name, Gender gender, String locality, Set<ServiceType> services,
                           BigDecimal hourlyRate, double rating, List<AvailabilityWindowDto> availability) {
}
