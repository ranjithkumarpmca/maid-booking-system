package com.maidbooking.application.search.filter;

import com.maidbooking.domain.model.Maid;
import com.maidbooking.domain.search.MaidFilter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** {@code locality=Koramangala} (case-insensitive exact match). */
@Component
public class LocalityFilter implements MaidFilter {

    @Override
    public Optional<Predicate<Maid>> predicateFor(Map<String, String> params) {
        return FilterParams.text(params, "locality")
                .map(locality -> maid -> maid.locality().equalsIgnoreCase(locality));
    }
}
