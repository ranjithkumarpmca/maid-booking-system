# Maid Booking System

Backend for a maid booking platform built with Java 17 and Spring Boot 3. Customers can find maids, book them (instant, scheduled or recurring), pay, and cancel. Data is kept in memory.

## Run

```
mvn clean test
mvn spring-boot:run
```

Swagger UI: http://localhost:8080/swagger-ui.html

A few maids are loaded at startup. Their ids come from `GET /api/maids/search`.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/maids` | Add a maid |
| GET | `/api/maids/search` | Find maids (filters: locality, service, skills, minRating, maxPrice, gender; optional `start` + `durationMinutes` to get only maids free at that time) |
| GET | `/api/maids/{id}/availability?date=` | Free time slots of a maid on a date |
| POST | `/api/bookings` | Book (type: INSTANT, SCHEDULED or RECURRING) |
| POST | `/api/bookings/{id}/payments` | Pay (needs an `Idempotency-Key` header) |
| DELETE | `/api/bookings/{id}` | Cancel the whole booking or series |
| DELETE | `/api/bookings/{id}/occurrences/{occurrenceId}` | Cancel one occurrence |

## Design

Layers: `api` -> `application` -> `domain`, with `infrastructure` implementing the domain interfaces. The domain package does not depend on Spring.

Every booking is a list of occurrences (one for instant/scheduled, many for recurring). Price, payment and refund are tracked per occurrence, so cancelling or paying a single occurrence of a series needs no special handling.

Each of these is an interface, so a new one is a new class and existing code stays untouched:

- `BookingStrategy`: booking types
- `PaymentMethod`: payment methods (card, UPI, wallet)
- `MaidFilter`: search filters
- `PricingStrategy`: pricing (hourly rate plus a weekend surcharge)
- `RefundPolicy`: refunds (24h+ notice 100%, 2h+ 50%, otherwise none)
- Repositories, `SlotInventory` and `PaymentGateway`: persistence and the payment provider (a mock is used)

Double booking is prevented in `SlotInventory.tryReserveAll`, which checks and reserves under a per-maid lock. A recurring booking reserves all its slots or none.

Payments are idempotent. Repeating a request with the same key returns the earlier result without charging again.

On cancellation the refund goes through the gateway first. If it fails, nothing is cancelled. Otherwise the occurrence is cancelled and the maid's slot becomes free again.

Booking status: `PENDING_PAYMENT` -> `CONFIRMED` -> `CANCELLED`.

## Assumptions

- Single currency (INR) and single time zone (Asia/Kolkata).
- A maid's availability is a weekly pattern, and a booking must fit within one day.
- Booking duration is 30 to 480 minutes. An instant booking starts now.
- A recurring booking repeats weekly on chosen days between two dates, with at most 60 occurrences.
- The slot is held while payment is pending (no expiry).
- No authentication; `customerId` is taken as given.
- The mock gateway always succeeds. Send `"simulate": "FAILURE"` in the payment details to make it decline.

## With more time

- Release unpaid bookings after a timeout.
- A real database behind the repository interfaces, with a constraint to stop overlapping slots.
- Store idempotency keys with an expiry.
- Rescheduling, pagination, and API tests with MockMvc.
