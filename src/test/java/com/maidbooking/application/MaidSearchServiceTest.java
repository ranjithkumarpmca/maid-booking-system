package com.maidbooking.application;

import com.maidbooking.application.availability.AvailabilityService;
import com.maidbooking.application.search.MaidSearchQuery;
import com.maidbooking.application.search.MaidSearchService;
import com.maidbooking.domain.exception.InvalidRequestException;
import com.maidbooking.domain.model.Gender;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.model.ServiceType;
import com.maidbooking.domain.model.TimeSlot;
import com.maidbooking.domain.search.MaidFilter;
import com.maidbooking.support.TestEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.maidbooking.support.TestEnvironment.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaidSearchServiceTest {

    private TestEnvironment env;
    private Maid lakshmi;
    private Maid meena;
    private Maid ravi;

    @BeforeEach
    void setUp() {
        env = new TestEnvironment();
        lakshmi = env.addMaid("Lakshmi", Gender.FEMALE, "Koramangala",
                Set.of(ServiceType.CLEANING, ServiceType.DISHWASHING), 250, 4.7);
        meena = env.addMaid("Meena", Gender.FEMALE, "Koramangala",
                Set.of(ServiceType.COOKING, ServiceType.CLEANING), 350, 4.5);
        ravi = env.addMaid("Ravi", Gender.MALE, "Indiranagar",
                Set.of(ServiceType.CLEANING, ServiceType.LAUNDRY), 220, 4.1);
    }

    private List<String> names(Map<String, String> params, TimeSlot slot) {
        return env.searchService.search(new MaidSearchQuery(slot, params)).stream().map(Maid::name).toList();
    }

    @Test
    @DisplayName("no criteria returns everyone, best rated first")
    void browseAll() {
        assertEquals(List.of("Lakshmi", "Meena", "Ravi"), names(Map.of(), null));
    }

    @Test
    @DisplayName("locality, service, skills, rating, price and gender filters narrow the result")
    void filters() {
        assertEquals(List.of("Lakshmi", "Meena"), names(Map.of("locality", "koramangala"), null));
        assertEquals(List.of("Meena"), names(Map.of("service", "COOKING"), null));
        assertEquals(List.of("Meena"), names(Map.of("skills", "COOKING,CLEANING"), null));
        assertEquals(List.of("Lakshmi", "Meena"), names(Map.of("minRating", "4.5"), null));
        assertEquals(List.of("Lakshmi", "Ravi"), names(Map.of("maxPrice", "250"), null));
        assertEquals(List.of("Ravi"), names(Map.of("gender", "male"), null));
        assertEquals(List.of("Lakshmi"), names(Map.of("locality", "Koramangala", "service", "DISHWASHING",
                "minRating", "4.6", "maxPrice", "300"), null));
    }

    @Test
    @DisplayName("unknown parameters are ignored; malformed values are rejected")
    void parameterHandling() {
        assertEquals(3, names(Map.of("colour", "blue"), null).size());
        assertThrows(InvalidRequestException.class, () -> names(Map.of("minRating", "high"), null));
        assertThrows(InvalidRequestException.class, () -> names(Map.of("service", "GARDENING"), null));
    }

    @Test
    @DisplayName("only maids free for the requested slot are returned")
    void availabilityAware() {
        env.bookScheduled(lakshmi, at(1, 10), 60); // busy 10-11

        assertEquals(List.of("Meena", "Ravi"), names(Map.of(), TimeSlot.of(at(1, 10), 60)));
        assertEquals(List.of("Meena", "Ravi"), names(Map.of(), TimeSlot.of(at(1, 10).plusMinutes(30), 60)));
        assertEquals(List.of("Lakshmi", "Meena", "Ravi"), names(Map.of(), TimeSlot.of(at(1, 11), 60)));
    }

    @Test
    @DisplayName("maids who do not work at the requested time are excluded")
    void workingHours() {
        assertEquals(List.of(), names(Map.of(), TimeSlot.of(at(1, 6), 60)));
        assertEquals(List.of(), names(Map.of(), TimeSlot.of(at(1, 17), 120)));
    }

    @Test
    @DisplayName("free slots of a day are working hours minus bookings")
    void freeSlots() {
        env.bookScheduled(lakshmi, at(1, 10), 60);
        env.bookScheduled(lakshmi, at(1, 14), 120);

        AvailabilityService availability = env.availability;
        List<TimeSlot> free = availability.freeSlots(lakshmi, TestEnvironment.dateOf(1));

        assertEquals(List.of(
                new TimeSlot(at(1, 8), at(1, 10)),
                new TimeSlot(at(1, 11), at(1, 14)),
                new TimeSlot(at(1, 16), at(1, 18))), free);
        assertEquals(LocalTime.of(8, 0), free.get(0).start().toLocalTime());
    }

    @Test
    @DisplayName("a new filter plugs in without changing the search service")
    void newFilterIsPluggable() {
        MaidFilter nameFilter = params -> Optional.ofNullable(params.get("nameStartsWith"))
                .<Predicate<Maid>>map(prefix -> maid -> maid.name().startsWith(prefix));
        List<MaidFilter> filters = new ArrayList<>(List.of(nameFilter));
        MaidSearchService custom = new MaidSearchService(env.maids, env.availability, filters);

        List<Maid> result = custom.search(new MaidSearchQuery(null, Map.of("nameStartsWith", "R")));

        assertEquals(List.of(ravi.name()), result.stream().map(Maid::name).toList());
        assertEquals(3, custom.search(new MaidSearchQuery(null, Map.of())).size());
        assertEquals(meena.name(), "Meena");
    }
}
