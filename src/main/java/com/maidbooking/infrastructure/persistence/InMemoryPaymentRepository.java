package com.maidbooking.infrastructure.persistence;

import com.maidbooking.domain.model.Payment;
import com.maidbooking.domain.repository.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<String, Payment> byId = new ConcurrentHashMap<>();
    private final Map<String, Payment> byIdempotencyKey = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        byId.put(payment.id(), payment);
        byIdempotencyKey.put(payment.idempotencyKey(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(byIdempotencyKey.get(idempotencyKey));
    }
}
