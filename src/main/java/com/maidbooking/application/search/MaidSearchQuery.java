package com.maidbooking.application.search;

import com.maidbooking.domain.model.TimeSlot;

import java.util.Map;
import java.util.Optional;

/**
 * @param slot         the time the customer needs a maid for; when null, availability is not checked (browse mode)
 * @param filterParams raw filter parameters (locality, service, minRating, ...), interpreted by the registered filters
 */
public record MaidSearchQuery(TimeSlot slot, Map<String, String> filterParams) {

    public MaidSearchQuery {
        filterParams = filterParams == null ? Map.of() : Map.copyOf(filterParams);
    }

    public Optional<TimeSlot> requestedSlot() {
        return Optional.ofNullable(slot);
    }
}
