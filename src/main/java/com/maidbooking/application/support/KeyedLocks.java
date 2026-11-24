package com.maidbooking.application.support;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Serialises work per key (a booking id, an idempotency key) while letting unrelated keys run in parallel.
 * <p>
 * Lock ordering rule to stay deadlock-free: an idempotency lock may be taken before a booking lock, never after.
 * (Locks are never evicted: fine for an in-memory demo; a real deployment would use a distributed lock or
 * database row locks.)
 */
@Component
public class KeyedLocks {

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T withLock(String key, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public static String booking(String bookingId) {
        return "booking:" + bookingId;
    }

    public static String idempotency(String idempotencyKey) {
        return "idempotency:" + idempotencyKey;
    }
}
