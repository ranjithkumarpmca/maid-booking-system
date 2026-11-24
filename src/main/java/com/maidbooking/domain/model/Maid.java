package com.maidbooking.domain.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A maid onboarded onto the platform. Immutable profile; what the maid is <em>busy</em> with is tracked
 * separately by {@link com.maidbooking.domain.repository.SlotInventory} so it can be locked atomically.
 */
public final class Maid {

    private final String id;
    private final String name;
    private final Gender gender;
    private final String locality;
    private final Set<ServiceType> services;
    private final Money hourlyRate;
    private final double rating;
    private final List<AvailabilityWindow> availability;

    public Maid(String id, String name, Gender gender, String locality, Set<ServiceType> services,
                Money hourlyRate, double rating, List<AvailabilityWindow> availability) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.locality = requireText(locality, "locality");
        if (gender == null) {
            throw new IllegalArgumentException("gender is required");
        }
        if (services == null || services.isEmpty()) {
            throw new IllegalArgumentException("A maid must offer at least one service");
        }
        if (hourlyRate == null || hourlyRate.isZero()) {
            throw new IllegalArgumentException("hourlyRate must be greater than zero");
        }
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 0 and 5");
        }
        if (availability == null || availability.isEmpty()) {
            throw new IllegalArgumentException("A maid must have at least one availability window");
        }
        assertNoOverlappingWindows(availability);
        this.gender = gender;
        this.services = Collections.unmodifiableSet(EnumSet.copyOf(services));
        this.hourlyRate = hourlyRate;
        this.rating = rating;
        this.availability = List.copyOf(availability);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Gender gender() {
        return gender;
    }

    public String locality() {
        return locality;
    }

    public Set<ServiceType> services() {
        return services;
    }

    public Money hourlyRate() {
        return hourlyRate;
    }

    public double rating() {
        return rating;
    }

    public List<AvailabilityWindow> availability() {
        return availability;
    }

    public boolean offers(ServiceType type) {
        return services.contains(type);
    }

    /** True if the slot lies inside one of the maid's weekly working windows (ignores existing bookings). */
    public boolean isWorkingDuring(TimeSlot slot) {
        return availability.stream().anyMatch(window -> window.covers(slot));
    }

    /** The maid's working slots on a date, ordered by time (ignores existing bookings). */
    public List<TimeSlot> workingSlotsOn(LocalDate date) {
        return availability.stream()
                .flatMap(window -> window.on(date).stream())
                .sorted(Comparator.comparing(TimeSlot::start))
                .toList();
    }

    private static void assertNoOverlappingWindows(List<AvailabilityWindow> windows) {
        for (int i = 0; i < windows.size(); i++) {
            for (int j = i + 1; j < windows.size(); j++) {
                if (windows.get(i).overlaps(windows.get(j))) {
                    throw new IllegalArgumentException("Availability windows must not overlap: "
                            + windows.get(i) + " and " + windows.get(j));
                }
            }
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
