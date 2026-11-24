package com.maidbooking.application.search;

import com.maidbooking.application.availability.AvailabilityService;
import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.repository.MaidRepository;
import com.maidbooking.domain.search.MaidFilter;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Discovery: applies every registered {@link MaidFilter} that has something to say about the query, then keeps
 * only maids genuinely available for the requested slot. New filters plug in without touching this class.
 */
@Service
public class MaidSearchService {

    private static final Comparator<Maid> BEST_FIRST = Comparator.comparingDouble(Maid::rating).reversed()
            .thenComparing(Maid::hourlyRate)
            .thenComparing(Maid::name);

    private final MaidRepository maids;
    private final AvailabilityService availability;
    private final List<MaidFilter> filters;

    public MaidSearchService(MaidRepository maids, AvailabilityService availability, List<MaidFilter> filters) {
        this.maids = maids;
        this.availability = availability;
        this.filters = List.copyOf(filters);
    }

    public List<Maid> search(MaidSearchQuery query) {
        Predicate<Maid> matchesFilters = filters.stream()
                .map(filter -> filter.predicateFor(query.filterParams()))
                .flatMap(Optional::stream)
                .reduce(maid -> true, Predicate::and);

        return maids.findAll().stream()
                .filter(matchesFilters)
                .filter(maid -> query.requestedSlot().map(slot -> availability.isAvailable(maid, slot)).orElse(true))
                .sorted(BEST_FIRST)
                .toList();
    }
}
