package com.maidbooking.infrastructure.persistence;

import com.maidbooking.domain.model.Booking;
import com.maidbooking.domain.repository.BookingRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBookingRepository implements BookingRepository {

    private final Map<String, Booking> store = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        store.put(booking.id(), booking);
        return booking;
    }

    @Override
    public Optional<Booking> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
