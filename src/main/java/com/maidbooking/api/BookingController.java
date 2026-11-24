package com.maidbooking.api;

import com.maidbooking.api.dto.BookingRequest;
import com.maidbooking.api.dto.BookingResponse;
import com.maidbooking.api.dto.CancellationResponse;
import com.maidbooking.api.dto.PaymentRequest;
import com.maidbooking.api.dto.PaymentResponse;
import com.maidbooking.application.booking.BookingService;
import com.maidbooking.application.cancellation.CancellationService;
import com.maidbooking.application.payment.PaymentCommand;
import com.maidbooking.application.payment.PaymentResult;
import com.maidbooking.application.payment.PaymentService;
import com.maidbooking.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Book, pay and cancel")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CancellationService cancellationService;

    public BookingController(BookingService bookingService, PaymentService paymentService,
                             CancellationService cancellationService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.cancellationService = cancellationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book a maid (INSTANT, SCHEDULED or RECURRING)",
            description = "Reserves the maid's time immediately; the booking stays PENDING_PAYMENT until paid. "
                    + "A recurring booking reserves every occurrence or none (409 if any clashes).")
    public BookingResponse book(@Valid @RequestBody BookingRequest request) {
        return ApiMapper.toResponse(bookingService.book(ApiMapper.toCommand(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a booking with its occurrences")
    public BookingResponse get(@PathVariable String id) {
        return ApiMapper.toResponse(bookingService.get(id));
    }

    @PostMapping("/{id}/payments")
    @Operation(summary = "Pay for a booking (idempotent)",
            description = "Send a unique `Idempotency-Key` header. Repeating the same request returns the stored "
                    + "outcome with `replayed=true` and never charges twice. 201 = charged, 200 = replay, "
                    + "402 = declined (retry with a new key).")
    public ResponseEntity<PaymentResponse> pay(@PathVariable String id,
                                               @RequestHeader("Idempotency-Key") String idempotencyKey,
                                               @Valid @RequestBody PaymentRequest request) {
        PaymentResult result = paymentService.pay(new PaymentCommand(
                id, request.method(), request.details(), idempotencyKey, request.occurrenceIds()));

        HttpStatus status;
        if (result.payment().status() == PaymentStatus.FAILED) {
            status = HttpStatus.PAYMENT_REQUIRED;
        } else {
            status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        }
        return ResponseEntity.status(status).body(ApiMapper.toResponse(result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel the whole booking (every upcoming occurrence of a series)",
            description = "Refunds follow the cancellation policy and the maid's slots become bookable again.")
    public CancellationResponse cancelSeries(@PathVariable String id) {
        return ApiMapper.toResponse(cancellationService.cancelSeries(id));
    }

    @DeleteMapping("/{id}/occurrences/{occurrenceId}")
    @Operation(summary = "Cancel a single occurrence of a booking")
    public CancellationResponse cancelOccurrence(@PathVariable String id, @PathVariable String occurrenceId) {
        return ApiMapper.toResponse(cancellationService.cancelOccurrence(id, occurrenceId));
    }
}
