package com.maidbooking.application.cancellation;

import com.maidbooking.domain.model.Booking;
import com.maidbooking.domain.model.Money;

import java.util.List;

public record CancellationResult(Booking booking, Money totalRefund, List<String> cancelledOccurrenceIds) {
}
