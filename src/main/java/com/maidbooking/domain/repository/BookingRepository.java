package com.maidbooking.domain.repository;

import com.maidbooking.domain.model.Booking;

import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(String id);
}
