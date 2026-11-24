package com.maidbooking.application;

import com.maidbooking.application.payment.PaymentCommand;
import com.maidbooking.application.payment.PaymentResult;
import com.maidbooking.domain.exception.IdempotencyConflictException;
import com.maidbooking.domain.exception.InvalidRequestException;
import com.maidbooking.domain.exception.InvalidStateException;
import com.maidbooking.domain.model.Booking;
import com.maidbooking.domain.model.BookingStatus;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.Money;
import com.maidbooking.domain.model.PaymentMethodType;
import com.maidbooking.domain.model.PaymentStatus;
import com.maidbooking.support.TestEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.maidbooking.support.TestEnvironment.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentServiceTest {

    private TestEnvironment env;
    private Maid maid;
    private Booking booking;

    @BeforeEach
    void setUp() {
        env = new TestEnvironment();
        maid = env.addDefaultMaid();
        booking = env.bookScheduled(maid, at(2, 10), 60); // INR 200
    }

    private PaymentCommand pay(String key, PaymentMethodType method, Map<String, String> details, Set<String> ids) {
        return new PaymentCommand(booking.id(), method, details, key, ids);
    }

    @Test
    @DisplayName("successful payment confirms the booking and charges the booking total")
    void successfulPayment() {
        PaymentResult result = env.payByUpi(booking, "key-1");

        assertEquals(PaymentStatus.SUCCEEDED, result.payment().status());
        assertFalse(result.replayed());
        assertEquals(Money.of(200), result.payment().amount());
        assertEquals(BookingStatus.CONFIRMED, env.bookingService.get(booking.id()).status());
        assertEquals(1, env.gateway.charges.get());
    }

    @Test
    @DisplayName("repeating a request with the same idempotency key replays the result without charging again")
    void idempotentReplay() {
        PaymentResult first = env.payByUpi(booking, "key-1");
        PaymentResult second = env.payByUpi(booking, "key-1");

        assertTrue(second.replayed());
        assertEquals(first.payment().id(), second.payment().id());
        assertEquals(1, env.gateway.charges.get());
    }

    @Test
    @DisplayName("an idempotency key reused for a different request is rejected")
    void idempotencyKeyReuse() {
        env.payByUpi(booking, "key-1");
        Map<String, String> card = Map.of("cardToken", "tok_123");

        assertThrows(IdempotencyConflictException.class,
                () -> env.paymentService.pay(pay("key-1", PaymentMethodType.CARD, card, null)));
    }

    @Test
    @DisplayName("concurrent duplicate requests with the same key charge exactly once")
    void concurrentDuplicates() throws Exception {
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<PaymentResult>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startGun.await();
                return env.payByUpi(booking, "same-key");
            }));
        }
        startGun.countDown();
        List<String> paymentIds = new ArrayList<>();
        for (Future<PaymentResult> future : futures) {
            paymentIds.add(future.get().payment().id());
        }
        pool.shutdown();

        assertEquals(1, env.gateway.charges.get());
        assertEquals(1, paymentIds.stream().distinct().count());
    }

    @Test
    @DisplayName("concurrent payments with different keys still cannot pay the same booking twice")
    void concurrentDifferentKeys() throws Exception {
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String key = "key-" + i;
            futures.add(pool.submit(() -> {
                startGun.await();
                try {
                    env.payByUpi(booking, key);
                    return true;
                } catch (InvalidStateException e) {
                    return false;
                }
            }));
        }
        startGun.countDown();
        int wins = 0;
        for (Future<Boolean> future : futures) {
            wins += future.get() ? 1 : 0;
        }
        pool.shutdown();

        assertEquals(1, wins);
        assertEquals(1, env.gateway.charges.get());
    }

    @Test
    @DisplayName("a declined charge leaves the booking unpaid; a retry needs a new key")
    void failedPayment() {
        env.gateway.failCharges = true;
        PaymentResult failed = env.payByUpi(booking, "key-1");

        assertEquals(PaymentStatus.FAILED, failed.payment().status());
        assertEquals(BookingStatus.PENDING_PAYMENT, env.bookingService.get(booking.id()).status());

        env.gateway.failCharges = false;
        PaymentResult replay = env.payByUpi(booking, "key-1");
        assertEquals(PaymentStatus.FAILED, replay.payment().status(), "same key replays the same outcome");
        assertEquals(1, env.gateway.charges.get());

        PaymentResult retry = env.payByUpi(booking, "key-2");
        assertEquals(PaymentStatus.SUCCEEDED, retry.payment().status());
        assertEquals(BookingStatus.CONFIRMED, env.bookingService.get(booking.id()).status());
    }

    @Test
    @DisplayName("each payment method validates its own details before the gateway is called")
    void methodValidation() {
        assertThrows(InvalidRequestException.class, () -> env.paymentService.pay(
                pay("k1", PaymentMethodType.UPI, Map.of("vpa", "not-a-vpa"), null)));
        assertThrows(InvalidRequestException.class, () -> env.paymentService.pay(
                pay("k2", PaymentMethodType.CARD, Map.of(), null)));
        assertThrows(InvalidRequestException.class, () -> env.paymentService.pay(
                pay("k3", PaymentMethodType.WALLET, Map.of("walletId", " "), null)));
        assertThrows(InvalidRequestException.class, () -> env.paymentService.pay(
                pay(" ", PaymentMethodType.CARD, Map.of("cardToken", "t"), null)), "missing idempotency key");
        assertEquals(0, env.gateway.charges.get());

        assertEquals(PaymentStatus.SUCCEEDED, env.paymentService.pay(
                pay("k4", PaymentMethodType.WALLET, Map.of("walletId", "w-1"), null)).payment().status());
    }

    @Test
    @DisplayName("an already paid or cancelled booking cannot be paid again")
    void invalidStates() {
        env.payByUpi(booking, "key-1");
        assertThrows(InvalidStateException.class, () -> env.payByUpi(booking, "key-2"));

        Booking other = env.bookScheduled(maid, at(3, 10), 60);
        env.cancellationService.cancelSeries(other.id());
        assertThrows(InvalidStateException.class, () -> env.payByUpi(other, "key-3"));
        assertEquals(1, env.gateway.charges.get());
    }

    @Test
    @DisplayName("a recurring booking can be paid occurrence by occurrence")
    void recurringPerOccurrence() {
        Booking series = env.bookRecurring(maid, Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), 0, 6);
        String first = series.occurrences().get(0).id();
        String second = series.occurrences().get(1).id();

        PaymentResult one = env.paymentService.pay(new PaymentCommand(series.id(), PaymentMethodType.UPI,
                Map.of("vpa", "asha@okbank"), "k1", Set.of(first)));
        assertEquals(Money.of(200), one.payment().amount());
        assertEquals(BookingStatus.CONFIRMED, env.bookingService.get(series.id()).status());
        assertFalse(env.bookingService.get(series.id()).occurrence(second).isPaid());

        PaymentResult rest = env.paymentService.pay(new PaymentCommand(series.id(), PaymentMethodType.UPI,
                Map.of("vpa", "asha@okbank"), "k2", null)); // everything still unpaid
        assertEquals(Money.of(400), rest.payment().amount());
        assertEquals(2, env.gateway.charges.get());
        assertThrows(InvalidStateException.class, () -> env.paymentService.pay(new PaymentCommand(series.id(),
                PaymentMethodType.UPI, Map.of("vpa", "asha@okbank"), "k3", Set.of(first))));
    }
}
