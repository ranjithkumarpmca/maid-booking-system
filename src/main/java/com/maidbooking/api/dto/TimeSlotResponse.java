package com.maidbooking.api.dto;

import java.time.LocalDateTime;

public record TimeSlotResponse(LocalDateTime start, LocalDateTime end) {
}
