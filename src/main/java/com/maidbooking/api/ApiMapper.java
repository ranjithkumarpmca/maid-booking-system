package com.maidbooking.api;

import com.maidbooking.api.dto.AddMaidRequest;
import com.maidbooking.api.dto.AvailabilityWindowDto;
import com.maidbooking.api.dto.BookingRequest;
import com.maidbooking.api.dto.BookingResponse;
import com.maidbooking.api.dto.CancellationResponse;
import com.maidbooking.api.dto.MaidResponse;
import com.maidbooking.api.dto.OccurrenceResponse;
import com.maidbooking.api.dto.PaymentResponse;
import com.maidbooking.api.dto.RecurrenceDto;
import com.maidbooking.api.dto.TimeSlotResponse;
import com.maidbooking.application.booking.BookingCommand;
import com.maidbooking.application.booking.RecurrenceRule;
import com.maidbooking.application.cancellation.CancellationResult;
import com.maidbooking.application.maid.OnboardMaidCommand;
import com.maidbooking.application.payment.PaymentResult;
import com.maidbooking.domain.model.AvailabilityWindow;
import com.maidbooking.domain.model.Booking;
import com.maidbooking.domain.model.BookingOccurrence;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.Payment;
import com.maidbooking.domain.model.TimeSlot;

/** Translates between the HTTP contract (DTOs) and the application layer; keeps controllers thin. */
public final class ApiMapper {

    private static final int DEFAULT_DURATION_MINUTES = 60;

    private ApiMapper() {
    }

    public static OnboardMaidCommand toCommand(AddMaidRequest request) {
        return new OnboardMaidCommand(
                request.name(),
                request.gender(),
                request.locality(),
                request.services(),
                Money.of(request.hourlyRate()),
                request.rating(),
                request.availability().stream()
                        .map(window -> new AvailabilityWindow(window.day(), window.start(), window.end()))
                        .toList());
    }

    public static BookingCommand toCommand(BookingRequest request) {
        return new BookingCommand(
                request.customerId(),
                request.maidId(),
                request.serviceType(),
                request.type(),
                request.startTime(),
                request.durationMinutes() == null ? DEFAULT_DURATION_MINUTES : request.durationMinutes(),
                toRule(request.recurrence()));
    }

    private static RecurrenceRule toRule(RecurrenceDto dto) {
        if (dto == null) {
            return null;
        }
        return new RecurrenceRule(dto.daysOfWeek(), dto.time(), dto.startDate(), dto.endDate());
    }

    public static MaidResponse toResponse(Maid maid) {
        return new MaidResponse(
                maid.id(),
                maid.name(),
                maid.gender(),
                maid.locality(),
                maid.services(),
                maid.hourlyRate().amount(),
                maid.rating(),
                maid.availability().stream()
                        .map(window -> new AvailabilityWindowDto(window.day(), window.start(), window.end()))
                        .toList());
    }

    public static TimeSlotResponse toResponse(TimeSlot slot) {
        return new TimeSlotResponse(slot.start(), slot.end());
    }

    public static BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.id(),
                booking.customerId(),
                booking.maidId(),
                booking.serviceType(),
                booking.bookingType(),
                booking.status(),
                booking.createdAt(),
                booking.totalAmount().amount(),
                booking.occurrences().stream().map(ApiMapper::toResponse).toList());
    }

    private static OccurrenceResponse toResponse(BookingOccurrence occurrence) {
        return new OccurrenceResponse(
                occurrence.id(),
                occurrence.slot().start(),
                occurrence.slot().end(),
                occurrence.status(),
                occurrence.price().amount(),
                occurrence.paidAmount().amount(),
                occurrence.refundedAmount().amount());
    }

    public static PaymentResponse toResponse(PaymentResult result) {
        Payment payment = result.payment();
        return new PaymentResponse(
                payment.id(),
                payment.bookingId(),
                payment.amount().amount(),
                payment.method(),
                payment.status(),
                payment.gatewayReference(),
                payment.failureReason(),
                payment.refundedAmount().amount(),
                payment.occurrenceIds(),
                result.replayed());
    }

    public static CancellationResponse toResponse(CancellationResult result) {
        return new CancellationResponse(
                result.booking().id(),
                result.booking().status(),
                result.totalRefund().amount(),
                result.cancelledOccurrenceIds());
    }
}
